package de.dkfz.tbi.otp.ngsdata

class SampleIdentifier {

    String name
    static belongsTo = [sample: Sample]

    static constraints = {
        name(unique: true, nullable: false,blank: false, minSize: 3,
            validator: {String val ->
                //should neither start nor end with a space
                return !val.startsWith(' ') && !val.endsWith(' ')
            }
        )
        sample()
    }

    String toString() {
        name
    }

    static mapping = {
        sample index: "sample_identifier_sample_idx"
    }
}
