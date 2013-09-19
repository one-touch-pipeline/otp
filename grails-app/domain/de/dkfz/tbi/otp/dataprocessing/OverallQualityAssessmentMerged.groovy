package de.dkfz.tbi.otp.dataprocessing

/**
 * To be extended later on
 * Class to represent the data for the entire set of chromosomes (1 to 22, X, Y and M) as one
 * for merged bam file
 */
class OverallQualityAssessmentMerged extends AbstractQualityAssessment {

    static belongsTo = [
        qualityAssessmentMergedPass: QualityAssessmentMergedPass
    ]
}
