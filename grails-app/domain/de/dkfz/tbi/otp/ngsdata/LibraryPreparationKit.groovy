package de.dkfz.tbi.otp.ngsdata

/**
 * This class represents library preparation kits which are used
 * for the library creation for sequencing purpose.
 *
 *
 */
class LibraryPreparationKit {

    /**
     * This is supposed to be the canonical human readable name of the kit.
     * It has to contain the manufacturer + kit name + kit version
     *
     * example: 'Agilent SureSelect V4+UTRs'
     *
     * For alternative spellings etc., see {@link LibraryPreparationKitSynonym}
     */
    String name

    String shortDisplayName

    static constraints = {
        name(unique: true, blank: false)
        shortDisplayName(unique: true, blank: false)
    }

    public String toString() {
        return name
    }
}
