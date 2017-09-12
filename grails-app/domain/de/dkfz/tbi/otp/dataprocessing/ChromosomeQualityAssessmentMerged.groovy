package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.hibernate.*

/**
 * Class to represent chromosomes that will be considered independently (1 to 22, X, Y and M)
 * for merged bam files
 */
class ChromosomeQualityAssessmentMerged extends QaJarQualityAssessment {

    /**
     * Name of the {@link ReferenceGenomeEntry} used in the BAM file where the data to identify the chromosome is read from
     * (depends on the reference used to align the reads)
     */
    String chromosomeName

    static belongsTo = [
        qualityAssessmentMergedPass: QualityAssessmentMergedPass
    ]

    static constraints = {
        qualityAssessmentMergedPass(validator: {
            ProcessedMergedBamFile.isAssignableFrom(Hibernate.getClass(it.abstractMergedBamFile))
        })
    }

    static mapping = {
        chromosomeName index: "abstract_quality_assessment_chromosome_name_idx"
        //qualityAssessmentMergedPass is defined in OverallQualityAssessmentMerged
    }
}

