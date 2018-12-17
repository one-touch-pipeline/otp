package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.TupleConstructor
import org.hibernate.Hibernate

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import static org.springframework.util.Assert.notNull

/**
 * Represents a single generation of one merged BAM file (whereas a {@link AbstractMergingWorkPackage} represents all
 * generations).
 */
abstract class AbstractMergedBamFile extends AbstractFileSystemBamFile implements Commentable {

    /**
     * Holds the number of lanes which were merged in this bam file
     */
    Integer numberOfMergedLanes

    /**
     * bam file satisfies criteria from this {@link AbstractMergingWorkPackage}
     */
    AbstractMergingWorkPackage workPackage

    /**
     * This property contains the transfer state of an AbstractMergedBamFile to the project folder.
     * Be aware that in case of the ProcessedMergedBamFile the property is used to trigger the transfer workflow,
     * whereas in the RoddyBamFile it is only used for documentation of the state.
     */
    FileOperationStatus fileOperationStatus = FileOperationStatus.DECLARED

    Comment comment

    QcTrafficLightStatus qcTrafficLightStatus

    @TupleConstructor
    enum QcTrafficLightStatus {
        // status is set by OTP when QC thresholds were met
        QC_PASSED(JobLinkCase.CREATE_LINKS),
        // status is set by bioinformaticians when they decide to keep the file although QC thresholds were not met
        ACCEPTED(JobLinkCase.SHOULD_NOT_OCCUR),
        // status is set by OTP when QC error thresholds were not met
        BLOCKED(JobLinkCase.CREATE_NO_LINK),
        // status is set by bioinformaticians when they decide not to use a file for further analyses
        REJECTED(JobLinkCase.SHOULD_NOT_OCCUR),
        // status is set by OTP when QC thresholds were not met but the project is configured to allow failed files
        AUTO_ACCEPTED(JobLinkCase.CREATE_LINKS),
        // status is set by OTP when project is configured to not check QC thresholds
        UNCHECKED(JobLinkCase.CREATE_LINKS),


        final JobLinkCase jobLinkCase

        /**
         * Category of {@link QcTrafficLightStatus}. It defines, if links should be created or nor should not
         * should not occur automatically in job system, but only set manually in the gui.
         */
        @TupleConstructor
        enum JobLinkCase {
            /**
             * For that cases, links should be created
             */
            CREATE_LINKS(true),
            /**
             * For that cases, no links should be created
             */
            CREATE_NO_LINK(true),
            /**
             * Cases set manually and should therefor not occur during workflow
             */
            SHOULD_NOT_OCCUR(false),

            final boolean allowedInWorkflow
        }
    }

    abstract boolean isMostRecentBamFile()

    abstract String getBamFileName()

    abstract String getBaiFileName()

    abstract AlignmentConfig getAlignmentConfig()

    abstract File getFinalInsertSizeFile()

    abstract Integer getMaximalReadLength()

    static constraints = {
        numberOfMergedLanes nullable: true, validator: { val, obj ->
            if (Hibernate.getClass(obj) == ExternallyProcessedMergedBamFile) {
                val == null
            } else {
                val >= 1
            }
        }
        md5sum nullable: true, validator: { val, obj ->
            return (!val || (val && obj.fileOperationStatus == FileOperationStatus.PROCESSED && obj.fileSize > 0))
        }
        fileOperationStatus validator: { val, obj ->
            return (val == FileOperationStatus.PROCESSED) == (obj.md5sum != null)
        }
        qcTrafficLightStatus nullable: true, validator: { status, obj ->
            if ([QcTrafficLightStatus.ACCEPTED, QcTrafficLightStatus.REJECTED, QcTrafficLightStatus.BLOCKED].contains(status) && !obj.comment) {
                return "a comment is required in case the QC status is set to ACCEPTED, REJECTED or BLOCKED"
            }
        }
        comment nullable: true
    }

    static mapping = {
        numberOfMergedLanes index: "abstract_merged_bam_file_number_of_merged_lanes_idx"
        qcTrafficLightStatus index: "abstract_bam_file_qc_traffic_light_status"
        workPackage lazy: false, index: "abstract_merged_bam_file_work_package_idx"
    }

    /**
     * This enum is used to specify the different transfer states of the {@link AbstractMergedBamFile} until it is copied to the project folder
     */
    enum FileOperationStatus {
        /**
         * default value -> state of the {@link AbstractMergedBamFile} when it is created (declared)
         * no processing has been started on the bam file and it is also not ready to be transferred yet
         */
        DECLARED,
        /**
         * An {@link AbstractMergedBamFile} with this status needs to be transferred.
         */
        NEEDS_PROCESSING,
        /**
         * An {@link AbstractMergedBamFile} is in process of being transferred.
         */
        INPROGRESS,
        /**
         * The transfer of the {@link AbstractMergedBamFile} is finished.
         */
        PROCESSED
    }

    void updateFileOperationStatus(FileOperationStatus status) {
        notNull(status, "the input status for the method updateFileOperationStatus is null")
        this.fileOperationStatus = status
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        return workPackage.referenceGenome
    }

    void validateAndSetBamFileInProjectFolder() {
        withTransaction {
            assert fileOperationStatus == FileOperationStatus.INPROGRESS
            assert !withdrawn
            assert CollectionUtils.exactlyOneElement(AbstractMergedBamFile.findAllWhere(
                    workPackage        : this.workPackage,
                    withdrawn          : false,
                    fileOperationStatus: FileOperationStatus.INPROGRESS
            )) == this
            workPackage.bamFileInProjectFolder = this
            assert workPackage.save(flush: true)
        }
    }

    @Override
    void withdraw() {
        withTransaction {
            BamFilePairAnalysis.findAllBySampleType1BamFileOrSampleType2BamFile(this, this).each {
                it.withdraw()
            }

            super.withdraw()
        }
    }

    File getBaseDirectory() {
        String antiBodyTarget = seqType.isChipSeq() ? "-${((MergingWorkPackage) mergingWorkPackage).antibodyTarget.name}" : ''
        OtpPath viewByPid = individual.getViewByPidPath(seqType)
        OtpPath path = new OtpPath(
                viewByPid,
                "${sample.sampleType.dirName}${antiBodyTarget}".toString(),
                seqType.libraryLayoutDirName,
                'merged-alignment'
        )
        return path.absoluteDataManagementPath
    }

    File getPathForFurtherProcessing() {
        if ([QcTrafficLightStatus.REJECTED, QcTrafficLightStatus.BLOCKED].contains(this.qcTrafficLightStatus)) {
            return null
        }
        mergingWorkPackage.refresh() //Sometimes the mergingWorkPackage.processableBamFileInProjectFolder is empty but should have a value
        AbstractMergedBamFile processableBamFileInProjectFolder = mergingWorkPackage.processableBamFileInProjectFolder
        if (this.id == processableBamFileInProjectFolder?.id) {
            return getPathForFurtherProcessingNoCheck()
        } else {
            throw new IllegalStateException("This BAM file is not in the project folder or not processable.\n" +
                    "this: ${this}\nprocessableBamFileInProjectFolder: ${processableBamFileInProjectFolder}")
        }
    }

    @Override
    Project getProject() {
        return individual?.project
    }

    Realm getRealm() {
        return project?.realm
    }

    protected abstract File getPathForFurtherProcessingNoCheck()

}
