package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Domain class to store the data from Fastqc "Per Sequence Quality Scores" module
 */
class FastqcPerSequenceQualityScores {

    /**
     * Quality Score. A Q score is simply a representation of the probability that a base-call is incorrect (Column label in fastqc is 'Quality' )
     */
    int quality

    /**
     * Counts of Quality Score (Column label in fastqc is 'Count' which is a reserved word)
     */
    double countQS

    static belongsTo = [
        fastqcProcessedFile: FastqcProcessedFile
    ]

    static constraints = {
        countQS(min: 0D)
    }
}
