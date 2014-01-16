package de.dkfz.tbi.otp.ngsdata

import org.apache.commons.io.FileUtils
import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(ReferenceGenomeService)
@TestMixin(GrailsUnitTestMixin)
@Mock([Realm, Project, ReferenceGenome, ReferenceGenomeEntry])
class ReferenceGenomeServiceTests {

    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntry
    Project project
    ReferenceGenomeService referenceGenomeService
    Realm realm

    File directory
    File file
    String referenceGenomePath = "/tmp/reference_genomes/referenceGenome/"

    @Before
    void setUp() {
        referenceGenomeService = new ReferenceGenomeService()
        referenceGenomeService.configService = new ConfigService()

        directory = new File(referenceGenomePath)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        file = new File("${referenceGenomePath}prefixName.fa")
        if (!file.exists()) {
            file.createNewFile()
            file << "test"
        }

        realm = DomainFactory.createRealmDataProcessingDKFZ([
            processingRootPath: 'tmp',
            ]).save([flush: true])

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

        referenceGenomeEntry = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "1",
                        classification: Classification.CHROMOSOME,
                        referenceGenome: referenceGenome
                        )
        referenceGenomeEntry.save(flush: true)

        ReferenceGenomeEntry referenceGenomeEntryTwo = new ReferenceGenomeEntry(
                        name: "chr2",
                        alias: "2",
                        classification: Classification.UNDEFINED,
                        referenceGenome: referenceGenome
                        )
        referenceGenomeEntryTwo.save(flush: true)
    }

    @After
    void tearDown() {
        referenceGenomeEntry = null
        referenceGenome = null
        project = null
        referenceGenomeService = null
        realm = null
        directory.deleteOnExit()
        file.deleteOnExit()
    }

    @Test
    void testRealmFilePathToDirectoryCorrect() {
        String pathExp = "${referenceGenomePath}"
        String pathAct = referenceGenomeService.filePathToDirectory(realm, referenceGenome)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmFilePathToDirectoryNullRealm() {
        realm = null
        String pathAct = referenceGenomeService.filePathToDirectory(realm as Realm, referenceGenome)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmFilePathToDirectoryNullRefGen() {
        referenceGenome = null
        String pathAct = referenceGenomeService.filePathToDirectory(realm, referenceGenome)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmFilePathToDirectoryWrongRealmOperationType() {
        realm.operationType = Realm.OperationType.DATA_MANAGEMENT
        String pathAct = referenceGenomeService.filePathToDirectory(realm, referenceGenome)
    }

    @Test(expected = RuntimeException)
    void testRealmFilePathToDirectoryCanNotReadDirectory() {
        FileUtils.deleteDirectory(directory)
        assertFalse directory.exists()
        String pathAct = referenceGenomeService.filePathToDirectory(realm, referenceGenome)
    }

    @Test
    void testFilePathToDirectory() {
        String pathExp = "${referenceGenomePath}"
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
        String pathAct = referenceGenomeService.filePathToDirectory(project as Project, referenceGenome)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathToDirectoryRefGenIsNull() {
        referenceGenome = null
        String pathAct = referenceGenomeService.filePathToDirectory(project, referenceGenome)
    }

    @Test(expected = RuntimeException)
    void testFilePathToDirectoryCanNotReadDirectory() {
        FileUtils.deleteDirectory(directory)
        assertFalse directory.exists()
        String pathAct = referenceGenomeService.filePathToDirectory(project, referenceGenome)
    }

    @Test
    void testFilePathOnlyPrefix() {
        String pathExp = "${referenceGenomePath}prefixName"
        String pathAct = referenceGenomeService.prefixOnlyFilePath(project, referenceGenome)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathOnlyPrefixReferenceGenomeIsNull() {
        referenceGenome = null
        String pathAct = referenceGenomeService.prefixOnlyFilePath(project, referenceGenome)
    }

    @Test
    void testFilePath() {
        String pathExp = "${referenceGenomePath}prefixName.fa"
        String pathAct = referenceGenomeService.fastaFilePath(project, referenceGenome)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathProjectIsNull() {
        project = null
        String pathAct = referenceGenomeService.fastaFilePath(project, referenceGenome)
    }

    @Test
    void testChromosomesInReferenceGenome() {
        List<ReferenceGenomeEntry> referenceGenomeEntriesExp = [referenceGenomeEntry]
        List<ReferenceGenomeEntry> referenceGenomeEntriesAct = referenceGenomeService.chromosomesInReferenceGenome(referenceGenome)
        assertEquals(referenceGenomeEntriesExp, referenceGenomeEntriesAct)
    }
}
