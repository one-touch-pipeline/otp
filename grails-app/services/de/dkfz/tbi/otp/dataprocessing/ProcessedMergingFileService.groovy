package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.*

class ProcessedMergingFileService {

    DataProcessingFilesService dataProcessingFilesService

    MergingPassService mergingPassService

    public String directory(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        MergingSet mergingSet = mergingPass.mergingSet
        MergingWorkPackage mergingWorkPackage = mergingSet.mergingWorkPackage
        Sample sample = mergingWorkPackage.sample
        Individual individual = sample.individual
        DataProcessingFilesService.OutputDirectories dirType = DataProcessingFilesService.OutputDirectories.MERGING
        String baseDir = dataProcessingFilesService.getOutputDirectory(individual, dirType)
        String seqTypeName = "${mergingWorkPackage.seqType.name}/${mergingWorkPackage.seqType.libraryLayout}"
        String workPackageCriteraPart = "${(mergingWorkPackage.processingType == MergingWorkPackage.ProcessingType.SYSTEM ? mergingWorkPackage.mergingCriteria : MergingWorkPackage.ProcessingType.MANUAL)}"
        String workPackageNamePart = "${seqTypeName}/${workPackageCriteraPart}"
        String dir = "${sample.sampleType.name}/${workPackageNamePart}/${mergingSet.identifier}/pass${mergingPass.identifier}"
        return "${baseDir}/${dir}"
    }

    public String directory(ProcessedMergedBamFile mergedBamFile) {
        notNull(mergedBamFile, "The parameter mergedBamFile is not allowed to be null")
        return directory(mergedBamFile.mergingPass)
    }
}
