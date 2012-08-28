package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Domain class to store the 'Kmer Content' module data from fastqc files
 *
 */
class FastqcKmerContent {

    /**
     * kmer (motif of length k observed more than once)
     */
    String sequence

    /**
     * Count (This Field maps to the 'Count' on the fastQC file, which is a reserved word)
     */
    int countOfKmer

    /**
     * Obs/Exp Overall
     */
    double overall

    /**
     * Obs/Exp Max
     */
    double max

    /**
     * Max Obs/Exp Position
     */
    int position

    static constraints = {
        sequence(blank: false)
    }

    static belongsTo = [
        fastqcProcessedFile : FastqcProcessedFile
    ]
}
