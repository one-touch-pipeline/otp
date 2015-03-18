package de.dkfz.tbi.otp.ngsdata

/**
 * This class stores the sequencing kits which were used to prepare the sequencing machines.
 * Furthermore a list of possible aliases is referenced.
 *
 */
class SequencingKitLabel {

    String name

    static hasMany = [alias : String]

    static constraints = {
        name(unique: true, blank: false)
    }

    public String toString() {
        return name
    }
}
