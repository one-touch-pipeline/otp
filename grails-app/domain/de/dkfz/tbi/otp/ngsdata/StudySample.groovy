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
}
