package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.After
import org.junit.Before
import org.junit.Test

@TestFor(LibraryPreparationKitSynonymService)
@Build([
    LibraryPreparationKitSynonym,
])
class LibraryPreparationKitSynonymServiceUnitTests {

    LibraryPreparationKitSynonymService libraryPreparationKitSynonymService
    LibraryPreparationKitService libraryPreparationKitService

    final static String LIBRARY_PREPARATION_KIT ="LibraryPreparationKit"

    final static String LIBRARY_PREPARATION_KIT_SYNONYM = "LibraryPreparationKitSynonym"

    final static String DIFFERENT_LIBRARY_PREPARATION_KIT_SYNONYM = "DifferentLibraryPreparationKitSynonym"

    @Before
    public void setUp() {
        libraryPreparationKitSynonymService = new LibraryPreparationKitSynonymService()
        libraryPreparationKitService = new LibraryPreparationKitService()
        createLibraryPreparationKitSynonym()
    }


    @After
    public void tearDown() {
        libraryPreparationKitSynonymService = null
        libraryPreparationKitService = null
    }


    @Test
    void testCreateLibraryPreparationKitSynonymUsingKitAsKitAndDifKitAsAlias() {
        assertEquals(
                libraryPreparationKitSynonymService.createLibraryPreparationKitSynonym(DIFFERENT_LIBRARY_PREPARATION_KIT_SYNONYM, LIBRARY_PREPARATION_KIT),
                LibraryPreparationKitSynonym.findByName(DIFFERENT_LIBRARY_PREPARATION_KIT_SYNONYM)
        )
    }

    @Test
    void testCreateLibraryPreparationKitSynonymUsingKitAsKitAndKitAsAlias() {
        shouldFail(AssertionError){
            libraryPreparationKitSynonymService.createLibraryPreparationKitSynonym(LIBRARY_PREPARATION_KIT, LIBRARY_PREPARATION_KIT)
        }
    }

    @Test
    void testCreateLibraryPreparationKitSynonymUsingKitAsKitAndKitNameAsAlias() {
        shouldFail(AssertionError){
            libraryPreparationKitSynonymService.createLibraryPreparationKitSynonym(LIBRARY_PREPARATION_KIT_SYNONYM, LIBRARY_PREPARATION_KIT)
        }
    }

    @Test
    void testCreateLibraryPreparationKitSynonymUsingNullAsKitAndDifKitAsAlias() {
        shouldFail(AssertionError){
            libraryPreparationKitSynonymService.createLibraryPreparationKitSynonym(DIFFERENT_LIBRARY_PREPARATION_KIT_SYNONYM, null)
        }
    }

    @Test
    void testCreateLibraryPreparationKitSynonymUsingKitAsKitAndNullAsAlias() {
        shouldFail(AssertionError){
            libraryPreparationKitSynonymService.createLibraryPreparationKitSynonym(null, LIBRARY_PREPARATION_KIT)
        }
    }


    LibraryPreparationKitSynonym createLibraryPreparationKitSynonym() {
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                name: LIBRARY_PREPARATION_KIT,
                shortDisplayName: LIBRARY_PREPARATION_KIT,
        )
        assert libraryPreparationKit.save(flush: true, failOnError: true)
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
                name: LIBRARY_PREPARATION_KIT_SYNONYM,
                libraryPreparationKit: libraryPreparationKit)
        assert libraryPreparationKitSynonym.save(flush: true, failOnError: true)
        return libraryPreparationKitSynonym
    }


}

