package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

/**
 * This class stores the sequencing kits which were used to prepare the sequencing machines.
 * Furthermore a list of possible aliases is referenced.
 * Should be called "Chemical Version" in the user interface.
 *
 */
class SequencingKitLabel implements Entity {

    String name

    static hasMany = [alias : String]

    static constraints = {
        name(unique: true, blank: false)
    }

    public String toString() {
        return name
    }
}
