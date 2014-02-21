package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.job.processing.ProcessingException

@TestMixin(GrailsUnitTestMixin)
@TestFor(MetaDataService)
@Mock([MetaDataKey, Realm, Project, Individual, SampleType, Sample, SeqType, SeqCenter,
    SeqPlatform, Run, RunSegment, SoftwareTool, SeqTrack, FileType, DataFile,
    ReferenceGenome, ReferenceGenomeProjectSeqType, ExomeSeqTrack, ExomeEnrichmentKit,
    ExomeEnrichmentKitService])

class MetaDataServiceUnitTests {

    MetaDataService metaDataService
    TestData testData

    final static String EXOME_ENRICHMENT_KIT_NAME = "ExomeEnrichmentKitName"

    @Before
    public void setUp() throws Exception {
        metaDataService = new MetaDataService()
        metaDataService.exomeEnrichmentKitService = new ExomeEnrichmentKitService()
        testData = new TestData()
        testData.createObjects()
    }

    @After
    public void tearDown() throws Exception {
        metaDataService = null
        testData = null
    }


    void testAssertAllNecessaryKeysExist() {
        metaDataService.assertAllNecessaryKeysExist(createAllMetadataKeys())
    }


    void testAssertAllNecessaryKeysExistWithMissingKey() {
        [
            0..MetaDataColumn.values().length
        ].each {
            List<MetaDataKey> keys = createAllMetadataKeys()
            keys.remove(it)
            metaDataService.assertAllNecessaryKeysExist(keys)
        }
    }


    void testAssertAllNecessaryKeysExistWithOptionalKey() {
        List<MetaDataKey> keys = createAllMetadataKeys()
        keys.add(new MetaDataKey([name: "optional"]))
        metaDataService.assertAllNecessaryKeysExist(keys)
    }


    void testGetKeysFromTokens() {
        assertEquals(MetaDataColumn.values().length, metaDataService.getKeysFromTokens(MetaDataColumn.values()*.name()).size())
    }


    @Ignore
    void testGetKeysFromTokensWithMissingKey() {
        shouldFail(ProcessingException.class) {
            metaDataService.getKeysFromTokens(MetaDataColumn.values()*.name().subList(0, 10))
        }
    }


    void testGetKeysFromTokensWithOptionalKey() {
        List<String> keys = MetaDataColumn.values()*.name()
        keys.add("optional")
        assertEquals(MetaDataColumn.values().length + 1, metaDataService.getKeysFromTokens(keys).size())
    }


    private List<MetaDataKey> createAllMetadataKeys() {
        List<MetaDataKey> keys = []
        MetaDataColumn.values().each {
            keys << new MetaDataKey(
                            name: it.name()
                            )
        }
        return keys
    }


    @Test
    void testEnrichOldDataWithNewInformationFrom() {
        ExomeSeqTrack exomeSeqTrack1 = testData.createExomeSeqTrack(testData.run)
        testData.dataFile.seqTrack = exomeSeqTrack1
        assertNotNull(testData.dataFile.save(flush: true))

        ExomeSeqTrack exomeSeqTrack2 = testData.createExomeSeqTrack(testData.run)
        ExomeEnrichmentKit exomeEnrichmentKit = testData.createEnrichmentKit(EXOME_ENRICHMENT_KIT_NAME)
        testData.addKitToExomeSeqTrack(exomeSeqTrack2, exomeEnrichmentKit)
        Run run = testData.createRun("testname2")
        RunSegment runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))
        DataFile dataFile = testData.createDataFile(exomeSeqTrack2, runSegment)
        assertNotNull(dataFile.save(flush: true))

        assertNull(exomeSeqTrack1.exomeEnrichmentKit)
        metaDataService.enrichOldDataWithNewInformationFrom(testData.run)
        assertEquals(exomeEnrichmentKit, exomeSeqTrack1.exomeEnrichmentKit)
    }
}
