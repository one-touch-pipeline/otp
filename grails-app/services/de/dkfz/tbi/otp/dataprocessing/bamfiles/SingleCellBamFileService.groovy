/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.filestore.PathOption

import java.nio.file.Path

@CompileDynamic
@Transactional
class SingleCellBamFileService<T extends AbstractBamFile> extends AbstractAbstractBamFileService<SingleCellBamFile> {

    AbstractBamFileService abstractBamFileService

    static final String INPUT_DIRECTORY_NAME = 'cell-ranger-input'

    static final String OUTPUT_DIRECTORY_NAME = 'outs'

    static final String ANALYSIS_DIRECTORY_NAME = 'analysis'

    static final String ORIGINAL_BAM_FILE_NAME = 'possorted_genome_bam.bam'

    static final String ORIGINAL_BAI_FILE_NAME = 'possorted_genome_bam.bam.bai'

    // is created manually
    static final String ORIGINAL_BAM_MD5SUM_FILE_NAME = 'possorted_genome_bam.md5sum'

    static final String METRICS_SUMMARY_CSV_FILE_NAME = "metrics_summary.csv"

    static final String WEB_SUMMARY_HTML_FILE_NAME = "web_summary.html"

    static final String CELL_RANGER_COMMAND_FILE_NAME = "cell_ranger_command.txt"

    static final List<String> CREATED_RESULT_FILES = [
            WEB_SUMMARY_HTML_FILE_NAME,
            METRICS_SUMMARY_CSV_FILE_NAME,
            ORIGINAL_BAM_FILE_NAME,
            ORIGINAL_BAI_FILE_NAME,
            ORIGINAL_BAM_MD5SUM_FILE_NAME,
            'filtered_feature_bc_matrix.h5',
            'raw_feature_bc_matrix.h5',
            'molecule_info.h5',
            'cloupe.cloupe',
    ].asImmutable()

    static final List<String> CREATED_RESULT_DIRS = [
            'filtered_feature_bc_matrix',
            'raw_feature_bc_matrix',
            'analysis',
    ].asImmutable()

    static final List<String> CREATED_RESULT_FILES_AND_DIRS = [
            CREATED_RESULT_FILES,
            CREATED_RESULT_DIRS,
    ].flatten().asImmutable()

    Path getWorkDirectory(SingleCellBamFile bamFile) {
        return abstractBamFileService.getBaseDirectory(bamFile).resolve(bamFile.workDirectoryName)
    }

    String buildWorkDirectoryName(CellRangerMergingWorkPackage workPackage, int identifier) {
        return [
                "RG_${workPackage.referenceGenome.name ?: '-'}",
                "TV_${workPackage.referenceGenomeIndex.toolWithVersion.replace(" ", "-")}",
                "EC_${workPackage.expectedCells ?: '-'}",
                "FC_${workPackage.enforcedCells ?: '-'}",
                "PV_${workPackage.config.programVersion.replace("/", "-")}",
                "ID_${identifier}",
        ].join('_')
    }

    Path getSampleDirectory(SingleCellBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(INPUT_DIRECTORY_NAME).resolve(bamFile.singleCellSampleName)
    }

    Path getOutputDirectory(SingleCellBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(bamFile.singleCellSampleName)
    }

    Path getResultDirectory(SingleCellBamFile bamFile) {
        return getOutputDirectory(bamFile).resolve(OUTPUT_DIRECTORY_NAME)
    }

    /**
     * Map of names to use for link and name used by CellRanger
     */
    Map<String, String> getFileMappingForLinks(SingleCellBamFile bamFile) {
        return CREATED_RESULT_FILES_AND_DIRS.collectEntries {
            [(getLinkNameForFile(bamFile, it)): it]
        }
    }

    /**
     * list of linked files
     */
    List<Path> getLinkedResultFiles(SingleCellBamFile bamFile) {
        Path result = getWorkDirectory(bamFile)
        return CREATED_RESULT_FILES_AND_DIRS.collect {
            result.resolve(getLinkNameForFile(bamFile, it))
        }
    }

    /**
     * return the name to use for the links of the result file, because the bam file should be named differently
     */
    private String getLinkNameForFile(SingleCellBamFile bamFile, String name) {
        switch (name) {
            case ORIGINAL_BAM_FILE_NAME:
                return bamFile.bamFileName
            case ORIGINAL_BAI_FILE_NAME:
                return bamFile.baiFileName
            case ORIGINAL_BAM_MD5SUM_FILE_NAME:
                return bamFile.md5SumFileName
            default:
                return name
        }
    }

    @Override
    Path getFinalInsertSizeFile(SingleCellBamFile bamFile, PathOption... options) {
        throw new UnsupportedOperationException("Final insert size file is not implemented for single cell BAM files (${bamFile})")
    }

    @Override
    protected Path getPathForFurtherProcessingNoCheck(SingleCellBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(bamFile.bamFileName)
    }

    Path getQualityAssessmentCsvFile(SingleCellBamFile bamFile) {
        return getResultDirectory(bamFile).resolve(METRICS_SUMMARY_CSV_FILE_NAME)
    }

    Path getWebSummaryResultFile(SingleCellBamFile bamFile) {
        return getResultDirectory(bamFile).resolve(WEB_SUMMARY_HTML_FILE_NAME)
    }
}
