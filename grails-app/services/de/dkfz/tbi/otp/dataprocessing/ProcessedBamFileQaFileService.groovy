/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.MergedAlignmentDataFileService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.getPath
import static org.springframework.util.Assert.notNull

class ProcessedBamFileQaFileService {

    ConfigService configService
    DataProcessingFilesService dataProcessingFilesService
    MergedAlignmentDataFileService mergedAlignmentDataFileService
    ProcessedAlignmentFileService processedAlignmentFileService
    ProcessedBamFileService processedBamFileService

    private static final String QUALITY_ASSESSMENT_DIR_NAME = "QualityAssessment"

    String directoryPath(QualityAssessmentPass pass) {
        String baseAndQaDir = directoryPath(pass.alignmentPass)
        String passDir = passDirectoryName(pass)
        return "${baseAndQaDir}/${passDir}"
    }

    String directoryPath(AlignmentPass alignmentPass) {
        String baseDir = processedAlignmentFileService.getDirectory(alignmentPass)
        String qaDir = QUALITY_ASSESSMENT_DIR_NAME
        return "${baseDir}/${qaDir}"
    }

    String passDirectoryName(QualityAssessmentPass pass) {
        return "pass${pass.identifier}"
    }

    File finalDestinationDirectory(final QualityAssessmentPass pass) {
        return getPath(
                configService.getRootPath().path,
                mergedAlignmentDataFileService.buildRelativePath(pass.seqType, pass.sample),
                QUALITY_ASSESSMENT_DIR_NAME,
                processedAlignmentFileService.getRunLaneDirectory(pass.seqTrack),
        )
    }

    String qualityAssessmentDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_quality.json"
    }

    String coverageDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_coverage.tsv"
    }

    String sortedCoverageDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_mappedFilteredAndSortedCoverage.tsv"
    }

    String coveragePlotFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_coveragePlot.png"
    }

    String insertSizeDataFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_qualityDistribution.hst"
    }

    String insertSizePlotFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_insertSizePlot.png"
    }

    String chromosomeMappingFileName(ProcessedBamFile bamFile) {
        String fileName = processedBamFileService.getFileNameNoSuffix(bamFile)
        return "${fileName}_chromosomeMapping.json"
    }

    Collection<String> allFileNames(final ProcessedBamFile bamFile) {
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
    boolean checkConsistencyForProcessingFilesDeletion(final QualityAssessmentPass pass) {
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
    long deleteProcessingFiles(final QualityAssessmentPass pass) {
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
