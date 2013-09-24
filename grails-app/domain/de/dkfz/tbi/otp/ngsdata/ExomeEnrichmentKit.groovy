package de.dkfz.tbi.otp.ngsdata

/**
 * This class represents Exome enrichment kits which are used
 * for the library creation for exome sequencing purpose.
 *
 *
 */
class ExomeEnrichmentKit {

    /**
     * This is supposed to be the human readable name of the kit.
     * It has to contain the manufacturer + kit name + kit version
     *
     * example: 'Agilent SureSelect V4+UTRs'
     */
    String name

    static constraints = {
        name(unique: true, blank: false)
    }

    public String toString() {
        return name
    }
}
