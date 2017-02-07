package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity

abstract class AbstractMergingWorkPackage implements Entity {

    Sample sample
    SeqType seqType
    ReferenceGenome referenceGenome
    Pipeline pipeline

    /**
     * The BAM file which moving to the final destination has been initiated for most recently.
     *
     * Note that if {@link AbstractMergedBamFile#fileOperationStatus} is {@link AbstractMergedBamFile.FileOperationStatus#INPROGRESS}, moving
     * is still in progress or has failed, so the file system is in an unclear state.
     * Also note that the referenced BAM file might be withdrawn.
     * If you want to use the referenced BAM file as input for further processing, use
     * {@link #getProcessableBamFileInProjectFolder()}).
     */
    /*
     * Due some strange behavior of GORM (?), this has to be set on null explicitly where objects of
     * {@link de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile} or its subclasses are built or created.
     * See {@link de.dkfz.tbi.otp.ngsdata.DomainFactory#createProcessedMergedBamFile}
     */
    AbstractMergedBamFile bamFileInProjectFolder

    static belongsTo = Sample

    static constraints = {
        bamFileInProjectFolder nullable: true, validator: { val, obj ->
            if (val) {
                val.workPackage == obj && [AbstractMergedBamFile.FileOperationStatus.INPROGRESS, AbstractMergedBamFile.FileOperationStatus.PROCESSED].contains(val.fileOperationStatus)
            } else {
                return true
            }
        }
    }

    /**
     * Returns the BAM file which is currently in the project folder, or <code>null</code> if there is no BAM file or it
     * is withdrawn or it is unknown which one currently is there.
     *
     * If you use the returned BAM file as input for further processing, ensure that the file on the file system is
     * consistent with the database object by comparing the file size on the file system to
     * {@link AbstractFileSystemBamFile#fileSize}. Perform this check a second time <em>after</em> reading from the file
     * to ensure that the file has not been overwritten between the first check and starting to read the file.
     */
    AbstractMergedBamFile getProcessableBamFileInProjectFolder() {
        if (bamFileInProjectFolder && !bamFileInProjectFolder.withdrawn &&
                bamFileInProjectFolder.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED) {
            return bamFileInProjectFolder
        } else {
            return null
        }
    }

    Project getProject() {
        return sample.project
    }

    Individual getIndividual() {
        return sample.individual
    }

    SampleType getSampleType() {
        return sample.sampleType
    }

    static mapping = {
        'class' index: "abstract_merging_work_package_class_idx"
        seqType index: "abstract_merging_work_package_seq_type_idx"
        referenceGenome index: "abstract_merging_work_package_reference_genome_idx"
        bamFileInProjectFolder index: "abstract_merging_work_package_bam_file_in_project_folder_idx"
    }
}
