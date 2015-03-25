package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.utils.ReferencedClass

@TestMixin(GrailsUnitTestMixin)
@TestFor(MetaDataValidationService)
@Mock([MetaDataValidationService, LibraryPreparationKitService, SequencingKitService,
    ReferencedClass, ChangeLog,
    Project, SeqPlatform, SeqCenter, Run,DataFile, MetaDataKey, MetaDataEntry,
    LibraryPreparationKit, LibraryPreparationKitSynonym,
    SequencingKit, SequencingKitSynonym])
class MetaDataValidationServiceUnitTests {

    MetaDataValidationService metaDataValidationService

    final static String LANE_NO = "LANE_NO"

    final static String BARCODE = "BARCODE"

    /**
     * Alternative key for {@link #BARCODE}
     */
    final static String INDEX_NO = "INDEX_NO"

    final static String LIB_PREP_KIT = "LIB_PREP_KIT"

    final static String SEQUENCING_TYPE = "SEQUENCING_TYPE"

    final static String LIBRARY_PREPARATION_KIT ="LibraryPreparationKit"

    final static String LIBRARY_PREPARATION_KIT_SYNONYM ="LibraryPreparationKitSynonym"

    final static String SEQUENCING_KIT = "SequencingKit"

    final static String OTHER_SEQUENCING_KIT = "OtherSequencingKit"

    final static String SEQUENCING_KIT_SYNONYM = "SequencingKitSynonym"

    // the String "UNKNOWN" is used instead of the enum, because that is how it appears in external input files
    final String UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE = "UNKNOWN"

    @Before
    public void setUp() throws Exception {
        metaDataValidationService = new MetaDataValidationService([
            libraryPreparationKitService: new LibraryPreparationKitService()
        ])
        metaDataValidationService.sequencingKitService = new SequencingKitService()
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

    void testCheckLibraryPreparationKitForExomeSeqType_UsingLibraryPreparationKit() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): LIBRARY_PREPARATION_KIT, (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.TRUE, metaDataValidationService.checkLibraryPreparationKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckLibraryPreparationKitForExomeSeqType_UsingLibraryPreparationKitSynonym() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): LIBRARY_PREPARATION_KIT_SYNONYM, (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.TRUE, metaDataValidationService.checkLibraryPreparationKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckLibraryPreparationKitForExomeSeqType_UsingUNKNOWN() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): UNKNOWN_VERIFIED_VALUE_FROM_METADATA_FILE, (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.TRUE, metaDataValidationService.checkLibraryPreparationKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckLibraryPreparationKitForExomeSeqType_NotValidEntry() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): "something", (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.FALSE, metaDataValidationService.checkLibraryPreparationKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckLibraryPreparationKitForExomeSeqType_NotExome() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): "something", (SEQUENCING_TYPE): "NOT_EXOME"])
        assertEquals(null, metaDataValidationService.checkLibraryPreparationKitForExomeSeqType(map[(LIB_PREP_KIT)]))
    }

    void testCheckLibraryPreparationKitForExomeSeqType_NoSequenceType() {
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): "something"])
        shouldFail(NullPointerException.class) {
            metaDataValidationService.checkLibraryPreparationKitForExomeSeqType(map[(LIB_PREP_KIT)])
        }
    }

    void testValidateMetaDataEntryForLIB_PREP_KIT() {
        Run run = createRun()
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = createLibraryPreparationKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(LIB_PREP_KIT): LIBRARY_PREPARATION_KIT, (SEQUENCING_TYPE): SeqTypeNames.EXOME.seqTypeName])
        assertTrue(metaDataValidationService.validateMetaDataEntry(run, map[(LIB_PREP_KIT)]))
    }


    void testValidateMetaDataEntryForSEQUENCING_KIT() {
        Run run = createRun()
        createSequencingKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(MetaDataColumn.SEQUENCING_KIT.name()): SEQUENCING_KIT])
        assertTrue(metaDataValidationService.validateMetaDataEntry(run, map[(MetaDataColumn.SEQUENCING_KIT.name())]))
    }

    void testValidateMetaDataEntryForSEQUENCING_KIT_MetadataUseSynonymValue() {
        Run run = createRun()
        createSequencingKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(MetaDataColumn.SEQUENCING_KIT.name()): SEQUENCING_KIT_SYNONYM])
        assertTrue(metaDataValidationService.validateMetaDataEntry(run, map[(MetaDataColumn.SEQUENCING_KIT.name())]))
    }

    void testValidateMetaDataEntryForSEQUENCING_KIT_MetadataValueDoesNotExistInDB_IsFalse() {
        Run run = createRun()
        createSequencingKitSynonym()
        Map<String, MetaDataEntry> map = createMetaDataEntry([(MetaDataColumn.SEQUENCING_KIT.name()): OTHER_SEQUENCING_KIT])
        assertFalse(metaDataValidationService.validateMetaDataEntry(run, map[(MetaDataColumn.SEQUENCING_KIT.name())]))
    }



    void testCheckSampleIdentifier_correct() {
        assert metaDataValidationService.checkSampleIdentifier('CorrectName')
    }

    void testCheckSampleIdentifier_correct_minLength() {
        assert metaDataValidationService.checkSampleIdentifier('123')
    }

    void testCheckSampleIdentifier_wrong_empty() {
        assert !metaDataValidationService.checkSampleIdentifier('')
    }

    void testCheckSampleIdentifier_wrong_tooShort() {
        assert !metaDataValidationService.checkSampleIdentifier('12')
    }



    private Run createRun() {
        Project project = TestData.createProject(
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

    private LibraryPreparationKitSynonym createLibraryPreparationKitSynonym() {
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                        name: LIBRARY_PREPARATION_KIT
                        )
        assertNotNull(libraryPreparationKit.save([flush: true]))
        LibraryPreparationKitSynonym libraryPreparationKitSynonym = new LibraryPreparationKitSynonym(
                        name: LIBRARY_PREPARATION_KIT_SYNONYM,
                        libraryPreparationKit: libraryPreparationKit)
        assertNotNull(libraryPreparationKitSynonym.save([flush: true]))
        return libraryPreparationKitSynonym
    }

    private SequencingKitSynonym createSequencingKitSynonym() {
        SequencingKit sequencingKit = new SequencingKit(
                name: SEQUENCING_KIT
        )
        assert sequencingKit.save(flush: true)

        SequencingKitSynonym sequencingKitSynonym = new SequencingKitSynonym(
                name:SEQUENCING_KIT_SYNONYM,
                sequencingKit: sequencingKit
        )
        assert sequencingKitSynonym.save(flush: true)
        return sequencingKitSynonym
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
