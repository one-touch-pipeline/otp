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
    int sequenceLength

    /**
     * This field is in the 'Sequence Duplication Levels' module at the FastQC file
     */
    double totalDuplicatePercentage

    static constraints = {
        fileType(blank: false)
        encoding(blank: false)
    }

    static belongsTo = [
        fastqcProcessedFile : FastqcProcessedFile
    ]
}
