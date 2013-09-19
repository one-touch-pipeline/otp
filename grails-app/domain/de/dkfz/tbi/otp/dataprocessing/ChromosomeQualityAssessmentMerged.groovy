package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry

/**
 * Class to represent chromosomes that will be considered independently (1 to 22, X, Y and M)
 * for merged bam files
 */
class ChromosomeQualityAssessmentMerged extends AbstractQualityAssessment {

    /**
     * Name of the {@link ReferenceGenomeEntry} used in the BAM file where the data to identify the chromosome is read from
     * (depends on the reference used to align the reads)
     */
    String chromosomeName

    static belongsTo = [
        qualityAssessmentMergedPass: QualityAssessmentMergedPass
    ]
}

