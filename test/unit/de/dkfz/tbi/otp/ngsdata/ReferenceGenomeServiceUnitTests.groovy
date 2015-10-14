package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.CreateFileHelper
import org.apache.commons.io.FileUtils
import org.junit.rules.TemporaryFolder
import static org.junit.Assert.*

import de.dkfz.tbi.otp.utils.HelperUtils
import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification


@TestFor(ReferenceGenomeService)
@TestMixin(GrailsUnitTestMixin)
@Mock([Realm, Project, ReferenceGenome, ReferenceGenomeEntry])
class ReferenceGenomeServiceUnitTests {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntry
    Project project
    ReferenceGenomeService referenceGenomeService
    Realm realm

    File directory
    File file

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        temporaryFolder.create() //Called directly because bug in junit

        referenceGenomeService = new ReferenceGenomeService()
        referenceGenomeService.configService = new ConfigService()

        directory = temporaryFolder.newFolder('reference_genomes', 'referenceGenome')

        file = new File(directory, "prefixName.fa")
        assert file.createNewFile()
        file << "test"

        realm = DomainFactory.createRealmDataProcessingDKFZ([
            processingRootPath: temporaryFolder.getRoot().path,
            ]).save([flush: true])

        project = TestData.createProject()
        project.name = "SOME_PROJECT"
        project.dirName = HelperUtils.uniqueString
        project.realmName = realm.name
        project.save(flush: true)

        referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.path = "referenceGenome"
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.length = ARBITRARY_REFERENCE_GENOME_LENGTH
        referenceGenome.lengthWithoutN = ARBITRARY_REFERENCE_GENOME_LENGTH
        referenceGenome.lengthRefChromosomes = ARBITRARY_REFERENCE_GENOME_LENGTH
        referenceGenome.lengthRefChromosomesWithoutN = ARBITRARY_REFERENCE_GENOME_LENGTH
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
    }

    @Test
    void testRealmFilePathToDirectoryCorrect() {
        File pathExp = directory
        File pathAct = referenceGenomeService.filePathToDirectory(realm, referenceGenome) as File
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmFilePathToDirectoryNullRealm() {
        realm = null
        referenceGenomeService.filePathToDirectory(realm as Realm, referenceGenome)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmFilePathToDirectoryNullRefGen() {
        referenceGenome = null
        referenceGenomeService.filePathToDirectory(realm, referenceGenome)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmFilePathToDirectoryWrongRealmOperationType() {
        realm.operationType = Realm.OperationType.DATA_MANAGEMENT
        referenceGenomeService.filePathToDirectory(realm, referenceGenome)
    }

    @Test(expected = RuntimeException)
    void testRealmFilePathToDirectoryCanNotReadDirectory() {
        FileUtils.deleteDirectory(directory)
        assertFalse directory.exists()
        referenceGenomeService.filePathToDirectory(realm, referenceGenome)
    }

    @Test
    void testRealmFilePathToDirectoryCanNotReadDirectory_NoFileCheck() {
        assert directory.deleteDir()
        File pathExp = directory
        File pathAct = referenceGenomeService.filePathToDirectory(realm, referenceGenome, false) as File
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testFilePathToDirectory() {
        File pathExp = directory
        File pathAct = referenceGenomeService.filePathToDirectory(project, referenceGenome) as File
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
        referenceGenomeService.filePathToDirectory(project as Project, referenceGenome)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathToDirectoryRefGenIsNull() {
        referenceGenome = null
        referenceGenomeService.filePathToDirectory(project, referenceGenome)
    }

    @Test(expected = RuntimeException)
    void testFilePathToDirectoryCanNotReadDirectory() {
        assert directory.deleteDir()
        assertFalse directory.exists()
        referenceGenomeService.filePathToDirectory(project, referenceGenome)
    }

    @Test
    void testFilePathToDirectory_FileNotExistButNoFileCheck() {
        assert directory.deleteDir()
        File pathExp = directory
        File pathAct = referenceGenomeService.filePathToDirectory(project, referenceGenome, false) as File
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testFilePathOnlyPrefix() {
        File pathExp = new File(directory, "prefixName")
        File pathAct = referenceGenomeService.prefixOnlyFilePath(project, referenceGenome) as File
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathOnlyPrefixReferenceGenomeIsNull() {
        referenceGenome = null
        String pathAct = referenceGenomeService.prefixOnlyFilePath(project, referenceGenome)
    }

    @Test
    void testFilePath() {
        File pathExp = new File(directory, "prefixName.fa")
        File pathAct = referenceGenomeService.fastaFilePath(project, referenceGenome) as File
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

    @Test
    void testPathToChromosomeSizeFilesPerReference_ProjectIsNull_ShouldFail() {
        assert TestCase.shouldFail(IllegalArgumentException) {
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(null, referenceGenome)
        }.contains('The project is not specified')
    }

    @Test
    void testPathToChromosomeSizeFilesPerReference_ReferenceGenomeIsNull_ShouldFail() {
        assert TestCase.shouldFail(IllegalArgumentException) {
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(project, null)
        }.contains('The reference genome is not specified')
    }

    @Test
    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithExistCheck_ShoulFail() {
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(project, referenceGenome, true)
        }.contains(ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
    }

    @Test
    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithoutExistCheck_AllFine() {
        File pathExp = new File(directory, ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        File pathAct = referenceGenomeService.pathToChromosomeSizeFilesPerReference(project, referenceGenome, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testPathToChromosomeSizeFilesPerReference_DirectoryExist_WithExistCheck_AllFine() {
        File pathExp = new File(directory, ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        CreateFileHelper.createFile(pathExp)
        File pathAct = referenceGenomeService.pathToChromosomeSizeFilesPerReference(project, referenceGenome, true)
        assertEquals(pathExp, pathAct)
    }
}
