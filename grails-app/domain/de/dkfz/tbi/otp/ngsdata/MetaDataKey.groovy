package de.dkfz.tbi.otp.ngsdata

class MetaDataKey {

    String name
    static constraints = { name(unique: true) }

    String toString() {
        name
    }
}
