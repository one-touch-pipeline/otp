package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.*
import static org.springframework.util.Assert.*

class ProcessedBamFileQaFileService {

    ConfigService configService
    DataProcessingFilesService dataProcessingFilesService
    MergedAlignmentDataFileService mergedAlignmentDataFileService
    ProcessedAlignmentFileService processedAlignmentFileService
    ProcessedBamFileService processedBamFileService

    private static final String QUALITY_ASSESSMENT_DIR_NAME = "QualityAssessment"

    public String directoryPath(QualityAssessmentPass pass) {
        String baseAndQaDir = directoryPath(pass.alignmentPass)
        String passDir = passDirectoryName(pass)
        return "${baseAndQaDir}/${passDir}"
    }

    public String directoryPath(AlignmentPass alignmentPass) {
        String baseDir = processedAlignmentFileService.getDirectory(alignmentPass)
        String qaDir = QUALITY_ASSESSMENT_DIR_NAME
        return "${baseDir}/${qaDir}"
    }

    public String passDirectoryName(QualityAssessmentPass pass) {
        return "pass${pass.identifier}"
    }

    public File finalDestinationDirectory(final QualityAssessmentPass pass) {
        return getPath(
                configService.getRootPath().path,
                mergedAlignmentDataFileService.buildRelativePath(pass.seqType, pass.sample),
                QUALITY_ASSESSMENT_DIR_NAME,
                processedAlignmentFileService.getRunLaneDirectory(pass.seqTrack),
        )
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

    public Collection<String> allFileNames(final ProcessedBamFile bamFile) {
        return [
            qualityAssessmentDataFileName(bamFile),
            coverageDataFileName(bamFile),
            sortedCoverageDataFileName(bamFile),
            coveragePlotFileName(bamFile),
            insertSizeDataFileName(bamFile),
            insertSizePlotFileName(bamFile),
            chromosomeMappingFileName(bamFile),
        ]
    }

    /**
     * Checks consistency for {@link #deleteProcessingFiles(QualityAssessmentPass)}.
     *
     * If there are inconsistencies, details are logged to the thread log (see {@link LogThreadLocal}).
     *
     * @return true if there is no serious inconsistency.
     */
    public boolean checkConsistencyForProcessingFilesDeletion(final QualityAssessmentPass pass) {
        notNull pass
        if (!pass.isLatestPass() || !pass.alignmentPass.isLatestPass()) {
            // The QA results of this pass are outdated, so in the final location they will have been overwritten with
            // the results of a later pass. Hence, checking if the files of this pass are in the final location does not
            // make sense.
            return true
        }
        return dataProcessingFilesService.checkConsistencyWithFinalDestinationForDeletion(
                new File(directoryPath(pass)),
                finalDestinationDirectory(pass),
                allFileNames(pass.processedBamFile))
    }

    /**
     * Deletes the files of the specified QA pass from the "processing" directory on the file system.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteProcessingFiles(final QualityAssessmentPass pass) {
        notNull pass
        if (!checkConsistencyForProcessingFilesDeletion(pass)) {
            return 0L
        }
        return dataProcessingFilesService.deleteProcessingFilesAndDirectory(
                pass.project,
                new File(directoryPath(pass)),
                allFileNames(pass.processedBamFile))
    }
}
