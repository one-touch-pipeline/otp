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

    @Test
    void testFindOutReadNumber_RegExprMatches() {
        def files = [
            [name: 'SOMEPID_L001_R2.fastq.gz', readNumber: 2],
            [name: 's_101202_7_1.fastq.gz', readNumber: 1],
            [name: 's_110421_3.read2.fastq.gz', readNumber: 2],
            [name: 'AB-1234_CDE_EFGH_091_lib14837_1189_7_1.fastq.tar.bz', readNumber: 1],
            [name: 'AB-1234_5647_lib12345_1_sequence.fastq.bz2', readNumber: 1],
            [name: 'CD-2345_6789_lib234567_7890_1.fastq.bz2', readNumber: 1],
            [name: 'NB_E_789_R.2.fastq.gz', readNumber: 2],
            [name: 'NB_E_234_R5.2.fastq.gz', readNumber: 2],
            [name: 'NB_E_345_T1S.2.fastq.gz', readNumber: 2],
            [name: 'NB_E_456_O_lane5.2.fastq.gz', readNumber: 2],
            [name: '00_MCF10A_GHI_JKL_WGBS_I.A34002.137487.C2RT2ACXX.1.1.fastq.gz', readNumber: 1],
            [name: '00_MCF10A_GHI_JKL_H3K4me1_I.IX1239-A26685-ACTTGA.134224.D2B0LACXX.2.1.fastq.gz', readNumber: 1],
            [name: 'RB7_Blut_R1.fastq.gz', readNumber: 1],
            [name: 'P021_WXYZ_L1_Rep3_2.fastq.gz', readNumber: 2],
            [name: 'H019_ASDF_L1_lib54321_1.fastq.gz', readNumber: 1],
            [name: 'FE-0100_H021_WXYZ_L1_5_1.fastq.gz', readNumber: 1],
        ]
        files.each { file ->
            assertEquals(file.readNumber, MetaDataService.findOutReadNumber(file.name))
        }
    }

    @Test
    void testFindOutReadNumber_RegExprNotMatches() {
        def files = [
            'D0DDVABXX_lane4.fastq.gz',
        ]
        files.each { file ->
            assert shouldFail(RuntimeException, { MetaDataService.findOutReadNumber(file) }) ==~ /cannot\sfind.*/
        }
    }

    @Test
    void testFindOutReadNumber_InvalidInput() {
        assert shouldFail(AssertionError, { MetaDataService.findOutReadNumber(null) }) ==~ /.*file\sname\smust\sbe\sprovided.*/
    }

    @Test
    void testFindOutReadNumberIfSingleEndOrByFileName_SingleEnd() {
        def file = 'D0DDVABXX_lane4.fastq.gz'
        assertEquals(1, MetaDataService.findOutReadNumberIfSingleEndOrByFileName(file, true))
        assertEquals(1, MetaDataService.findOutReadNumberIfSingleEndOrByFileName(null, true))
    }

    @Test
    void testFindOutReadNumberIfSingleEndOrByFileName_FromFileName() {
        def file = 'SOMEPID_L001_R2.fastq.gz'
        assertEquals(2, MetaDataService.findOutReadNumberIfSingleEndOrByFileName(file, false))
    }
}
