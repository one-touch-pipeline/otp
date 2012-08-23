package de.dkfz.tbi.otp.ngsqc

/**
 * Domain class to store the modules from fastqc files
 * 
 */
class FastqcModule {

    /**
     * Name of the module
     */
    String name

    /**
     * Identifier of the module
     */
    String identifier

    static constraints = {
        name(blank: false)
        identifier(blank: false, unique: true)
    }
}
