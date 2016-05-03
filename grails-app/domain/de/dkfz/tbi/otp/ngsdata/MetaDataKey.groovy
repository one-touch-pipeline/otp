package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class MetaDataKey implements Entity {

    String name
    static constraints = { name(unique: true) }

    String toString() {
        name
    }
}
