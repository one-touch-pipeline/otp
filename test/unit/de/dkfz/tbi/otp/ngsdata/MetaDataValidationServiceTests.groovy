package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(MetaDataValidationService)
@Mock([MetaDataValidationService, ExomeEnrichmentKitService,
    Project, SeqPlatform, SeqCenter, Run,DataFile, MetaDataKey, MetaDataEntry,
    ExomeEnrichmentKit, ExomeEnrichmentKitIdentifier])
class MetaDataValidationServiceTests {

    MetaDataValidationService metaDataValidationService

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

    void testCheckExomeEnrichmentKit_UsingExomeEnrichmentKit() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([LIB_PREP_KIT: EXOME_ENRICHMENT_KIT, SEQUENCING_TYPE: SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.TRUE, metaDataValidationService.checkExomeEnrichmentKit(map[LIB_PREP_KIT]))
    }

    void testCheckExomeEnrichmentKit_UsingExomeEnrichmentKitIdentifier() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([LIB_PREP_KIT: EXOME_ENRICHMENT_KIT_IDENTIFIER, SEQUENCING_TYPE: SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.TRUE, metaDataValidationService.checkExomeEnrichmentKit(map[LIB_PREP_KIT]))
    }

    void testCheckExomeEnrichmentKit_NotValidEntry() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([LIB_PREP_KIT: "something", SEQUENCING_TYPE: SeqTypeNames.EXOME.seqTypeName])
        assertEquals(Boolean.FALSE, metaDataValidationService.checkExomeEnrichmentKit(map[LIB_PREP_KIT]))
    }

    void testCheckExomeEnrichmentKit_NotExome() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([LIB_PREP_KIT: "something", SEQUENCING_TYPE: "NOT_EXOME"])
        assertEquals(null, metaDataValidationService.checkExomeEnrichmentKit(map[LIB_PREP_KIT]))
    }

    void testCheckExomeEnrichmentKit_NoSequenceType() {
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([LIB_PREP_KIT: "something"])
        shouldFail(NullPointerException.class) {
            metaDataValidationService.checkExomeEnrichmentKit(map[LIB_PREP_KIT])
        }
    }

    void testValidateMetaDataEntryForLIB_PREP_KIT() {
        Run run = createRun()
        ExomeEnrichmentKitIdentifier exomeEnrichmentKitIdentifier = createExomeEnrichmentKitIdentifier()
        Map<String, MetaDataEntry> map = createMetaDataEntry([LIB_PREP_KIT: EXOME_ENRICHMENT_KIT, SEQUENCING_TYPE: SeqTypeNames.EXOME.seqTypeName])
        assertTrue(metaDataValidationService.validateMetaDataEntry(run, map[LIB_PREP_KIT]))
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

    private Map<String, MetaDataEntry> createMetaDataEntry(Map<String, String> map) {
        DataFile dataFile = new DataFile(fileName: "2_ATCACC_L005_R2_complete_filtered.fastq.gz")
        assertNotNull(dataFile.save([flush: true]))

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
}

