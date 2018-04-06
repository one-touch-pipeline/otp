package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.hibernate.*

import static org.springframework.util.Assert.*

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

    enum QcTrafficLightStatus {
        // status is set by OTP when QC threshold passed
        QC_PASSED,
        // status is set by bioinformaticians when they decide to keep the file although a QC threshold not passed
        ACCEPTED,
        // status is set by OTP when a QC error threshold not passed
        BLOCKED,
        // status is set by  bioinformaticians when they decide not to use a file for further analyses
        REJECTED
    }

    public abstract boolean isMostRecentBamFile()

    public abstract String getBamFileName()

    public abstract String getBaiFileName()

    public abstract AlignmentConfig getAlignmentConfig()

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
        workPackage lazy: false
        workPackage index: "abstract_merged_bam_file_work_package_idx"
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


    public void updateFileOperationStatus(FileOperationStatus status) {
        notNull(status, "the input status for the method updateFileOperationStatus is null")
        this.fileOperationStatus = status
    }

    public ReferenceGenome getReferenceGenome() {
        return workPackage.referenceGenome
    }

    public void validateAndSetBamFileInProjectFolder() {
        withTransaction {
            assert fileOperationStatus == FileOperationStatus.INPROGRESS
            assert !withdrawn
            assert CollectionUtils.exactlyOneElement(AbstractMergedBamFile.findAll {
                workPackage == this.workPackage &&
                        withdrawn == false &&
                        fileOperationStatus == FileOperationStatus.INPROGRESS
            }).id == this.id

            workPackage.bamFileInProjectFolder = this
            assert workPackage.save(flush: true)
        }
    }

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
            throw new IllegalStateException("This BAM file is not in the project folder or not processable.\nthis: ${this}\nprocessableBamFileInProjectFolder: ${processableBamFileInProjectFolder}")
        }
    }

    @Override
    Project getProject() {
        return individual?.project
    }

    protected abstract File getPathForFurtherProcessingNoCheck()

}
