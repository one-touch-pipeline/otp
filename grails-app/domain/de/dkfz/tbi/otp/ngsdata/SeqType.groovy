package de.dkfz.tbi.otp.ngsdata

class SeqType {

    String name
    String libraryLayout
    String dirName

    static constraints = {
        name(blank: false)
        libraryLayout(blank: false)
        dirName(blank: false)
    }

    /**
     * Retrieves the unique natural Id (human readable)
     * Should not be changed, since this is stored at the database.
     */
    String getNaturalId() {
        return "${name}_${libraryLayout}"
    }

    String toString() {
        "${name} ${libraryLayout}"
    }
}
