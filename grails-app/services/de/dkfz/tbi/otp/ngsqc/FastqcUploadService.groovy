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
package de.dkfz.tbi.otp.ngsqc

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

import java.util.regex.Matcher

/**
 * Service providing methods to parse FastQC files and saving the parsed data to the database
 */
@Transactional
class FastqcUploadService {

    static final String DATA_FILE_NAME = "fastqc_data.txt"
    static final Map<String, String> PROPERTIES_REGEX_TO_BE_PARSED = [
            nReads: /\nTotal\sSequences\t(\d+)\t?\n/,
            sequenceLength: /\nSequence\slength\t(\d+(?:-\d+)?)\t?\n/,
    ]

    FastqcDataFilesService fastqcDataFilesService

    /**
     * Uploads the fastQC file contents generated from the fastq file to the database
     */
    void uploadFastQCFileContentsToDataBase(FastqcProcessedFile fastqc) {
        assert fastqc : "No FastQC file defined"
        try {
            Map parsedFastqcFile = parseFastQCFile(fastqc, PROPERTIES_REGEX_TO_BE_PARSED)
            fastqc.dataFile.nReads = parsedFastqcFile["nReads"] as long
            fastqc.dataFile.sequenceLength = parsedFastqcFile["sequenceLength"]
            fastqc.dataFile.save()
            fastqc.contentUploaded = true
            fastqc.save()
        } catch (final Throwable t) {
            throw new RuntimeException("Failed to load data from ${DATA_FILE_NAME} of ${fastqc} into the database.", t)
        }
    }

    /**
     * Parses the FastQC result file for nReads (Total Sequences) & sequenceLength (Sequence length)
     */
    Map<String, String> parseFastQCFile(FastqcProcessedFile fastqc, Map<String, String> propertiesToBeParsedWithRegEx) {
        assert fastqc : "No FastQC file defined"
        assert propertiesToBeParsedWithRegEx: "No properties defined to parse for in FastQC file ${fastqc}."

        String fastqcFileContent = getFastQCFileContent(fastqc)
        assert fastqcFileContent : "FastQC file content of ${fastqc} is empty."

        Map<String, String> parsedProperties = [:]

        propertiesToBeParsedWithRegEx.each { String key, String regex ->
            String value = parsePropertyFromFastQC(fastqcFileContent, regex) {
                throw new RuntimeException("FastQC file ${fastqc} contains no information about ${key} with regular expression ${regex}")
            }
            parsedProperties << [(key): value]
        }

        return parsedProperties
    }

    private static String parsePropertyFromFastQC(String fastqcFileContent, String regex, Closure exception) {
        Matcher matcher = fastqcFileContent =~ regex
        if (matcher) {
            return matcher.group(1) as String
        }
        return exception()
    }

    String getFastQCFileContent(FastqcProcessedFile fastqc) {
        return fastqcDataFilesService.getInputStreamFromZipFile(fastqc.dataFile, DATA_FILE_NAME).text
    }
}
