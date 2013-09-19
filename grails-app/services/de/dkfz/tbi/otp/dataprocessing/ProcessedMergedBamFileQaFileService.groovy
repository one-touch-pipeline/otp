package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.SavingException

/**
 * Service for {@link ProcessedMergedBamFile}
 *
 *
 */
class ProcessedMergedBamFileQaFileService {

    ProcessedMergedBamFileService processedMergedBamFileService

    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    public static final MD5SUM_NAME = 'MD5SUMS'

    /**
     * returns the directory for the given {@link QualityAssessmentMergedPass}.
     */
    public String directoryPath(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        String baseDir = processedMergedBamFileService.directory(pass.processedMergedBamFile)
        String qaDir = "QualityAssessment"
        String passDir = passDirectory(pass)
        return "${baseDir}/${qaDir}/${passDir}"
    }

    /**
     * returns the part of the directory for the pass for the given {@link QualityAssessmentMergedPass}.
     */
    public String passDirectory(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        return "pass${pass.identifier}"
    }

    /**
     * returns the file path (file name + directory) for the JSON file.
     */
    public String qualityAssessmentDataFilePath(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        String dir = directoryPath(pass)
        String filename = qualityAssessmentDataFileName(pass.processedMergedBamFile)
        return "${dir}/${filename}"
    }

    /**
     * returns the file path (file name + directory) for the coverage file.
     */
    public String coverageDataFilePath(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        String dir = directoryPath(pass)
        String filename = coverageDataFileName(pass.processedMergedBamFile)
        return "${dir}/${filename}"
    }

    /**
     * returns the file path (file name + directory) for the mapped, filtered and sorted coverage file.
     */
    public String mappedFilteredSortedCoverageDataFilePath(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        String dir = directoryPath(pass)
        String filename = sortedCoverageDataFileName(pass.processedMergedBamFile)
        return "${dir}/${filename}"
    }

    /**
     * returns the file path (file name + directory) for the coverage plot.
     */
    public String coveragePlotFilePath(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        String dir = directoryPath(pass)
        String filename = coveragePlotFileName(pass.processedMergedBamFile)
        return "${dir}/${filename}"
    }

    /**
     * returns the file path (file name + directory) for the file the insert size plot is created from.
     */
    public String insertSizeDataFilePath(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        String dir = directoryPath(pass)
        String filename = insertSizeDataFileName(pass.processedMergedBamFile)
        return "${dir}/${filename}"
    }

    /**
     * returns the file path (file name + directory) for the insert size plot.
     */
    public String insertSizePlotFilePath(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        String dir = directoryPath(pass)
        String filename = insertSizePlotFileName(pass.processedMergedBamFile)
        return "${dir}/${filename}"
    }

    /**
     * returns the file path (file name + directory) chromosome mapping, filtering and sorting.
     */
    public String chromosomeMappingFilePath(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        String dir = directoryPath(pass)
        String filename = chromosomeMappingFileName(pass.processedMergedBamFile)
        return "${dir}/${filename}"
    }

    /**
     * returns the file name for the JSON file.
     */
    public String qualityAssessmentDataFileName(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the processed merged bam file is null")
        String fileName = processedMergedBamFileService.fileNameNoSuffix(bamFile)
        return "${fileName}_quality.json"
    }

    /**
     * returns the file name for the coverage file.
     */
    public String coverageDataFileName(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the processed merged bam file is null")
        String fileName = processedMergedBamFileService.fileNameNoSuffix(bamFile)
        return "${fileName}_coverage.tsv"
    }

    /**
     * returns the file name for the mapped, filtered and sorted coverage file.
     */
    public String sortedCoverageDataFileName(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the processed merged bam file is null")
        String fileName = processedMergedBamFileService.fileNameNoSuffix(bamFile)
        return "${fileName}_mappedFilteredAndSortedCoverage.tsv"
    }

    /**
     * returns the file name for the coverage plot.
     */
    public String coveragePlotFileName(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the processed merged bam file is null")
        String fileName = processedMergedBamFileService.fileNameNoSuffix(bamFile)
        return "${fileName}_coveragePlot.png"
    }

    /**
     * returns the file name for the data the insert size plot is created from.
     */
    public String insertSizeDataFileName(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the processed merged bam file is null")
        String fileName = processedMergedBamFileService.fileNameNoSuffix(bamFile)
        return "${fileName}_qualityDistribution.hst"
    }

    /**
     * returns the file name for the insert size plot.
     */
    public String insertSizePlotFileName(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the processed merged bam file is null")
        String fileName = processedMergedBamFileService.fileNameNoSuffix(bamFile)
        return "${fileName}_insertSizePlot.png"
    }

    /**
     * returns the file name for the chromosome mapping, filtering and sorting.
     */
    public String chromosomeMappingFileName(ProcessedMergedBamFile bamFile) {
        notNull(bamFile, "the processed merged bam file is null")
        String fileName = processedMergedBamFileService.fileNameNoSuffix(bamFile)
        return "${fileName}_chromosomeMapping.json"
    }

    /**
     * validates the existence (read access and size bigger zero) of the QA data files.
     *
     * @return <code>true</code>, if all files exist, <code>false</code> otherwise
     */
    public boolean validateQADataFiles(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        boolean coverageDataFileExists = validateFile(coverageDataFilePath(pass))
        boolean qualityAssessmentFileExists = validateFile(qualityAssessmentDataFilePath(pass))
        boolean insertSizeDataFileExists = validateFile(insertSizeDataFilePath(pass))
        return coverageDataFileExists && qualityAssessmentFileExists && insertSizeDataFileExists
    }

    /**
     * validates the existence (read access and size bigger zero) of the coverage plot and update the database.
     *
     * @return <code>true</code>, if the plot exist, <code>false</code> otherwise
     */
    public boolean validateCoveragePlotAndUpdateProcessedMergedBamFileStatus(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        pass.processedMergedBamFile.hasCoveragePlot = validateFile(coveragePlotFilePath(pass))
        assertSave(pass.processedMergedBamFile)
        return pass.processedMergedBamFile.hasCoveragePlot
    }

    /**
     * validates the existence (read access and size bigger zero) of the insert size plot and update the database.
     *
     * @return <code>true</code>, if the plot exist, <code>false</code> otherwise
     */
    public boolean validateInsertSizePlotAndUpdateProcessedMergedBamFileStatus(QualityAssessmentMergedPass pass) {
        notNull(pass, "the quality assessment merged pass is null")
        pass.processedMergedBamFile.hasInsertSizePlot = validateFile(insertSizePlotFilePath(pass))
        assertSave(pass.processedMergedBamFile)
        return pass.processedMergedBamFile.hasInsertSizePlot
    }

    /**
     * @param file, the ProcessedMergedBamFile for which the QA results were produced
     * @return path to the directory where the file with the calculated md5sums for the latest qa results is stored
     */
    public String qaResultsMd5sumFile(ProcessedMergedBamFile file) {
        notNull(file, "the input of the method qaResultDirectory is null")
        QualityAssessmentMergedPass pass = qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(file)
        return directoryPath(pass) + "/" + MD5SUM_NAME
    }

    /**
     * checks and returns, if the file can be read and has a size bigger zero.
     *
     * @param path the path to check
     * @return the result of the check
     */
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
