package de.dkfz.tbi.otp.ngsdata

class StatSizeFileName {

    String name

    static constraints = {
        name(unique: 'referenceGenome')
    }

    static belongsTo = [
            referenceGenome: ReferenceGenome
    ]

}
