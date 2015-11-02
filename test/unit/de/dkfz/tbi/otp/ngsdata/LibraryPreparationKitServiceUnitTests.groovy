package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.job.processing.ProcessingException

@TestMixin(GrailsUnitTestMixin)
@TestFor(LibraryPreparationKitService)
@Build([
    DataFile,
    LibraryPreparationKitSynonym,
    ExomeSeqTrack,
    FileType,
    Realm,
    ReferenceGenomeProjectSeqType,
    RunSegment,
    SeqTrack,
    SeqPlatform,
    SeqPlatformGroup,
])
class LibraryPreparationKitServiceUnitTests {

    LibraryPreparationKitService libraryPreparationKitService
    TestData testData

    final static String LIBRARY_PREPARATION_KIT ="LibraryPreparationKit"

    final static String LIBRARY_PREPARATION_KIT_NAME = "LibraryPreparationKitName"

    final static String DIFFERENT_LIBRARY_PREPARATION_KIT_NAME = "DifferentLibraryPreparationKitName"

    final static String SHORT_DISPLAY_NAME ="LPK"

    final static String DIFFERENT_SHORT_DISPLAY_NAME ="DLPK"

    @Before
    public void setUp() throws Exception {
        testData = new TestData()
        testData.createObjects()
        testData.seqTrack.seqType = testData.exomeSeqType
        assertNotNull(testData.seqTrack.save(flush: true))
        libraryPreparationKitService = new LibraryPreparationKitService()
    }


    @After
    public void tearDown() throws Exception {
        libraryPreparationKitService = null
        testData = null
    }


    @Test
    void testFindLibraryPreparationKitByNameOrAliasUsingKitName() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        assertEquals(libraryPreparationKitSynonym.libraryPreparationKit, libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(LIBRARY_PREPARATION_KIT))
    }


    @Test
    void testFindLibraryPreparationKitByNameOrAliasUsingAliasName() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        assertEquals(libraryPreparationKitSynonym.libraryPreparationKit, libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(LIBRARY_PREPARATION_KIT_NAME))
    }


    @Test
    void testFindLibraryPreparationKitByNameOrAliasUsingUnknownName() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        assertNull(libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias("UNKNOWN"))
    }


    @Test
    void testFindLibraryPreparationKitByNameOrAliasUsingNull() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        shouldFail(IllegalArgumentException) { libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(null) }
    }

    @Test
    void testCreateLibraryPreparationKitUsingKitAndDisplayName() {
        assertEquals(
                libraryPreparationKitService.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT, SHORT_DISPLAY_NAME),
                libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(LIBRARY_PREPARATION_KIT)
        )
    }

    @Test
    void testCreateLibraryPreparationKitUsingKitTwiceAndDifferentDisplayNames() {
        libraryPreparationKitService.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT, SHORT_DISPLAY_NAME)
        shouldFail(AssertionError) { libraryPreparationKitService.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT, DIFFERENT_SHORT_DISPLAY_NAME) }
    }

    @Test
    void testCreateLibraryPreparationKitUsingDisplayNameTwiceAndDifferentKits() {
        libraryPreparationKitService.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT, SHORT_DISPLAY_NAME)
        shouldFail(AssertionError) { libraryPreparationKitService.createLibraryPreparationKit(DIFFERENT_LIBRARY_PREPARATION_KIT_NAME, SHORT_DISPLAY_NAME) }
    }

    @Test
    void testCreateLibraryPreparationKitUsingDifferentKitsAndDifferentDisplayName() {
        libraryPreparationKitService.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT, SHORT_DISPLAY_NAME)
        libraryPreparationKitService.createLibraryPreparationKit(DIFFERENT_LIBRARY_PREPARATION_KIT_NAME, DIFFERENT_SHORT_DISPLAY_NAME)
    }

    @Test
    void testCreateLibraryPreparationKitUsingNull() {
        shouldFail(AssertionError) { libraryPreparationKitService.createLibraryPreparationKit(null, null) }
    }

    @Test
    void testCreateLibraryPreparationKitUsingKitAndNull() {
        shouldFail(AssertionError) { libraryPreparationKitService.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT, null) }
    }

    @Test
    void testCreateLibraryPreparationKitUsingNullAndDisplayName() {
        shouldFail(AssertionError) { libraryPreparationKitService.createLibraryPreparationKit(null, SHORT_DISPLAY_NAME) }
    }



    LibraryPreparationKitSynonym createLibraryPreparationKitSynonym() {
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                        name: LIBRARY_PREPARATION_KIT,
                        shortDisplayName: LIBRARY_PREPARATION_KIT,
                        )
        assertNotNull(libraryPreparationKit.save([flush: true]))
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
                        name: LIBRARY_PREPARATION_KIT_NAME,
                        libraryPreparationKit: libraryPreparationKit)
        assertNotNull(libraryPreparationKitSynonym.save([flush: true]))
        return libraryPreparationKitSynonym
    }

}

