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


    void testFindLibraryPreparationKitByNameOrAliasUsingKitName() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        assertEquals(libraryPreparationKitSynonym.libraryPreparationKit, libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(LIBRARY_PREPARATION_KIT))
    }


    void testFindLibraryPreparationKitByNameOrAliasUsingAliasName() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        assertEquals(libraryPreparationKitSynonym.libraryPreparationKit, libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(LIBRARY_PREPARATION_KIT_NAME))
    }


    void testFindLibraryPreparationKitByNameOrAliasUsingUnknownName() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        assertNull(libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias("UNKNOWN"))
    }


    void testFindLibraryPreparationKitByNameOrAliasUsingNull() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        shouldFail(IllegalArgumentException) { libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(null) }
    }



    LibraryPreparationKitSynonym createLibraryPreparationKitSynonym() {
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                        name: LIBRARY_PREPARATION_KIT
                        )
        assertNotNull(libraryPreparationKit.save([flush: true]))
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
                        name: LIBRARY_PREPARATION_KIT_NAME,
                        libraryPreparationKit: libraryPreparationKit)
        assertNotNull(libraryPreparationKitSynonym.save([flush: true]))
        return libraryPreparationKitSynonym
    }

}

