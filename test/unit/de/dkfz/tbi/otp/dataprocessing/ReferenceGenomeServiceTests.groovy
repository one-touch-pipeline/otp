package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.util.Environment

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(ReferenceGenomeService)
@TestMixin(GrailsUnitTestMixin)
@Mock([Realm, Project, ReferenceGenome])
class ReferenceGenomeServiceTests {

    ReferenceGenome referenceGenome
    Project project
    ReferenceGenomeService referenceGenomeService

    File directory
    File file

    @Before
    void setUp() {
        referenceGenomeService = new ReferenceGenomeService()
        referenceGenomeService.configService = new ConfigService()

        directory = new File("/tmp/reference_genomes/referenceGenome/")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        file = new File("/tmp/reference_genomes/referenceGenome/prefixName.fa")
        if (!file.exists()) {
            file.createNewFile()
            file << "test"
        }

        Realm realm = new Realm()
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

        project = new Project()
        project.name = "SOME_PROJECT"
        project.dirName = "/tmp/alignmentPassService/"
        project.realmName = realm.name
        project.save(flush: true)

        referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.path = "referenceGenome"
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.save(flush: true)
    }

    @After
    void tearDown() {
        referenceGenome = null
        project = null
        referenceGenomeService = null
        directory.deleteOnExit()
        file.deleteOnExit()
    }

    @Test
    void testFilePathToDirectory() {
        String pathExp = "/tmp/reference_genomes/referenceGenome/"
        String pathAct = referenceGenomeService.filePathToDirectory(project, referenceGenome)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testFilePathToDirectoryWrongPath() {
        String wrongPath = "test"
        String pathAct = referenceGenomeService.filePathToDirectory(project, referenceGenome)
        assertNotSame(wrongPath, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathToDirectoryProjectIsNull() {
        project = null
        String pathAct = referenceGenomeService.filePathToDirectory(project, referenceGenome)
    }

    @Test
    void testFilePathOnlySuffix() {
        String pathExp = "/tmp/reference_genomes/referenceGenome/prefixName"
        String pathAct = referenceGenomeService.filePathOnlySuffix(project, referenceGenome)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathOnlySuffixReferenceGenomeIsNull() {
        referenceGenome = null
        String pathAct = referenceGenomeService.filePathOnlySuffix(project, referenceGenome)
    }

    @Test
    void testFilePath() {
        String pathExp = "/tmp/reference_genomes/referenceGenome/prefixName.fa"
        String pathAct = referenceGenomeService.fastaFilePath(project, referenceGenome)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathProjectIsNull() {
        project = null
        String pathAct = referenceGenomeService.fastaFilePath(project, referenceGenome)
    }
}