package de.dkfz.tbi.otp.dataprocessing

class RoddyMergedBamQa extends RoddyQualityAssessment {

    static constraints = {
        chromosome(unique: ['qualityAssessmentMergedPass', 'class'])
    }
}
