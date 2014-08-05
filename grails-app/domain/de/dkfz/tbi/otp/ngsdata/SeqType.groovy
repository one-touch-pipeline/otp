package de.dkfz.tbi.otp.ngsdata

class SeqType {

    /**
     * Used in file system paths. Also used in the GUI unless {@link #alias} is set.
     */
    String name
    String libraryLayout
    String dirName
    /**
     * Can be set to override {@link #name} for display in the GUI.
     */
    String alias
    /**
     * Display name used in the GUI.
     */
    String aliasOrName

    static constraints = {
        name(blank: false)
        libraryLayout(blank: false)
        dirName(blank: false)
        alias(nullable: true, blank: false)
        //For unknown reason the object creation fail, if it is not set as nullable
        aliasOrName(nullable: true, blank: false)
    }

    static mapping = {
        aliasOrName formula: '(CASE WHEN alias IS NOT NULL THEN alias ELSE name END)'
    }

    /**
     * Retrieves the unique natural Id (human readable)
     * Should not be changed, since this is stored at the database.
     */
    String getNaturalId() {
        return "${name}_${libraryLayout}"
    }


    String toString() {
        "${aliasOrName} ${libraryLayout}"
    }
}