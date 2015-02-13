package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Domain class to store the 'Basic Statistics' module data from fastqc files
 *
 */
class FastqcBasicStatistics {

    String fileType
    String encoding
    long totalSequences
    long filteredSequences
    String sequenceLength

    /**
     * This field is in the 'Sequence Duplication Levels' module at the FastQC file
     */
    double totalDuplicatePercentage

    static constraints = {
        fileType(blank: false)
        encoding(blank: false)
        sequenceLength(blank: false)
    }

    static belongsTo = [
        fastqcProcessedFile: FastqcProcessedFile
    ]



    static mapping = {
        fastqcProcessedFile index: "fastqc_basic_statistics_fastqc_processed_file_idx"
    }
}
