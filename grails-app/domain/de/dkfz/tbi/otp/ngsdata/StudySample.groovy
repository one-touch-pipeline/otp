package de.dkfz.tbi.otp.ngsdata

class StudySample {
    String studyIdentifier
    static belongsTo = [
        study: Study,
        individual: Individual
    ]
    static constraints = {
        individual(nullable: true)
    }

    static mapping = {
        study index: "study_sample_study_idx"
        individual index: "study_sample_individual_idx"
    }
}
