package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Service providing methods to parse FastQC files and saving the parsed data to the database
 */
class FastqcUploadService {

    static final String DATA_FILE_NAME = "fastqc_data.txt"
    static final Map<String, String> PROPERTIES_REGEX_TO_BE_PARSED= [
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
            fastqc.dataFile.save(flush: true, failOnError: true)
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
        def matcher = fastqcFileContent =~ regex
        if (matcher) {
            return matcher.group(1) as String
        } else {
            exception()
        }
    }

    String getFastQCFileContent(FastqcProcessedFile fastqc) {
        return fastqcDataFilesService.getInputStreamFromZipFile(fastqc.dataFile, DATA_FILE_NAME).text
    }
}
