package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import org.springframework.security.access.prepost.PreAuthorize

class LibraryPreparationKitService {

    /**
     * look up the {@link LibraryPreparationKit} by its name and by its aliases ({@link LibraryPreparationKitSynonym})
     *
     * @param nameOrAlias the name used for look up
     * @return the found {@link LibraryPreparationKit} or <code>null</code>
     */
    public static LibraryPreparationKit findLibraryPreparationKitByNameOrAlias(String nameOrAlias) {
        notNull(nameOrAlias, "the input 'nameOrAlias' is null")
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.findByName(nameOrAlias)
        if (!libraryPreparationKit) { //not found by name, try aliases
            libraryPreparationKit = LibraryPreparationKitSynonym.findByName(nameOrAlias)?.libraryPreparationKit
        }
        return libraryPreparationKit
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public LibraryPreparationKit createLibraryPreparationKit(String name, String shortDisplayName, String adapterFile, String adapterSequence) {
        assert name : "name must not be null"
        assert shortDisplayName : "shortDisplayName must not be null"
        assert !hasLibraryPreparationKitByNameOrAlias(name) : "The LibraryPreparationKit '${name}' exists already"
        assert !LibraryPreparationKit.findByShortDisplayName(shortDisplayName) : "The shortdisplayname '${shortDisplayName}' exists already"
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                name: name,
                shortDisplayName: shortDisplayName,
                adapterFile: adapterFile ?: null,
                adapterSequence: adapterSequence ?: null,
        )
        assert libraryPreparationKit.save(flush: true, failOnError: true)
        return libraryPreparationKit
    }

    public static boolean hasLibraryPreparationKitByNameOrAlias(String nameOrAlias) {
        assert nameOrAlias: "the input nameoralias '${nameOrAlias}' is null"
        return LibraryPreparationKit.findByName(nameOrAlias) || LibraryPreparationKitSynonym.findByName(nameOrAlias)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public LibraryPreparationKit addAdapterFileToLibraryPreparationKit(LibraryPreparationKit libraryPreparationKit, String adapterFile) {
        assert libraryPreparationKit : "libraryPreparationKit must not be null"
        assert adapterFile : "adapterFile must not be null"
        libraryPreparationKit.adapterFile = adapterFile
        assert libraryPreparationKit.save(flush: true, failOnError: true)
        return libraryPreparationKit
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public LibraryPreparationKit addAdapterSequenceToLibraryPreparationKit(LibraryPreparationKit libraryPreparationKit, String adapterSequence) {
        assert libraryPreparationKit : "libraryPreparationKit must not be null"
        assert adapterSequence : "adapterSequence must not be null"
        libraryPreparationKit.adapterSequence = adapterSequence
        assert libraryPreparationKit.save(flush: true, failOnError: true)
        return libraryPreparationKit
    }
}
