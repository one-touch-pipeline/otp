package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Domain class to store the Overrepresented sequences data from fastqc files
 *
 */
class FastqcOverrepresentedSequences {

    /**
     * Overrepresented Sequence
     */
    String sequence

    /**
     * Count of Overrepresented Sequences (This field maps to the 'Count' on the fastQC file, which is a reserved word)
     */
    int countOverRep

    /**
     * Percentage
     */
    double percentage

    /**
     * Possible Source
     */
    String possibleSource

    static belongsTo = [
        fastqcProcessedFile : FastqcProcessedFile
    ]

    static constraints = {
        sequence(blank: false)
        countOverRep(min:0)
        percentage(min:0D, max:100D)
        possibleSource(blank:false)
    }
}
