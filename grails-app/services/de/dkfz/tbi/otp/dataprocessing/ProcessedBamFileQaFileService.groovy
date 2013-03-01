package de.dkfz.tbi.otp.dataprocessing

class ProcessedBamFileQaFileService {

    def processedBamFileService

    public String directoryPath(ProcessedBamFile bamFile) {
        String baseDir = processedBamFileService.getDirectory(bamFile)
        String qaDir = "QualityAssessment"
        return "${baseDir}/${qaDir}"
    }

    public String qualityAssessmentDataFilePath(ProcessedBamFile bamFile) {
        String dir = directoryPath(bamFile)
        String filename = qualityAssessmentDataFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String coverageDataFilePath(ProcessedBamFile bamFile) {
        String dir = directoryPath(bamFile)
        String filename = coverageDataFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String sortedcoverageDataFilePath(ProcessedBamFile bamFile) {
        String dir = directoryPath(bamFile)
        String filename = coverageDataFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String coveragePlotFilePath(ProcessedBamFile bamFile) {
        String dir = directoryPath(bamFile)
        String filename = coveragePlotFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String qualityAssessmentDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_quality.json"
    }

    public String coverageDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_coverage.tsv"
    }

    public String sortedcoverageDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_sorted_coverage.tsv"
    }

    public String coveragePlotFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_coveragePlot.png"
    }
}