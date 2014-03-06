package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.job.processing.ProcessingException

@TestMixin(GrailsUnitTestMixin)
@TestFor(ExomeEnrichmentKitService)
@Mock([ExomeEnrichmentKitService, ExomeEnrichmentKit, ExomeEnrichmentKitSynonym, Sample,
    SeqTrack, Realm, Project, Individual, SampleType, SeqCenter, SeqPlatform, Run, SoftwareTool,
    ExomeSeqTrack, FileType, ReferenceGenome, ReferenceGenomeProjectSeqType, SeqType, RunSegment,
    DataFile])
class ExomeEnrichmentKitServiceUnitTests {

    ExomeEnrichmentKitService exomeEnrichmentKitService
    TestData testData

    final static String EXOME_ENRICHMENT_KIT ="ExomeEnrichmentKit"

    final static String EXOME_ENRICHMENT_KIT_NAME = "ExomeEnrichmentKitName"

    final static String DIFFERENT_EXOME_ENRICHMENT_KIT_NAME = "DifferentExomeEnrichmentKitName"

    @Before
    public void setUp() throws Exception {
        testData = new TestData()
        testData.createObjects()
        testData.seqTrack.seqType = testData.exomeSeqType
        assertNotNull(testData.seqTrack.save(flush: true))
        exomeEnrichmentKitService = new ExomeEnrichmentKitService()
    }


    @After
    public void tearDown() throws Exception {
        exomeEnrichmentKitService = null
        testData = null
    }


    void testFindExomeEnrichmentKitByNameOrAliasUsingKitName() {
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym = createExomeEnrichmentKitSynonym()
        assertEquals(exomeEnrichmentKitSynonym.exomeEnrichmentKit, exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias(EXOME_ENRICHMENT_KIT))
    }


    void testFindExomeEnrichmentKitByNameOrAliasUsingAliasName() {
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym = createExomeEnrichmentKitSynonym()
        assertEquals(exomeEnrichmentKitSynonym.exomeEnrichmentKit, exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias(EXOME_ENRICHMENT_KIT_NAME))
    }


    void testFindExomeEnrichmentKitByNameOrAliasUsingUnknownName() {
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym = createExomeEnrichmentKitSynonym()
        assertNull(exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias("UNKNOWN"))
    }


    void testFindExomeEnrichmentKitByNameOrAliasUsingNull() {
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym = createExomeEnrichmentKitSynonym()
        shouldFail(IllegalArgumentException) { exomeEnrichmentKitService.findExomeEnrichmentKitByNameOrAlias(null) }
    }





    @Test(expected = IllegalArgumentException)
    void testValidateExomeEnrichmentKitKitIsNull() {
        exomeEnrichmentKitService.validateExomeEnrichmentKit(testData.sample, null)
    }


    @Test(expected = IllegalArgumentException)
    void testValidateExomeEnrichmentKitSampleIsNull() {
        exomeEnrichmentKitService.validateExomeEnrichmentKit(null, new ExomeEnrichmentKit())
    }


    @Test
    void testValidateExomeEnrichmentKitNoSeqTrackInSample() {
        testData.seqTrack.sample = null
        exomeEnrichmentKitService.validateExomeEnrichmentKit(testData.sample, new ExomeEnrichmentKit())
    }


    @Test
    void testValidateExomeEnrichmentKitOneSeqTrack() {
        testData.seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        assertNotNull(testData.seqType.save([flush: true]))
        exomeEnrichmentKitService.validateExomeEnrichmentKit(testData.sample, new ExomeEnrichmentKit())
    }


    @Test
    void testValidateExomeEnrichmentKitOneExomeSeqTrackInSampleWithoutKit() {
        testData.seqTrack.sample = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        exomeEnrichmentKitService.validateExomeEnrichmentKit(testData.sample, new ExomeEnrichmentKit())
    }


