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





    @Test(expected = IllegalArgumentException)
    void testValidateLibraryPreparationKitKitIsNull() {
        libraryPreparationKitService.validateLibraryPreparationKit(testData.sample, null)
    }


    @Test(expected = IllegalArgumentException)
    void testValidateLibraryPreparationKitSampleIsNull() {
        libraryPreparationKitService.validateLibraryPreparationKit(null, new LibraryPreparationKit())
    }


    @Test
    void testValidateLibraryPreparationKitNoSeqTrackInSample() {
        testData.seqTrack.sample = null
        libraryPreparationKitService.validateLibraryPreparationKit(testData.sample, new LibraryPreparationKit())
    }


    @Test
    void testValidateLibraryPreparationKitOneSeqTrack() {
        testData.seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        assertNotNull(testData.seqType.save([flush: true]))
        libraryPreparationKitService.validateLibraryPreparationKit(testData.sample, new LibraryPreparationKit())
    }


    @Test
    void testValidateLibraryPreparationKitOneExomeSeqTrackInSampleWithoutKit() {
        testData.seqTrack.sample = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        libraryPreparationKitService.validateLibraryPreparationKit(testData.sample, new LibraryPreparationKit())
    }


    @Test
    void testValidateLibraryPreparationKitOneExomeSeqTrackInSampleWithSameKit() {
        testData.seqTrack.sample = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        LibraryPreparationKit sameLibraryPreparationKit = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack, sameLibraryPreparationKit)
        libraryPreparationKitService.validateLibraryPreparationKit(testData.sample, sameLibraryPreparationKit)
    }


    @Test(expected = ProcessingException)
    void testValidateLibraryPreparationKitOneExomeSeqTrackInSampleComparedWithDifferentKit() {
        testData.seqTrack.sample = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        LibraryPreparationKit libraryPreparationKit1 = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        LibraryPreparationKit libraryPreparationKit2 = testData.createLibraryPreparationKit(DIFFERENT_LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack, libraryPreparationKit1)
        libraryPreparationKitService.validateLibraryPreparationKit(testData.sample, libraryPreparationKit2)
    }


    @Test(expected = ProcessingException)
    void testValidateLibraryPreparationKitTwoExomeSeqTrackInSampleComparedWithDifferentKit() {
        testData.seqTrack.sample = null
        LibraryPreparationKit libraryPreparationKit1 = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        LibraryPreparationKit libraryPreparationKit2 = testData.createLibraryPreparationKit(DIFFERENT_LIBRARY_PREPARATION_KIT_NAME)
        // create an inconsistent state. This _should_ never be in the database
        // but we want to see if this is detected
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        testData.addKitToExomeSeqTrack(exomeSeqTrack1, libraryPreparationKit1)
        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, libraryPreparationKit2)
        // Even though kit2 already exists in the "DB", it is still invalid because it conflicts with Kit1
        libraryPreparationKitService.validateLibraryPreparationKit(testData.sample, libraryPreparationKit2)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneFirstSeqTrackInSample() {
        testData.seqTrack.seqType = testData.seqType
        assertNotNull(testData.seqTrack.save(flush: true))

        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(testData.runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneFirstExomeSeqTrackInSample() {
        testData.seqTrack.sample = null
        testData.dataFile.seqTrack = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        LibraryPreparationKit libraryPreparationKit = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack, libraryPreparationKit)
        testData.dataFile.seqTrack = exomeSeqTrack
        assertNotNull(testData.dataFile.seqTrack.save(flush: true))

        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(testData.runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldSeqTrackInSample() {
        testData.seqTrack.seqType = testData.seqType
        assertNotNull(testData.seqTrack.save(flush: true))

        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        LibraryPreparationKit libraryPreparationKit = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack, libraryPreparationKit)
        DataFile dataFile = testData.createDataFile(exomeSeqTrack, runSegment)
        assertNotNull(dataFile.save(flush: true))

        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldExomeSeqTrackWithSameKitInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        LibraryPreparationKit libraryPreparationKit = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack1, libraryPreparationKit)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, libraryPreparationKit)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
    }


    @Test(expected = IllegalArgumentException)
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldExomeSeqTrackWithDiffKitInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        LibraryPreparationKit libraryPreparationKit = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack1, libraryPreparationKit)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        LibraryPreparationKit libraryPreparationKit1 = testData.createLibraryPreparationKit(DIFFERENT_LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, libraryPreparationKit1)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldExomeSeqTrackWithNoKitInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        LibraryPreparationKit libraryPreparationKit = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, libraryPreparationKit)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        assertNull(exomeSeqTrack1.libraryPreparationKit)
        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
        assertEquals(libraryPreparationKit, exomeSeqTrack1.libraryPreparationKit)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneFirstExomeSeqTrackWithNoKitInSample() {
        testData.seqTrack.sample = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        testData.dataFile.seqTrack = exomeSeqTrack
        assertNotNull(testData.dataFile.seqTrack.save(flush: true))

        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(testData.runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldExomeSeqTrackWithNoKitsInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackWithNoKitAndOldExomeSeqTrackWithKitInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))
        LibraryPreparationKit libraryPreparationKit = testData.createLibraryPreparationKit(LIBRARY_PREPARATION_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack1, libraryPreparationKit)

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        assertNull(exomeSeqTrack2.libraryPreparationKit)
        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
        assertEquals(libraryPreparationKit, exomeSeqTrack2.libraryPreparationKit)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneRunSegmentContainsBamFiles() {
        testData.seqTrack.seqType = testData.seqType
        assertNotNull(testData.seqTrack.save(flush: true))

        FileType fileType = testData.createFileType(FileType.Type.ALIGNMENT)

        DataFile dataFile = testData.createDataFile(null, testData.runSegment, fileType)
        assertNotNull(dataFile.save(flush: true))

        libraryPreparationKitService.inferKitInformationForOldLaneFromNewLane(testData.runSegment)
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

