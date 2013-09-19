package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class MergingWorkPackageService {

    /**
     * @param sample, {@link ProcessedBamFile}s, which belong to one {@link MergingWorkPackage} and shell be merged are from this sample
     * @param seqType, BamFiles, which belong to one workPackage and shell be merged are from this seqType
     * @param criteria, the MergingCriteria defines, which criteria the {@link ProcessedBamFile}s have to fulfill to be merged
     * @return the new created workPackage, containing the information, which BamFiles are
     * available for the corresponding {@link Sample} and {@link SeqType} and satisfy the given {@link MergingCriteria} or custom selection.
     */
    MergingWorkPackage createWorkPackage(Sample sample, SeqType seqType, MergingCriteria criteria) {
        notNull(sample, "the input sample for the method createWorkPackage is null")
        notNull(seqType, "the input seqType for the method createWorkPackage is null")
        notNull(criteria, "the input criteria for the method createWorkPackage is null")
        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                sample: sample,
                seqType: seqType,
                mergingCriteria: criteria
                )
        assertSave(mergingWorkPackage)
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }

    /**
     * @return a list of workpackages, to which the processedMergedBamFiles belong to, which are currently in transfer
     */
    List<ProcessedMergedBamFile> workPackagesOfFilesInTransfer() {
        List<ProcessedMergedBamFile> filesInTransfer = ProcessedMergedBamFile.findAllByFileOperationStatus(AbstractBamFile.FileOperationStatus.INPROGRESS)
        return filesInTransfer ? filesInTransfer*.mergingPass*.mergingSet*.mergingWorkPackage : []
    }
}
