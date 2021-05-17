/*
 * Copyright 2011-2020 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.apache.commons.io.FileUtils
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.ngsdata.referencegenome.FastaEntry
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import static org.junit.Assert.*

@TestFor(ReferenceGenomeService)
@TestMixin(GrailsUnitTestMixin)
@Mock([
        ProcessingOption,
        ProcessingPriority,
        Project,
        Realm,
        ReferenceGenome,
        ReferenceGenomeEntry,
        StatSizeFileName,
])
class ReferenceGenomeServiceUnitTests {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntry
    Project project
    ReferenceGenomeService referenceGenomeService

    File directory
    File file

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        referenceGenomeService = new ReferenceGenomeService()
        referenceGenomeService.configService = new ConfigService()
        referenceGenomeService.processingOptionService = new ProcessingOptionService()
        referenceGenomeService.fileSystemService = new TestFileSystemService()
        referenceGenomeService.fileService = new FileService([
                configService: referenceGenomeService.configService,
        ])
        referenceGenomeService.configService.processingOptionService = referenceGenomeService.processingOptionService

        directory = temporaryFolder.newFolder("reference_genomes", "referenceGenome")
        DomainFactory.createProcessingOptionBasePathReferenceGenome(directory.parent)


        file = new File(directory, "prefixName.fa")
        assert file.createNewFile()
        file << "test"

        project = DomainFactory.createProject()
        project.save(flush: true)

        referenceGenome = DomainFactory.createReferenceGenome(
                name                  : "hg19_1_24",
                path                  : "referenceGenome",
                fileNamePrefix        : "prefixName",
                cytosinePositionsIndex: "cytosine_idx.pos.gz",
                fingerPrintingFileName: 'fingerPrinting.bed',
        )

        referenceGenomeEntry = new ReferenceGenomeEntry(
                name           : "chr1",
                alias          : "1",
                classification : Classification.CHROMOSOME,
                referenceGenome: referenceGenome,
        )
        referenceGenomeEntry.save(flush: true)

        ReferenceGenomeEntry referenceGenomeEntryTwo = new ReferenceGenomeEntry(
                name           : "chr2",
                alias          : "2",
                classification : Classification.UNDEFINED,
                referenceGenome: referenceGenome,
        )
        referenceGenomeEntryTwo.save(flush: true)
    }

    @After
    void tearDown() {
        referenceGenomeEntry = null
        referenceGenome = null
        project = null
        referenceGenomeService = null
    }

    @Test
    void testRealmFilePathToDirectoryCorrect() {
        File pathExp = directory
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome) as File
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testRealmFilePathToDirectoryNullRefGen() {
        referenceGenome = null
        referenceGenomeService.referenceGenomeDirectory(referenceGenome)
    }

    @Test(expected = RuntimeException)
    void testRealmFilePathToDirectoryCanNotReadDirectory() {
        FileUtils.deleteDirectory(directory)
        assertFalse directory.exists()
        referenceGenomeService.referenceGenomeDirectory(referenceGenome)
    }

    @Test
    void testRealmFilePathToDirectoryCanNotReadDirectory_NoFileCheck() {
        assert directory.deleteDir()
        File pathExp = directory
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testFilePathToDirectory() {
        File pathExp = directory
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testFilePathToDirectoryWrongPath() {
        File wrongPath = new File("test")
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome)
        assertNotSame(wrongPath, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathToDirectoryRefGenIsNull() {
        referenceGenome = null
        referenceGenomeService.referenceGenomeDirectory(referenceGenome)
    }

    @Test(expected = RuntimeException)
    void testFilePathToDirectoryCanNotReadDirectory() {
        assert directory.deleteDir()
        assertFalse directory.exists()
        referenceGenomeService.referenceGenomeDirectory(referenceGenome)
    }

    @Test
    void testFilePathToDirectory_FileNotExistButNoFileCheck() {
        assert directory.deleteDir()
        File pathExp = directory
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testFilePath() {
        File pathExp = new File(directory, "prefixName.fa")
        File pathAct = referenceGenomeService.fastaFilePath(referenceGenome)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testFingerPrintingFile() {
        File pathExp = new File(directory, "fingerPrinting/fingerPrinting.bed")
        File pathAct = referenceGenomeService.fingerPrintingFile(referenceGenome, false)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFingerPrintingFileReferenceGenomeIsNull() {
        referenceGenome = null
        referenceGenomeService.fingerPrintingFile(referenceGenome, false)
    }

    @Test
    void testChromosomesInReferenceGenome() {
        List<ReferenceGenomeEntry> referenceGenomeEntriesExp = [referenceGenomeEntry]
        List<ReferenceGenomeEntry> referenceGenomeEntriesAct = referenceGenomeService.chromosomesInReferenceGenome(referenceGenome)
        assertEquals(referenceGenomeEntriesExp, referenceGenomeEntriesAct)
    }

    @Test
    void testPathToChromosomeSizeFilesPerReference_ReferenceGenomeIsNull_ShouldFail() {
        assert TestCase.shouldFail(IllegalArgumentException) {
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(null)
        }.contains('The reference genome is not specified')
    }

    @Test
    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithExistCheck_ShouldFail() {
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome, true)
        }.contains(ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
    }

    @Test
    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithoutExistCheck_AllFine() {
        File pathExp = new File(directory, ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        File pathAct = referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testPathToChromosomeSizeFilesPerReference_DirectoryExist_WithExistCheck_AllFine() {
        File pathExp = new File(directory, ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        CreateFileHelper.createFile(pathExp)
        File pathAct = referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome, true)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void test_cytosinePositionIndexFilePath_AllFine() {
        File pathExp = new File(directory, referenceGenome.cytosinePositionsIndex)
        CreateFileHelper.createFile(pathExp)

        assertEquals pathExp, referenceGenomeService.cytosinePositionIndexFilePath(referenceGenome)
    }

    @Test
    void test_cytosinePositionIndexFilePath_fileDoesntExist_shouldFail() {
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.cytosinePositionIndexFilePath(referenceGenome)
        }.contains(referenceGenome.cytosinePositionsIndex)
    }

    @Test
    void test_loadReferenceGenome() {
        String name = "my_reference_gnome"
        String path = "bwa06_my_reference_gnome"
        String fileNamePrefix = "my_reference_gnome"
        String cytosinePositionsIndex = null
        String fingerPrintingFileName = "my_fingerprint.bed"
        String statSizeFileName = "my_reference_gnome.fa.chrLenOnlyACGT.tab"
        String chromosomePrefix = ""
        String chromosomeSuffix = ""

        DomainFactory.createDefaultRealmWithProcessingOption()

        temporaryFolder.newFolder("reference_genomes", path)

        String fastaName = "chr21"
        String fastaAlias = "21"
        long fastaLength = 249250621
        long fastaLengthWithoutN = 238204518
        Classification fastaClassification = Classification.CHROMOSOME

        List<FastaEntry> fastaEntries = [
                new FastaEntry(fastaName, fastaAlias, fastaLength, fastaLengthWithoutN, fastaClassification),
        ]

        referenceGenomeService.loadReferenceGenome(name, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
                fastaEntries, fingerPrintingFileName, [statSizeFileName])

        ReferenceGenome referenceGenome = CollectionUtils.exactlyOneElement(ReferenceGenome.findAllByName(name))
        assert referenceGenome.path == path
        assert referenceGenome.fileNamePrefix == fileNamePrefix
        assert referenceGenome.cytosinePositionsIndex == cytosinePositionsIndex
        assert referenceGenome.fingerPrintingFileName == fingerPrintingFileName

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
