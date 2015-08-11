package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*

class LibraryPreparationKitService {

    /**
     * look up the {@link LibraryPreparationKit} by its name and by its aliases ({@link LibraryPreparationKitSynonym})
     *
     * @param nameOrAlias the name used for look up
     * @return the found {@link LibraryPreparationKit} or <code>null</code>
     */
    public LibraryPreparationKit findLibraryPreparationKitByNameOrAlias(String nameOrAlias) {
        notNull(nameOrAlias, "the input 'nameOrAlias' is null")
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.findByName(nameOrAlias)
        if (!libraryPreparationKit) { //not found by name, try aliases
            libraryPreparationKit = LibraryPreparationKitSynonym.findByName(nameOrAlias)?.libraryPreparationKit
        }
        return libraryPreparationKit
    }

}
