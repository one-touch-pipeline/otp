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

final class CellRangerFileNames {
    static final String INPUT_DIRECTORY_NAME = 'cell-ranger-input'

    static final String OUTPUT_DIRECTORY_NAME = 'outs'

    static final String ANALYSIS_DIRECTORY_NAME = 'analysis'

    static final String ORIGINAL_BAM_FILE_NAME = 'possorted_genome_bam.bam'

    static final String ORIGINAL_BAI_FILE_NAME = 'possorted_genome_bam.bam.bai'

    // is created manually
    static final String ORIGINAL_BAM_MD5SUM_FILE_NAME = 'possorted_genome_bam.md5sum'

    static final String METRICS_SUMMARY_CSV_FILE_NAME = 'metrics_summary.csv'

    static final String WEB_SUMMARY_HTML_FILE_NAME = 'web_summary.html'

    static final String CELL_RANGER_COMMAND_FILE_NAME = 'cell_ranger_command.txt'

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
    ]

    static final List<String> CREATED_RESULT_DIRS = [
            'filtered_feature_bc_matrix',
            'raw_feature_bc_matrix',
            ANALYSIS_DIRECTORY_NAME,
    ].asImmutable()

    static final List<String> CREATED_RESULT_FILES_AND_DIRS = [
            CREATED_RESULT_FILES,
            CREATED_RESULT_DIRS,
    ].collectMany { it }.asImmutable()

    private CellRangerFileNames() { }
}
