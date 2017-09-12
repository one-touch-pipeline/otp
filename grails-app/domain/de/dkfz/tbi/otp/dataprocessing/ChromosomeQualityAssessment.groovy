package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.hibernate.*

/**
 * Class to represent chromosomes that will be considered independently (1 to 22, X, Y and M)
 * for single lane
 */
class ChromosomeQualityAssessment extends QaJarQualityAssessment {

    /**
     * Name of the {@link ReferenceGenomeEntry} used in the BAM file where the data to identify the chromosome is read from
     * (depends on the reference used to align the reads)
     */
    String chromosomeName

    static belongsTo = [
        qualityAssessmentPass: QualityAssessmentPass
    ]

    static constraints = {
        qualityAssessmentPass(validator: {
            ProcessedBamFile.isAssignableFrom(Hibernate.getClass(it.processedBamFile))
        })
    }
}
