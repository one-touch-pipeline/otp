package de.dkfz.tbi.otp.dataprocessing

class MergingWorkPackageService {

    /**
     * @return a list of workpackages, to which the processedMergedBamFiles belong to, which are currently in transfer
     */
    List<ProcessedMergedBamFile> workPackagesOfFilesInTransfer() {
        List<ProcessedMergedBamFile> filesInTransfer = ProcessedMergedBamFile.findAllByFileOperationStatus(AbstractBamFile.FileOperationStatus.INPROGRESS)
        return filesInTransfer ? filesInTransfer*.mergingPass*.mergingSet*.mergingWorkPackage : []
    }
}
