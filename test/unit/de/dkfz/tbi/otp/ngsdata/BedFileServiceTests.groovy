package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.Environment
import org.junit.*

@TestFor(BedFileService)
@TestMixin(GrailsUnitTestMixin)
@Mock([Realm, ReferenceGenome, ExomeEnrichmentKit, BedFile])
class BedFileServiceTests {

    Realm realm
    File directory
    File bedFileFs
    BedFile bedFileDom

    BedFileService bedFileService

    private final static String referenceGenomePath = "/tmp/reference_genomes/referenceGenome/"
    private final static String target_regions_dir = "targetRegions"

    @Before
    void setUp() {
        ReferenceGenomeService referenceGenomeService = new ReferenceGenomeService()
        referenceGenomeService.configService = new ConfigService()
        bedFileService = new BedFileService()
        bedFileService.referenceGenomeService = referenceGenomeService

        directory = new File("${referenceGenomePath}/${target_regions_dir}")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        bedFileFs = new File("${referenceGenomePath}/${target_regions_dir}/bedFileName")
        if (!bedFileFs.exists()) {
            bedFileFs.createNewFile()
            bedFileFs << "test"
        }

        realm = new Realm()
        realm.name = "def"
        realm.env = Environment.getCurrent().getName()
        realm.operationType = Realm.OperationType.DATA_PROCESSING
        realm.cluster = Realm.Cluster.DKFZ
        realm.rootPath = ""
        realm.processingRootPath = "tmp"
        realm.programsRootPath = ""
        realm.webHost = ""
        realm.host = ""
        realm.port = 8080
        realm.unixUser = ""
        realm.timeout = 1000000
        realm.pbsOptions = ""
        realm.save(flush : true)

        ReferenceGenome referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.path = "referenceGenome"
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.save(flush: true)

        ExomeEnrichmentKit kit = new ExomeEnrichmentKit(
                name: "kitName1"
                )
        kit.save(flush: true)

        bedFileDom = new BedFile(
                fileName: "bedFileName",
                targetSize: 111,
                referenceGenome: referenceGenome,
                exomeEnrichmentKit: kit
                )
        bedFileDom.save(flush: true)
    }

    @After
    void tearDown() {
        realm = null
        bedFileDom = null
        directory.deleteOnExit()
        bedFileFs.deleteOnExit()
        bedFileService = null
    }

    @Test(expected = IllegalArgumentException)
    void testNullProject() {
        bedFileService.filePath(null, bedFileDom)
    }

    @Test(expected = IllegalArgumentException)
    void testNullBedFile() {
        bedFileService.filePath(realm, null)
    }

    @Test(expected = RuntimeException)
    void testFileCanNotRead() {
        assertTrue bedFileFs.delete()
        bedFileService.filePath(realm, bedFileDom)
    }

    @Test
    void testCorrectCase() {
        String path = bedFileService.filePath(realm, bedFileDom)
        String expectedPath = "${referenceGenomePath}/${target_regions_dir}/bedFileName"
        assertEquals(path, expectedPath)
    }
}
