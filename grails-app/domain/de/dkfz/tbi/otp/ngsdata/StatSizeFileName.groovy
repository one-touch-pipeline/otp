package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class StatSizeFileName implements Entity {

    String name

    static constraints = {
        name(unique: 'referenceGenome')
    }

    static belongsTo = [
            referenceGenome: ReferenceGenome
    ]

}
