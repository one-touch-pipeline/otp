package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Maps the 'Sequence Duplication Levels' module
 * This module at FastQC file has a field that will be stored on the 'Basic Statistics' module corresponding class
 * (#Total Duplicate Percentage	11.091313808867866)
 *
 */
class FastqcSequenceDuplicationLevels {

    /**
     * Duplication level (Column label in fastqc is 'Duplication Level' )
     * It maps values from '1' to '10++' (the '10' maps the '10++' which stands for more than 10)
     */
    int duplicationLevel

    /**
     * Relative Count (Column label in fastqc is 'Relative count')
     */
    double relativeCount

    static constraints = {
        duplicationLevel(min: 1, max:10)
        relativeCount(min: 0D)
    }

    static belongsTo = [
        fastqcProcessedFile: FastqcProcessedFile
    ]
}
