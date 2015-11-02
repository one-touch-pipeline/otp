package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import org.springframework.security.access.prepost.PreAuthorize

class LibraryPreparationKitSynonymService {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public LibraryPreparationKitSynonym createLibraryPreparationKitSynonym(String alias, String name){
        assert alias : "the input alias '${alias}' must not be null"
        assert name  : "the input name '${name}' must not be null"
        assert LibraryPreparationKit.findByName(name): "The LibraryPreparationKit '${name}' does not exist"
        assert !LibraryPreparationKitService.hasLibraryPreparationKitByNameOrAlias(alias) : "The LibraryPreparationKit '${alias}' exists already"
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
                name: alias,
                libraryPreparationKit: LibraryPreparationKit.findByName(name)
        )
        assert libraryPreparationKitSynonym.save(flush: true, failOnError: true)
        return libraryPreparationKitSynonym
    }
}