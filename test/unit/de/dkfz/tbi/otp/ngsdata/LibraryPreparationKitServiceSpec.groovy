package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


@Mock([
        LibraryPreparationKit,
])
class LibraryPreparationKitServiceSpec extends MetadataFieldsServiceSpec<LibraryPreparationKit> {
    LibraryPreparationKitService libraryPreparationKitService = new LibraryPreparationKitService()

    final static String SHORT_DISPLAY_NAME = "LPK"
    final static String OTHER_SHORT_DISPLAY_NAME = "DLPK"
    final static String ADAPTER_FILE = "/file.fa"
    final static String ADAPTER_SEQUENCE = "ATGC"

    def setup() {
        properties = [
                shortDisplayName                : SHORT_DISPLAY_NAME,
                adapterFile                     : ADAPTER_FILE,
                reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
        ]
        otherProperties = [
                shortDisplayName                : OTHER_SHORT_DISPLAY_NAME,
                adapterFile                     : ADAPTER_FILE,
                reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
        ]
    }

    void "test createLibraryPreparationKit succeeds"() {
        given:
        service.create(OTHER_NAME,
                [
                        shortDisplayName                : OTHER_SHORT_DISPLAY_NAME,
                        adapterFile                     : ADAPTER_FILE,
                        reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
                ]
        )

        when:
        service.create(name,
                [
                        shortDisplayName                : shortDisplayName,
                        adapterFile                     : adapterFile,
                        reverseComplementAdapterSequence: adapterSequence,
                ]
        )

        then:
        exactlyOneElement(LibraryPreparationKit.findAllByNameAndShortDisplayNameAndAdapterFileAndReverseComplementAdapterSequence(OTHER_NAME, OTHER_SHORT_DISPLAY_NAME, ADAPTER_FILE, ADAPTER_SEQUENCE))

        where:
        name | shortDisplayName   | adapterFile  | adapterSequence
        NAME | SHORT_DISPLAY_NAME | ADAPTER_FILE | ADAPTER_SEQUENCE
        NAME | SHORT_DISPLAY_NAME | null         | ADAPTER_SEQUENCE
        NAME | SHORT_DISPLAY_NAME | ADAPTER_FILE | null
    }

    void "test createLibraryPreparationKit fails with null as argument"() {
        when:
        service.create(name,
                [
                        shortDisplayName                : shortDisplayName,
                        adapterFile                     : adapterFile,
                        reverseComplementAdapterSequence: adapterSequence,
                ]
        )

        then:
        thrown(AssertionError)

        where:
        name | shortDisplayName   | adapterFile  | adapterSequence
        null | SHORT_DISPLAY_NAME | ADAPTER_FILE | ADAPTER_SEQUENCE
        NAME | null               | ADAPTER_FILE | ADAPTER_SEQUENCE
    }

    void "test createLibraryPreparationKit fails with existing names"() {
        given:
        service.create(NAME,
                [
                        shortDisplayName                : SHORT_DISPLAY_NAME,
                        adapterFile                     : ADAPTER_FILE,
                        reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
                ]
        )

        when:
        service.create(name,
                [
                        shortDisplayName                : shortDisplayName,
                        adapterFile                     : adapterFile,
                        reverseComplementAdapterSequence: adapterSequence,
                ]
        )

        then:
        thrown(AssertionError)

        where:
        name       | shortDisplayName         | adapterFile  | adapterSequence
        NAME       | OTHER_SHORT_DISPLAY_NAME | ADAPTER_FILE | ADAPTER_SEQUENCE
        OTHER_NAME | SHORT_DISPLAY_NAME       | ADAPTER_FILE | ADAPTER_SEQUENCE
    }


    void "test addAdapterFileToLibraryPreparationKit succeeds"() {
        given:
        LibraryPreparationKit kit = service.create(NAME,
                [
                        shortDisplayName                : SHORT_DISPLAY_NAME,
                        adapterFile                     : null,
                        reverseComplementAdapterSequence: null,
                ]
        )

        when:
        service.addAdapterFileToLibraryPreparationKit(kit, ADAPTER_FILE)

        then:
        kit.adapterFile == ADAPTER_FILE
    }

    void "test addAdapterFileToLibraryPreparationKit fails"() {
        given:
        LibraryPreparationKit kit = service.create(NAME,
                [
                        shortDisplayName                : SHORT_DISPLAY_NAME,
                        adapterFile                     : null,
                        reverseComplementAdapterSequence: null,
                ]
        )

        when:
        service.addAdapterFileToLibraryPreparationKit(kit, adapterFile)

        then:
        thrown(AssertionError)

        where:
        adapterFile << [null, ""]
    }

    void "test addAdapterSequenceToLibraryPreparationKit"() {
        given:
        LibraryPreparationKit kit = service.create(NAME,
                [
                        shortDisplayName                : SHORT_DISPLAY_NAME,
                        adapterFile                     : null,
                        reverseComplementAdapterSequence: null,
                ]
        )

        when:
        service.addAdapterSequenceToLibraryPreparationKit(kit, ADAPTER_SEQUENCE)

        then:
        kit.reverseComplementAdapterSequence == ADAPTER_SEQUENCE
    }

    void "test addAdapterSequenceToLibraryPreparationKit fails"() {
        given:
        LibraryPreparationKit kit = service.create(NAME,
                [
                        shortDisplayName                : SHORT_DISPLAY_NAME,
                        adapterFile                     : null,
                        reverseComplementAdapterSequence: null,
                ]
        )

        when:
        service.addAdapterSequenceToLibraryPreparationKit(kit, adapterSequence)

        then:
        thrown(AssertionError)

        where:
        adapterSequence << [null, ""]
    }

    protected MetadataFieldsService getService() {
        return libraryPreparationKitService
    }
}
