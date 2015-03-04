package de.dkfz.tbi.otp.ngsdata

/**
 * This class stores the sequencing kits which were used to prepare the sequencing machines.
 *
 */
class SequencingKit {

    String name

    static constraints = {
        name(unique: true, blank: false)
    }

    public String toString() {
        return name
    }
}
