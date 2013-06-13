package de.dkfz.tbi.otp.dataprocessing

/**
 * Class to represent chromosomes that will be considered independently (1 to 22, X, Y and M)
 */
class ChromosomeQualityAssessment extends AbstractQualityAssessment {

    /**
     * Name used in the BAM file where the data to identify the chromosome is read from
     * (depends on the reference used to align the reads)
     */
    String chromosomeName
}
