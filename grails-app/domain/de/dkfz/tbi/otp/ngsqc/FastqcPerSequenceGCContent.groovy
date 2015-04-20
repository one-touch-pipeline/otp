package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Domain class to store the data from Fastqc "Per Sequence GC Content" module
 */
class FastqcPerSequenceGCContent {

    /**
     * Percentage of GC content (Column Label in fastqc file is '#GC Content')
     */
    int percentageOfGC

    /**
     * Mean quality score value for this column (Column label in fastqc file is 'Count' which is a reserved word)
     */
    double countGC

    static belongsTo = [
        fastqcProcessedFile: FastqcProcessedFile
    ]

    static constraints = {
        percentageOfGC(min: 0, max:100)
    }

    static mapping = {
        fastqcProcessedFile index: "fastqc_per_sequencegccontent_fastqc_processed_file_idx"
    }
}
