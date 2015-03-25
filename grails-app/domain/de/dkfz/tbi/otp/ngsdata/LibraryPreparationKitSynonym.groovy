package de.dkfz.tbi.otp.ngsdata

/**
 * represents different spellings of the {@link LibraryPreparationKit}
 *
 */
class LibraryPreparationKitSynonym {

    /**
     * possible spelling of the related {@link LibraryPreparationKit}
     */
    String name

    static belongsTo = [
        libraryPreparationKit : LibraryPreparationKit
    ]

    static constraints = {
        name(unique: true, blank: false)
    }

    static mapping = {
        libraryPreparationKit index: "library_preparation_kit_synonym_library_preparation_kit_idx"
    }
}
