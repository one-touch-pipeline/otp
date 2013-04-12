package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ProcessedMergedBamFileService {

    DataProcessingFilesService dataProcessingFilesService

    public String getDirectory(MergingPass mergingPass) {
        Individual ind = mergingPass.mergingSet.mergingWorkPackage.sample.individual
        DataProcessingFilesService.OutputDirectories dirType = DataProcessingFilesService.OutputDirectories.MERGING
        String baseDir = dataProcessingFilesService.getOutputDirectory(ind, dirType)
        String middleDir = "test" //TODO what to use
        return "${baseDir}/${middleDir}/${mergingPass.getDirectory()}"
    }

    public String getDirectory(ProcessedMergedBamFile mergedBamFile) {
        return getDirectory(mergedBamFile.mergingPass)
    }

    public String getFileNameNoSuffix(ProcessedMergedBamFile mergedBamFile) {
        //TODO naming, alignment use run and lane, but they not available here
        return "processedMergedBamFile"
    }

    public String getFileName(ProcessedMergedBamFile mergedBamFile) {
        String body = getFileNameNoSuffix(mergedBamFile)
        return "${body}.bam"
    }

    public String getFilePath(ProcessedMergedBamFile mergedBamFile) {
        String dir = getDirectory(mergedBamFile)
        String filename = getFileName(mergedBamFile)
        return "${dir}/${filename}"
    }

    public String getFileNameForMetrics(ProcessedMergedBamFile mergedBamFile) {
        return getFileNameNoSuffix(mergedBamFile) + "_metrics.txt"
    }

    public String getFilePathForMetrics(ProcessedMergedBamFile mergedBamFile) {
        String dir = getDirectory(mergedBamFile)
        String filename = getFileNameForMetrics(mergedBamFile)
        return "${dir}/${filename}"
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }

    public boolean updateBamFile(ProcessedMergedBamFile bamFile) {
        File file = new File(getFilePath(bamFile))
        if (!file.canRead()) {
            return false
        }
        bamFile.fileExists = true
        bamFile.fileSize = file.length()
        bamFile.dateFromFileSystem = new Date(file.lastModified())
        assertSave(bamFile)
        return bamFile.fileSize
    }

    public boolean updateBamFileIndex(ProcessedMergedBamFile bamFile) {
        String path = getFilePath(bamFile)
        File file = new File("${path}.bai")
        if (!file.canRead()) {
            return false
        }
        bamFile.hasIndexFile = true
        assertSave(bamFile)
        return true
    }
}
