package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.CollectionUtils

class LibraryPreparationKitService extends MetadataFieldsService<LibraryPreparationKit> {

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    LibraryPreparationKit addAdapterFileToLibraryPreparationKit(LibraryPreparationKit libraryPreparationKit, String adapterFile) {
        assert libraryPreparationKit : "libraryPreparationKit must not be null"
        assert adapterFile : "adapterFile must not be null"
        libraryPreparationKit.adapterFile = adapterFile
        assert libraryPreparationKit.save(flush: true, failOnError: true)
        return libraryPreparationKit
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    LibraryPreparationKit addAdapterSequenceToLibraryPreparationKit(LibraryPreparationKit libraryPreparationKit, String reverseComplementAdapterSequence) {
        assert libraryPreparationKit : "libraryPreparationKit must not be null"
        assert reverseComplementAdapterSequence : "reverseComplementAdapterSequence must not be null"
        libraryPreparationKit.reverseComplementAdapterSequence = reverseComplementAdapterSequence
        assert libraryPreparationKit.save(flush: true, failOnError: true)
        return libraryPreparationKit
    }

    @Override
    protected LibraryPreparationKit findByName(String name, Map properties = [:]) {
        return CollectionUtils.atMostOneElement(clazz.findAllByNameIlike(name))
    }

    @Override
    protected void checkProperties(Map properties) {
        assert properties.shortDisplayName  : "the input shortDisplayName '${properties.shortDisplayName}' must not be null"
        assert !LibraryPreparationKit.findByShortDisplayName(properties.shortDisplayName) : "The shortdisplayname '${properties.shortDisplayName}' exists already"
    }

    @Override
    protected Class getClazz() {
        return LibraryPreparationKit
    }
}
