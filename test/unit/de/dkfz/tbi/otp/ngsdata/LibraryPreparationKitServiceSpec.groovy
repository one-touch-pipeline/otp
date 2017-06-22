package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


@Mock([
        LibraryPreparationKit,
        LibraryPreparationKitSynonym,
])
class LibraryPreparationKitServiceSpec extends Specification {
    LibraryPreparationKitService service = new LibraryPreparationKitService()

    final static String LIBRARY_PREPARATION_KIT_NAME = "LibraryPreparationKitName"
    final static String DIFFERENT_LIBRARY_PREPARATION_KIT_NAME = "DifferentLibraryPreparationKitName"
    final static String SHORT_DISPLAY_NAME = "LPK"
    final static String DIFFERENT_SHORT_DISPLAY_NAME = "DLPK"
    final static String ADAPTER_FILE = "/file.fa"
    final static String ADAPTER_SEQUENCE = "ATGC"
    final static String LIBRARY_PREPARATION_KIT_SYNONYM = "LibraryPreparationKitSynonym"


    void "test createLibraryPreparationKit succeeds"() {
        given:
        service.createLibraryPreparationKit(DIFFERENT_LIBRARY_PREPARATION_KIT_NAME, DIFFERENT_SHORT_DISPLAY_NAME, ADAPTER_FILE, ADAPTER_SEQUENCE)

        when:
        service.createLibraryPreparationKit(name, shortDisplayName, adapterFile, adapterSequence)

        then:
        exactlyOneElement(LibraryPreparationKit.findAllByNameAndShortDisplayNameAndAdapterFileAndReverseComplementAdapterSequence(DIFFERENT_LIBRARY_PREPARATION_KIT_NAME, DIFFERENT_SHORT_DISPLAY_NAME, ADAPTER_FILE, ADAPTER_SEQUENCE))

        where:
        name                         | shortDisplayName   | adapterFile  | adapterSequence
        LIBRARY_PREPARATION_KIT_NAME | SHORT_DISPLAY_NAME | ADAPTER_FILE | ADAPTER_SEQUENCE
        LIBRARY_PREPARATION_KIT_NAME | SHORT_DISPLAY_NAME | null         | ADAPTER_SEQUENCE
        LIBRARY_PREPARATION_KIT_NAME | SHORT_DISPLAY_NAME | ADAPTER_FILE | null
    }

    void "test createLibraryPreparationKit fails with null as argument"() {
        when:
        service.createLibraryPreparationKit(name, shortDisplayName, adapterFile, adapterSequence)

        then:
        thrown(AssertionError)

        where:
        name                         | shortDisplayName   | adapterFile  | adapterSequence
        null                         | SHORT_DISPLAY_NAME | ADAPTER_FILE | ADAPTER_SEQUENCE
        LIBRARY_PREPARATION_KIT_NAME | null               | ADAPTER_FILE | ADAPTER_SEQUENCE
    }

    void "test createLibraryPreparationKit fails with existing names"() {
        given:
        service.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME, SHORT_DISPLAY_NAME, ADAPTER_FILE, ADAPTER_SEQUENCE)

        when:
        service.createLibraryPreparationKit(name, shortDisplayName, adapterFile, adapterSequence)

        then:
        thrown(AssertionError)

        where:
        name                                   | shortDisplayName             | adapterFile  | adapterSequence
        LIBRARY_PREPARATION_KIT_NAME           | DIFFERENT_SHORT_DISPLAY_NAME | ADAPTER_FILE | ADAPTER_SEQUENCE
        DIFFERENT_LIBRARY_PREPARATION_KIT_NAME | SHORT_DISPLAY_NAME           | ADAPTER_FILE | ADAPTER_SEQUENCE
    }

    void "test findLibraryPreparationKitByNameOrAlias succeeds"() {
        given:
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()

        expect:
        libraryPreparationKitSynonym.libraryPreparationKit == service.findLibraryPreparationKitByNameOrAlias(name)

        where:
        name << [LIBRARY_PREPARATION_KIT_NAME, LIBRARY_PREPARATION_KIT_SYNONYM]
    }

    void "test findLibraryPreparationKitByNameOrAlias succeeds returns null if no LPK exists"() {
        given:
        createLibraryPreparationKitSynonym()

        expect:
        !service.findLibraryPreparationKitByNameOrAlias("UNKNOWN")
    }

    void "test findLibraryPreparationKitByNameOrAlias fails with null as argument"() {
        given:
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()

        when:
        libraryPreparationKitSynonym.libraryPreparationKit == service.findLibraryPreparationKitByNameOrAlias(null)

        then:
        thrown(IllegalArgumentException)
    }

    static LibraryPreparationKitSynonym createLibraryPreparationKitSynonym() {
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                name: LIBRARY_PREPARATION_KIT_NAME,
                shortDisplayName: SHORT_DISPLAY_NAME,
        )
        assertNotNull(libraryPreparationKit.save([flush: true]))
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
                name: LIBRARY_PREPARATION_KIT_SYNONYM,
                libraryPreparationKit: libraryPreparationKit,
        )
        assertNotNull(libraryPreparationKitSynonym.save([flush: true]))
        return libraryPreparationKitSynonym
    }


    void "test addAdapterFileToLibraryPreparationKit succeeds"() {
        given:
        LibraryPreparationKit kit = service.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME, SHORT_DISPLAY_NAME, null, null)

        when:
        service.addAdapterFileToLibraryPreparationKit(kit, ADAPTER_FILE)

        then:
        kit.adapterFile == ADAPTER_FILE
    }

    void "test addAdapterFileToLibraryPreparationKit fails"() {
        given:
        LibraryPreparationKit kit = service.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME, SHORT_DISPLAY_NAME, null, null)

        when:
        service.addAdapterFileToLibraryPreparationKit(kit, adapterFile)

        then:
        thrown(AssertionError)

        where:
        adapterFile << [null, ""]
    }

    void "test addAdapterSequenceToLibraryPreparationKit"() {
        given:
        LibraryPreparationKit kit = service.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME, SHORT_DISPLAY_NAME, null, null)

        when:
        service.addAdapterSequenceToLibraryPreparationKit(kit, ADAPTER_SEQUENCE)

        then:
        kit.reverseComplementAdapterSequence == ADAPTER_SEQUENCE
    }

    void "test addAdapterSequenceToLibraryPreparationKit fails"() {
        given:
        LibraryPreparationKit kit = service.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME, SHORT_DISPLAY_NAME, null, null)

        when:
        service.addAdapterSequenceToLibraryPreparationKit(kit, adapterSequence)

        then:
        thrown(AssertionError)

        where:
        adapterSequence << [null, ""]
    }
}
