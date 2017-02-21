package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.CollectionUtils
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
@Mock([Realm, Project, ProjectCategory, ReferenceGenome, ReferenceGenomeEntry, StatSizeFileName])
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
        referenceGenomeService = new ReferenceGenomeService()
        referenceGenomeService.configService = new ConfigService()

        directory = temporaryFolder.newFolder('reference_genomes', 'referenceGenome')

        file = new File(directory, "prefixName.fa")
        assert file.createNewFile()
        file << "test"

        realm = DomainFactory.createRealmDataProcessingDKFZ([
            processingRootPath: temporaryFolder.getRoot().path,
            ]).save([flush: true])

        project = DomainFactory.createProject()
        project.name = "SOME_PROJECT"
        project.dirName = HelperUtils.uniqueString
        project.realmName = realm.name
        project.save(flush: true)

        referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.path = "referenceGenome"
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.cytosinePositionsIndex = "cytosine_idx.pos.gz"
        referenceGenome.fingerPrintingFileName = 'fingerPrinting.bed'
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
    void testFingerPrintingFile() {
        File pathExp = new File(directory, "fingerPrinting/fingerPrinting.bed")
        File pathAct = referenceGenomeService.fingerPrintingFile(project, referenceGenome, false) as File
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFingerPrintingFileReferenceGenomeIsNull() {
        referenceGenome = null
        referenceGenomeService.fingerPrintingFile(project, referenceGenome, false)
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
    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithExistCheck_ShouldFail() {
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

    @Test
    void test_cytosinePositionIndexFilePath_AllFine() {
        File pathExp = new File(directory, referenceGenome.cytosinePositionsIndex)
        CreateFileHelper.createFile(pathExp)

        assertEquals pathExp, referenceGenomeService.cytosinePositionIndexFilePath(project, referenceGenome)
    }

    @Test
    void test_cytosinePositionIndexFilePath_fileDoesntExist_shouldFail() {
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.cytosinePositionIndexFilePath(project, referenceGenome)
        }.contains(referenceGenome.cytosinePositionsIndex)
    }

    @Test
    void test_loadReferenceGenome() {
        String name = "my_reference_gnome"
        String path = "bwa06_my_reference_gnome"
        String fileNamePrefix = "my_reference_gnome"
        String cytosinePositionsIndex = null
        String statSizeFileName = "my_reference_gnome.fa.chrLenOnlyACGT.tab"

        String fastaName = "chr21"
        String fastaAlias = "21"
        long fastaLength = 249250621
        long fastaLengthWithoutN = 238204518
        Classification fastaClassification = Classification.CHROMOSOME

        List<FastaEntry> fastaEntries = [
                new FastaEntry(fastaName, fastaAlias, fastaLength, fastaLengthWithoutN, fastaClassification),
        ]

        referenceGenomeService.loadReferenceGenome(name, path, fileNamePrefix, cytosinePositionsIndex,
                fastaEntries, [statSizeFileName])

        ReferenceGenome referenceGenome = CollectionUtils.exactlyOneElement(ReferenceGenome.findAllByName(name))
        assert referenceGenome.path == path
        assert referenceGenome.fileNamePrefix == fileNamePrefix
        assert referenceGenome.cytosinePositionsIndex == cytosinePositionsIndex

        ReferenceGenomeEntry entry = CollectionUtils.exactlyOneElement(ReferenceGenomeEntry.findAllByName(fastaName))
        assert entry.referenceGenome == referenceGenome
        assert entry.alias == fastaAlias
        assert entry.length == fastaLength
        assert entry.lengthWithoutN == fastaLengthWithoutN
        assert entry.classification == fastaClassification

        StatSizeFileName statSizeFileName1 = CollectionUtils.exactlyOneElement(StatSizeFileName.findAllByName(statSizeFileName))
        assert statSizeFileName1.referenceGenome == referenceGenome
    }
}
