package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.utils.ReferencedClass

@TestMixin(GrailsUnitTestMixin)
@TestFor(MetaDataValidationService)
@Mock([MetaDataValidationService, ExomeEnrichmentKitService,
    ReferencedClass, ChangeLog,
    Project, SeqPlatform, SeqCenter, Run,DataFile, MetaDataKey, MetaDataEntry,
    ExomeEnrichmentKit, ExomeEnrichmentKitIdentifier])
class MetaDataValidationServiceTests {

    MetaDataValidationService metaDataValidationService

    final static String LANE_NO = "LANE_NO"

    final static String BARCODE = "BARCODE"

    /**
     * Alternative key for {@link #BARCODE}
     */
    final static String INDEX_NO = "INDEX_NO"

    final static String LIB_PREP_KIT = "LIB_PREP_KIT"

    final static String SEQUENCING_TYPE = "SEQUENCING_TYPE"

    final static String EXOME_ENRICHMENT_KIT ="ExomeEnrichmentKit"

    final static String EXOME_ENRICHMENT_KIT_IDENTIFIER ="ExomeEnrichmentKitIdentifier"

    @Before
    public void setUp() throws Exception {
        metaDataValidationService = new MetaDataValidationService([
            exomeEnrichmentKitService: new ExomeEnrichmentKitService()
        ])
    }

    @After
    public void tearDown() throws Exception {
        metaDataValidationService = null
    }

