package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.ngsdata.DataFile

/**
 * Domain class to store the data from several fastqc modules
 * Different modules and corresponding column fields that will be grouped on this class
 * Per base sequence analysis:  Mean, Median, Lower Quartile, Upper Quartile, 10th Percentile, 90th Percentile
 * Per base sequence content :  G, A, T, C
 * Per base GC content       :  %GC
 * Per base N content        :  N-Count
 * 
 */
class FastqcPerBaseSequenceAnalysis {

    /**
     * Cycles of the sequencing (corresponds to column 'Base')
     */
    int cycle

    /**
     * Mean quality score value for this column
     */
    double mean

    /**
     * Median quality score
     */
    double median

    /**
     * Lower quartile 'q1' quality score
     */
    double q1

    /**
     * Upper quartile 'q3' quality score
     */
    double q3

    /**
     * 10th percentile 'Left-Whisker' value
     */
    double p10th

    /**
     * 90th percentile 'Right-Whisker' value
     */
    double p90th

    double countOfNucleotideA
    double countOfNucleotideC
    double countOfNucleotideG
    double countOfNucleotideT
    double countOfNucleotideN

    /**
     * Percentage of GC
     */
    double percentageOfGC

    static belongsTo = [
        dataFile : DataFile
    ]

    static constraints = {
        cycle()
        mean()
        median()
        q1()
        q3()
        p10th()
        p90th()
        countOfNucleotideA(min:0D)
        countOfNucleotideC(min:0D)
        countOfNucleotideG(min:0D)
        countOfNucleotideT(min:0D)
        countOfNucleotideN(min:0D)
        percentageOfGC(min:0D,max:100D)
    }
}
