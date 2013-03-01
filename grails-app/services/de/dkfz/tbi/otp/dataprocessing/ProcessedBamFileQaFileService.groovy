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

    public String sortedCoverageDataFilePath(ProcessedBamFile bamFile) {
        String dir = directoryPath(bamFile)
        String filename = sortedCoverageDataFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String coveragePlotFilePath(ProcessedBamFile bamFile) {
        String dir = directoryPath(bamFile)
        String filename = coveragePlotFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String insertSizeDataFilePath(ProcessedBamFile bamFile) {
        String dir = directoryPath(bamFile)
        String filename = insertSizeDataFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String insertSizePlotFilePath(ProcessedBamFile bamFile) {
        String dir = directoryPath(bamFile)
        String filename = insertSizePlotFileName(bamFile)
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

    public String sortedCoverageDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_filtered_and_sorted_coverage.tsv"
    }

    public String coveragePlotFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_coveragePlot.png"
    }

    public String insertSizeDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_quality_distribution.hst"
    }

    public String insertSizePlotFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_insertSizePlot.png"
    }
}