    void testCombineLaneAndIndexUsingIndex() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "AAAAAA"], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1_AAAAAA", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingBarcode() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (BARCODE): "AAAAAA"], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1_AAAAAA", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingIndexNoValue() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): ""], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingBarcodeNoValue() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (BARCODE): ""], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingIndexOnlySpaces() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "  "], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingBarcodeOnlySpaces() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (BARCODE): "  "], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexNeitherIndexNorBarcode() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1"], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingIndexTwoTimes() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "AAAAAA"], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1_AAAAAA", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingBarcodeTwoTimes() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (BARCODE): "AAAAAA"], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1_AAAAAA", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingBarcodeNoLaneNo() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(BARCODE): "AAAAAA"], dataFile)
        shouldFail(NullPointerException) { metaDataValidationService.combineLaneAndIndex(dataFile) }
    }

    void testCombineLaneAndIndexUsingIndexAndBarCodeBothCorrect() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "AAAAAA", (BARCODE): "BBBBBB"], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1_AAAAAA", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingIndexAndBarCodeWhereIndexNoIsEmpty() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "", (BARCODE): "BBBBBB"], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingIndexAndBarCodeWhereBarcodeIsEmpty() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "AAAAAA", (BARCODE): ""], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1_AAAAAA", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingIndexAndBarCodeWhereIndexNoHasWhiteSpaces() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "  ", (BARCODE): "BBBBBB"], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingIndexAndBarCodeWhereBarcodeHasWhiteSpaces() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "AAAAAA", (BARCODE): "  "], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1_AAAAAA", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCombineLaneAndIndexUsingIndexAndBarCodeWhereBothAreEmpty() {
        DataFile dataFile = createDataFile()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LANE_NO): "1", (INDEX_NO): "", (BARCODE): ""], dataFile)
        metaDataValidationService.combineLaneAndIndex(dataFile)
        assertEquals("1", getMetaDataEntryValue(dataFile, LANE_NO))
    }

    void testCheckExomeEnrichmentKitForExomeSeqType_UsingExomeEnrichmentKit() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): EXOME_ENRICHMENT_KIT, (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.TRUE, metaDataValidationService.checkExomeEnrichmentKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckExomeEnrichmentKitForExomeSeqType_UsingExomeEnrichmentKitIdentifier() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): EXOME_ENRICHMENT_KIT_IDENTIFIER, (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.TRUE, metaDataValidationService.checkExomeEnrichmentKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckExomeEnrichmentKitForExomeSeqType_UsingUNKNOWN() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): "UNKNOWN", (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.TRUE, metaDataValidationService.checkExomeEnrichmentKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckExomeEnrichmentKitForExomeSeqType_NotValidEntry() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): "something", (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.FALSE, metaDataValidationService.checkExomeEnrichmentKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckExomeEnrichmentKitForExomeSeqType_NotExome() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): "something", (SEQUENCING_TYPE): "NOT_EXOME"])
        assertEquals(null, metaDataValidationService.checkExomeEnrichmentKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckExomeEnrichmentKitForExomeSeqType_NoSequenceType() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): "something"])
        shouldFail(NullPointerException.class) {
            metaDataValidationService.checkExomeEnrichmentKitForExomeSeqType(map[(LIB_PREP_KIT)])
        }
    }

    void testValidateMetaDataEntryForLIB_PREP_KIT() {
        Run run = createRun()
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): EXOME_ENRICHMENT_KIT, (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertTrue(metaDataValidationService.validateMetaDataEntry(run, map[(LIB_PREP_KIT)]))
    }

    private Run createRun() {
        Project project = new Project(
                        name: "projectname",
                        dirName: "projectdirName",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true]))
        SeqPlatform seqPlatform = new SeqPlatform(
                        name: "seqPlatformName",
                        model: "seqPlatformModel"
                        )
        assertNotNull(seqPlatform.save([flush: true]))
        SeqCenter seqCenter = new SeqCenter(
                        name: "seqcenter",
                        dirName: "seqcenter"
                        )
        assertNotNull(seqCenter.save([flush: true]))
        Run run = new Run(
                        name: "run",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: Run.StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true]))
        return run
    }

    private ExomeEnrichmentKitIdentifier createExomeEnrichmentKitIdentifier() {
        ExomeEnrichmentKit exomeEnrichmentKit = new ExomeEnrichmentKit(
                        name: EXOME_ENRICHMENT_KIT
                        )
        assertNotNull(exomeEnrichmentKit.save([flush: true]))
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = new ExomeEnrichmentKitIdentifier(
                        name: EXOME_ENRICHMENT_KIT_IDENTIFIER,
                        exomeEnrichmentKit: exomeEnrichmentKit)
        assertNotNull(exomeEnrichmentKitIdentifier.save([flush: true]))
        return exomeEnrichmentKitIdentifier
    }

    private DataFile createDataFile() {
        DataFile dataFile = new DataFile(fileName: "2_ATCACC_L005_R2_complete_filtered.fastq.gz")
        assertNotNull(dataFile.save([flush: true]))
        return dataFile
    }

    private Map<String, MetaDataEntry> createMetaDataEntry(Map<String, String> map, DataFile dataFile = null) {
        if (!dataFile) {
            dataFile = createDataFile()
        }

        Map<String, MetaDataEntry> returnMap = [:]
        map.each { String key, String value ->
            MetaDataKey metaDataKey = new MetaDataKey(name: key)
            assertNotNull(metaDataKey.save([flush: true]))

            MetaDataEntry metaDataEntry = new MetaDataEntry(value: value, dataFile: dataFile, key: metaDataKey, source: MetaDataEntry.Source.SYSTEM)
            assertNotNull(metaDataEntry.save([flush: true]))
            returnMap.put(key, metaDataEntry)
        }
        return returnMap
    }

    /**
     * helper to return value of the the {@link MetaDataEntry} for the given {@link DataFile} and key
     */
    private String getMetaDataEntryValue(DataFile dataFile, String key) {
        MetaDataKey metaDataKey = MetaDataKey.findByName(key)
        assertNotNull(metaDataKey)
        MetaDataEntry metaDataEntry = MetaDataEntry.findByDataFileAndKey(dataFile, metaDataKey)
        assertNotNull(metaDataEntry)
        return metaDataEntry.value
    }
}
