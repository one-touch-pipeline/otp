package de.dkfz.tbi.otp.dataprocessing

import org.hibernate.*

/**
 * To be extended later on
 * Class to represent the data for the entire set of chromosomes (1 to 22, X, Y and M) as one
 * for single lane
 */
class OverallQualityAssessment extends QaJarQualityAssessment {

    static belongsTo = [
        qualityAssessmentPass: QualityAssessmentPass
    ]

    static constraints = {
        qualityAssessmentPass(validator: {
            ProcessedBamFile.isAssignableFrom(Hibernate.getClass(it.processedBamFile))
        })
    }

    static mapping = {
        qualityAssessmentPass index: "abstract_quality_assessment_quality_assessment_pass_idx"
    }
}
