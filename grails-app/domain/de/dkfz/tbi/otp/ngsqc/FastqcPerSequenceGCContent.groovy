package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.ngsdata.DataFile

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
        dataFile: DataFile
    ]

    static constraints = {
        percentageOfGC(min: 0, max:100)
    }
}
