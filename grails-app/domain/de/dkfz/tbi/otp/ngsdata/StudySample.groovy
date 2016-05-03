package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class StudySample implements Entity {
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
