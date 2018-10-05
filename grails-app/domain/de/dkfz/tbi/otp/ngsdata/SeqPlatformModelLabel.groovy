package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

/**
 * This class stores the sequencing platform models which were used for sequencing the data.
 * Furthermore a list of possible importAliases is referenced.
 */
class SeqPlatformModelLabel implements Entity {

    String name

    static constraints = {
        name unique: true, blank: false
    }

    static hasMany = [importAlias : String]

    @Override
    String toString() {
        return name
    }
}
