package de.dkfz.tbi.otp.ngsdata



class SeqType {

    /**
     * One of {@link SeqTypeNames#seqTypeName}.
     * Used in file system paths, for example by ProcessedMergedBamFileService.fileNameNoSuffix(ProcessedMergedBamFile).
     * Used in the GUI unless {@link #alias} is set.
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
        libraryLayout(blank: false)  // TODO: OTP-1123: unique constraint for (name, libraryLayout)
        dirName(blank: false, unique: 'libraryLayout')  // TODO: OTP-1124: unique constraint for (dirName, libraryLayoutDirName)
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

    String getLibraryLayoutDirName() {
        return getLibraryLayoutDirName(libraryLayout)
    }

    static String getLibraryLayoutDirName(final String libraryLayout) {
        return libraryLayout.toLowerCase()
    }

    String toString() {
        "${aliasOrName} ${libraryLayout}"
    }
}
