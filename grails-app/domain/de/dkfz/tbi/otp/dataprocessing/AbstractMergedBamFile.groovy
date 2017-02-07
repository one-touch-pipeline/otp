package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.hibernate.*

import static org.springframework.util.Assert.notNull

/**
 * Represents a single generation of one merged BAM file (whereas a {@link AbstractMergingWorkPackage} represents all
 * generations).
 */
abstract class AbstractMergedBamFile extends AbstractFileSystemBamFile {

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

    public abstract boolean isMostRecentBamFile()

    public abstract String getBamFileName()

    public abstract String getBaiFileName()

    public abstract AlignmentConfig getAlignmentConfig()

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
    }

    static mapping = {
        numberOfMergedLanes index: "abstract_merged_bam_file_number_of_merged_lanes_idx"
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


    abstract public void withdraw()


    private withdrawCorrespondingSnvResults() {
        withTransaction {
            //find snv and make them withdrawn
            SnvJobResult.withCriteria {
                isNull 'inputResult'
                snvCallingInstance {
                    or {
                        eq 'sampleType1BamFile', this
                        eq 'sampleType2BamFile', this
                    }
                }
            }.each {
                it.withdraw()
            }
        }
    }

    File getBaseDirectory() {
        return new File(AbstractMergedBamFileService.destinationDirectory(this))
    }

    File getPathForFurtherProcessing() {
        mergingWorkPackage.refresh() //Sometimes the mergingWorkPackage.processableBamFileInProjectFolder is empty but should have a value
        AbstractMergedBamFile processableBamFileInProjectFolder = mergingWorkPackage.processableBamFileInProjectFolder
        if (this.id == processableBamFileInProjectFolder?.id) {
            return getPathForFurtherProcessingNoCheck()
        } else {
            throw new IllegalStateException("This BAM file is not in the project folder or not processable.\nthis: ${this}\nprocessableBamFileInProjectFolder: ${processableBamFileInProjectFolder}")
        }
    }

    protected abstract File getPathForFurtherProcessingNoCheck()

}
