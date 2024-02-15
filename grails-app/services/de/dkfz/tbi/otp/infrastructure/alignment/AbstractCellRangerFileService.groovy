/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure.alignment

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile

import java.nio.file.Path

@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract trait AbstractCellRangerFileService implements AbstractAlignmentFileService<SingleCellBamFile> {

    Path getSampleDirectory(SingleCellBamFile bamFile) {
        return getDirectoryPath(bamFile).resolve(CellRangerFileNames.INPUT_DIRECTORY_NAME).resolve(bamFile.id.toString())
    }

    Path getOutputDirectory(SingleCellBamFile bamFile) {
        return getDirectoryPath(bamFile).resolve(bamFile.id.toString())
    }

    Path getResultDirectory(SingleCellBamFile bamFile) {
        return getOutputDirectory(bamFile).resolve(CellRangerFileNames.OUTPUT_DIRECTORY_NAME)
    }

    /**
     * Map of names to use for link and name used by CellRanger
     */
    Map<String, String> getFileMappingForLinks(SingleCellBamFile bamFile) {
        return CellRangerFileNames.CREATED_RESULT_FILES_AND_DIRS.collectEntries {
            [(getLinkNameForFile(bamFile, it)): it]
        }
    }

    /**
     * list of linked files
     */
    List<Path> getLinkedResultFiles(SingleCellBamFile bamFile) {
        Path result = getDirectoryPath(bamFile)
        return CellRangerFileNames.CREATED_RESULT_FILES_AND_DIRS.collect {
            result.resolve(getLinkNameForFile(bamFile, it))
        }
    }

    /**
     * return the name to use for the links of the result file, because the bam file should be named differently
     */
    private String getLinkNameForFile(SingleCellBamFile bamFile, String name) {
        switch (name) {
            case CellRangerFileNames.ORIGINAL_BAM_FILE_NAME:
                return bamFile.bamFileName
            case CellRangerFileNames.ORIGINAL_BAI_FILE_NAME:
                return bamFile.baiFileName
            case CellRangerFileNames.ORIGINAL_BAM_MD5SUM_FILE_NAME:
                return bamFile.md5SumFileName
            default:
                return name
        }
    }

    Path getQualityAssessmentCsvFile(SingleCellBamFile bamFile) {
        return getResultDirectory(bamFile).resolve(CellRangerFileNames.METRICS_SUMMARY_CSV_FILE_NAME)
    }

    Path getWebSummaryResultFile(SingleCellBamFile bamFile) {
        return getResultDirectory(bamFile).resolve(CellRangerFileNames.WEB_SUMMARY_HTML_FILE_NAME)
    }

    @Override
    Path getInsertSizeFile(SingleCellBamFile bamFile) {
        throw new UnsupportedOperationException("Insert size file is not implemented for single cell BAM files (${bamFile})")
    }
}
