package de.dkfz.tbi.otp.ngsdata

class SampleIdentifier {

    String name
    static belongsTo = [sample: Sample]

    static constraints = {
        name(unique: true)    // should be unique
        sample()
    }

    String toString() {
        name
    }
}