    @Test
    void testValidateExomeEnrichmentKitOneExomeSeqTrackInSampleWithSameKit() {
        testData.seqTrack.sample = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit sameExomeEnrichmentKit = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack, sameExomeEnrichmentKit)
        exomeEnrichmentKitService.validateExomeEnrichmentKit(testData.sample, sameExomeEnrichmentKit)
    }


    @Test(expected = ProcessingException)
    void testValidateExomeEnrichmentKitOneExomeSeqTrackInSampleComparedWithDifferentKit() {
        testData.seqTrack.sample = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit exomeEnrichmentKit1 = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        ExomeEnrichmentKit exomeEnrichmentKit2 = testData.createEnrichmentKit(DIFFERENT_EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack, exomeEnrichmentKit1)
        exomeEnrichmentKitService.validateExomeEnrichmentKit(testData.sample, exomeEnrichmentKit2)
    }


    @Test(expected = ProcessingException)
    void testValidateExomeEnrichmentKitTwoExomeSeqTrackInSampleComparedWithDifferentKit() {
        testData.seqTrack.sample = null
        ExomeEnrichmentKit exomeEnrichmentKit1 = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        ExomeEnrichmentKit exomeEnrichmentKit2 = testData.createEnrichmentKit(DIFFERENT_EXOME_ENRICHMENT_KIT_NAME)
        // create an inconsistent state. This _should_ never be in the database
        // but we want to see if this is detected
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        testData.addKitToExomeSeqTrack(exomeSeqTrack1, exomeEnrichmentKit1)
        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, exomeEnrichmentKit2)
        // Even though kit2 already exists in the "DB", it is still invalid because it conflicts with Kit1
        exomeEnrichmentKitService.validateExomeEnrichmentKit(testData.sample, exomeEnrichmentKit2)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneFirstSeqTrackInSample() {
        testData.seqTrack.seqType = testData.seqType
        assertNotNull(testData.seqTrack.save(flush: true))

        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(testData.runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneFirstExomeSeqTrackInSample() {
        testData.seqTrack.sample = null
        testData.dataFile.seqTrack = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack, exomeEnrichmentKit)
        testData.dataFile.seqTrack = exomeSeqTrack
        assertNotNull(testData.dataFile.seqTrack.save(flush: true))

        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(testData.runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldSeqTrackInSample() {
        testData.seqTrack.seqType = testData.seqType
        assertNotNull(testData.seqTrack.save(flush: true))

        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack, exomeEnrichmentKit)
        DataFile dataFile = testData.createDataFile(exomeSeqTrack, runSegment)
        assertNotNull(dataFile.save(flush: true))

        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldExomeSeqTrackWithSameKitInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack1, exomeEnrichmentKit)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, exomeEnrichmentKit)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
    }


    @Test(expected = IllegalArgumentException)
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldExomeSeqTrackWithDiffKitInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack1, exomeEnrichmentKit)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit exomeEnrichmentKit1 = testData.createEnrichmentKit(DIFFERENT_EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, exomeEnrichmentKit1)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackAndOldExomeSeqTrackWithNoKitInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, exomeEnrichmentKit)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        assertNull(exomeSeqTrack1.exomeEnrichmentKit)
        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
        assertEquals(exomeEnrichmentKit, exomeSeqTrack1.exomeEnrichmentKit)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneFirstExomeSeqTrackWithNoKitInSample() {
        testData.seqTrack.sample = null
        ExomeSeqTrack exomeSeqTrack = testData.createExomeSeqTrack(testData.run)
        testData.dataFile.seqTrack = exomeSeqTrack
        assertNotNull(testData.dataFile.seqTrack.save(flush: true))

        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(testData.runSegment)
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

        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneNewExomeSeqTrackWithNoKitAndOldExomeSeqTrackWithKitInSample() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        testData.seqTrack = exomeSeqTrack1
        assertNotNull(testData.seqTrack.save(flush: true))
        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack1, exomeEnrichmentKit)

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        assertNull(exomeSeqTrack2.exomeEnrichmentKit)
        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(runSegment)
        assertEquals(exomeEnrichmentKit, exomeSeqTrack2.exomeEnrichmentKit)
    }


    @Test
    void testInferInformationForOldLaneFromNewLaneRunSegmentContainsBamFiles() {
        testData.seqTrack.seqType = testData.seqType
        assertNotNull(testData.seqTrack.save(flush: true))

        FileType fileType = testData.createFileType(FileType.Type.ALIGNMENT)

        DataFile dataFile = testData.createDataFile(null, testData.runSegment, fileType)
        assertNotNull(dataFile.save(flush: true))

        exomeEnrichmentKitService.inferKitInformationForOldLaneFromNewLane(testData.runSegment)
    }


    ExomeEnrichmentKitSynonym createExomeEnrichmentKitSynonym() {
        ExomeEnrichmentKit exomeEnrichmentKit = new ExomeEnrichmentKit(
                        name: EXOME_ENRICHMENT_KIT
                        )
        assertNotNull(exomeEnrichmentKit.save([flush: true]))
        ExomeEnrichmentKitSynonym exomeEnrichmentKitSynonym = new ExomeEnrichmentKitSynonym(
                        name: EXOME_ENRICHMENT_KIT_NAME,
                        exomeEnrichmentKit: exomeEnrichmentKit)
        assertNotNull(exomeEnrichmentKitSynonym.save([flush: true]))
        return exomeEnrichmentKitSynonym
    }

}

