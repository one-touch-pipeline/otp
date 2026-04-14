/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure.fastqc

import de.dkfz.tbi.otp.dataprocessing.*

import java.nio.file.Path

trait AbstractFastqcFileService implements ArtefactFileService<FastqcProcessedFile> {

    static final String FAST_QC_DIRECTORY_PART = DataProcessingFilesService.OutputDirectories.FASTX_QC.toString().toLowerCase()
    static final String FAST_QC_FILE_SUFFIX = "_fastqc"
    static final String FAST_QC_ZIP_SUFFIX = ".zip"
    static final String HTML_FILE_EXTENSION = ".html"
    static final String MD5SUM_FILE_EXTENSION = ".md5sum"

    abstract Path fastqcOutputPath(FastqcProcessedFile fastqcProcessedFile)

    Path fastqcHtmlPath(FastqcProcessedFile fastqcProcessedFile) {
        Path fastqc = fastqcOutputPath(fastqcProcessedFile)
        String fileName = fastqc.fileName.toString().replace(FAST_QC_ZIP_SUFFIX, HTML_FILE_EXTENSION)
        return fastqc.resolveSibling(fileName)
    }

    Path fastqcOutputMd5sumPath(FastqcProcessedFile fastqcProcessedFile) {
        Path fastqc = fastqcOutputPath(fastqcProcessedFile)
        String fileName = fastqc.fileName.toString().concat(MD5SUM_FILE_EXTENSION)
        return fastqc.resolveSibling(fileName)
    }

    /**
     * returns the calculated name for the fastqc file, base on the fastq name, including the zip suffix
     */
    String fastqcFileName(FastqcProcessedFile fastqcProcessedFile) {
        return "${fastqcFileNameWithoutZipSuffix(fastqcProcessedFile)}${FAST_QC_ZIP_SUFFIX}"
    }

    /**
     * returns the calculated name for the fastqc file, base on the fastq name, and without the zip suffix
     */
    String fastqcFileNameWithoutZipSuffix(FastqcProcessedFile fastqcProcessedFile) {
        return fastqcFileNameWithoutZipSuffixHelper(inputFileNameAdaption(fastqcProcessedFile.sequenceFile.fileName))
    }

    /**
     * Remove suffix for compressed files
     */
    String inputFileNameAdaption(String fileName) {
        Integer suffixLength = CompressionFormat.getUsedFormat(fileName)?.suffix?.length()
        return suffixLength ? fileName[0..<-suffixLength] : fileName
    }

    private String fastqcFileNameWithoutZipSuffixHelper(String fileName) {
        /*
         * The fastqc tool does not allow to specify the output file name, only the output directory.
         * To access the file we need code to create the same name for the output file as the fastqc tool.
         * How the name is created from the input file name is looked up from the fastqc tool. The rule is in:
         * uk.ac.babraham.FastQC.Analysis.OfflineRunner.analysisComplete
         */
        String body = fileName.replaceAll("stdin:", "").replaceAll("\\.gz\$", "")
                .replaceAll("\\.bz2\$", "").replaceAll("\\.txt\$", "")
                .replaceAll("\\.fastq\$", "").replaceAll("\\.fq\$", "")
                .replaceAll("\\.csfastq\$", "").replaceAll("\\.sam\$", "")
                .replaceAll("\\.bam\$", "")
        return "${body}${FAST_QC_FILE_SUFFIX}"
    }
}
