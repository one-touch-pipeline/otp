package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SavingException

class ProcessedBamFileQaFileService {

    ProcessedBamFileService processedBamFileService

    public String directoryPath(QualityAssessmentPass pass) {
        String baseDir = processedBamFileService.getDirectory(pass.processedBamFile)
        String qaDir = "QualityAssessment"
        String passDir = passDirectory(pass)
        return "${baseDir}/${qaDir}/${passDir}"
    }

    public String passDirectory(QualityAssessmentPass pass) {
        return "pass${pass.identifier}"
    }

    public String qualityAssessmentDataFilePath(QualityAssessmentPass pass) {
        String dir = directoryPath(pass)
        String filename = qualityAssessmentDataFileName(pass.processedBamFile)
        return "${dir}/${filename}"
    }

    public String coverageDataFilePath(QualityAssessmentPass pass) {
        String dir = directoryPath(pass)
        String filename = coverageDataFileName(pass.processedBamFile)
        return "${dir}/${filename}"
    }

    public String mappedFilteredSortedCoverageDataFilePath(QualityAssessmentPass pass) {
        String dir = directoryPath(pass)
        String filename = sortedCoverageDataFileName(pass.processedBamFile)
        return "${dir}/${filename}"
    }

    public String coveragePlotFilePath(QualityAssessmentPass pass) {
        String dir = directoryPath(pass)
        String filename = coveragePlotFileName(pass.processedBamFile)
        return "${dir}/${filename}"
    }

    public String insertSizeDataFilePath(QualityAssessmentPass pass) {
        String dir = directoryPath(pass)
        String filename = insertSizeDataFileName(pass.processedBamFile)
        return "${dir}/${filename}"
    }

    public String insertSizePlotFilePath(QualityAssessmentPass pass) {
        String dir = directoryPath(pass)
        String filename = insertSizePlotFileName(pass.processedBamFile)
        return "${dir}/${filename}"
    }

    public String chromosomeMappingFilePath(QualityAssessmentPass pass) {
        String dir = directoryPath(pass)
        String filename = chromosomeMappingFileName(pass.processedBamFile)
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
        return "${fileName}_mappedFilteredAndSortedCoverage.tsv"
    }

    public String coveragePlotFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_coveragePlot.png"
    }

    public String insertSizeDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_qualityDistribution.hst"
    }

    public String insertSizePlotFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_insertSizePlot.png"
    }

    public String chromosomeMappingFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_chromosomeMapping.json"
    }

    public boolean validateQADataFiles(QualityAssessmentPass pass) {
        boolean coverageDataFileExists = validateFile(coverageDataFilePath(pass))
        boolean qualityAssessmentFileExists = validateFile(qualityAssessmentDataFilePath(pass))
        boolean insertSizeDataFileExists = validateFile(insertSizeDataFilePath(pass))
        return coverageDataFileExists && qualityAssessmentFileExists && insertSizeDataFileExists
    }

    public boolean validateCoveragePlotAndUpdateProcessedBamFileStatus(QualityAssessmentPass pass) {
        pass.processedBamFile.hasCoveragePlot = validateFile(coveragePlotFilePath(pass))
        assertSave(pass.processedBamFile)
        return pass.processedBamFile.hasCoveragePlot
    }

    public boolean validateInsertSizePlotAndUpdateProcessedBamFileStatus(QualityAssessmentPass pass) {
        pass.processedBamFile.hasInsertSizePlot = validateFile(insertSizePlotFilePath(pass))
        assertSave(pass.processedBamFile)
        return pass.processedBamFile.hasInsertSizePlot
    }

    private boolean validateFile(String path) {
        File file = new File(path)
        return file.canRead() && file.size() != 0
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
