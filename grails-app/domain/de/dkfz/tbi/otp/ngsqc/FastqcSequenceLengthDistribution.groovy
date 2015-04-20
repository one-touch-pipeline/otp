package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Domain class to store the 'Sequence Length Distribution' module data from fastqc files
 *
 */
class FastqcSequenceLengthDistribution {

    /**
     * Length of the sequences (Column Label in fastqc file is 'length').
     * Length can be a range, therefore a String is used.
     */
    String length

    /**
     * Number of sequences with same length (Column label in fastqc file is 'Count' which is a reserved word)
     */
    double countSequences

    static belongsTo = [
        fastqcProcessedFile: FastqcProcessedFile
    ]

    static constraints = {
        length nullable: false, blank: false
    }

    static mapping = {
        fastqcProcessedFile index: "fastqc_sequence_length_distribution_fastqc_processed_file_idx"
    }
}
