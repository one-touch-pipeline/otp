/*
 * Copyright 2011-2022 The OTP authors
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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.apache.commons.io.FileUtils
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.Path

class ReferenceGenomeServiceSpec extends Specification implements DataTest, ServiceUnitTest<ReferenceGenomeService> {

    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntry
    Project project
    ReferenceGenomeService referenceGenomeService

    File directory
    File file

    @TempDir
    Path tempDir

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                ProcessingOption,
                ProcessingPriority,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeEntry,
                StatSizeFileName,
        ]
    }

    void setupTest() {
        referenceGenomeService = new ReferenceGenomeService()
        referenceGenomeService.configService = new TestConfigService()
        referenceGenomeService.processingOptionService = new ProcessingOptionService()
        referenceGenomeService.fileSystemService = new TestFileSystemService()
        referenceGenomeService.fileService = new FileService([
                configService: referenceGenomeService.configService,
        ])
        referenceGenomeService.configService.processingOptionService = referenceGenomeService.processingOptionService

        directory = tempDir.resolve("reference_genomes/referenceGenome").toFile()
        DomainFactory.createProcessingOptionBasePathReferenceGenome(directory.parent)

        file = CreateFileHelper.createFile(directory.toPath().resolve("prefixName.fa"), "test").toFile()

        project = DomainFactory.createProject()
        project.save(flush: true)

        referenceGenome = DomainFactory.createReferenceGenome(
                name: "hg19_1_24",
                path: "referenceGenome",
                fileNamePrefix: "prefixName",
                cytosinePositionsIndex: "cytosine_idx.pos.gz",
                fingerPrintingFileName: 'fingerPrinting.bed',
        )

        referenceGenomeEntry = new ReferenceGenomeEntry(
                name: "chr1",
                alias: "1",
                classification: Classification.CHROMOSOME,
                referenceGenome: referenceGenome,
        )
        referenceGenomeEntry.save(flush: true)

        ReferenceGenomeEntry referenceGenomeEntryTwo = new ReferenceGenomeEntry(
                name: "chr2",
                alias: "2",
                classification: Classification.UNDEFINED,
                referenceGenome: referenceGenome,
        )
        referenceGenomeEntryTwo.save(flush: true)
    }

    void testRealmFilePathToDirectoryCorrect() {
        given:
        setupTest()
        File pathExp = directory

        when:
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome) as File

        then:
        pathExp == pathAct
    }

    void testRealmFilePathToDirectoryNullRefGen() {
        given:
        setupTest()
        referenceGenome = null

        when:
        referenceGenomeService.referenceGenomeDirectory(referenceGenome)

        then:
        thrown IllegalArgumentException
    }

    void testRealmFilePathToDirectoryCanNotReadDirectory() {
        given:
        setupTest()
        FileUtils.deleteDirectory(directory)

        when:
        referenceGenomeService.referenceGenomeDirectory(referenceGenome)

        then:
        directory.exists() == false
        thrown RuntimeException
    }

    void testRealmFilePathToDirectoryCanNotReadDirectory_NoFileCheck() {
        given:
        setupTest()
        boolean deletion = directory.deleteDir()
        File pathExp = directory

        when:
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome, false)

        then:
        deletion == true
        pathExp == pathAct
    }

    void testFilePathToDirectory() {
        given:
        setupTest()
        File pathExp = directory

        when:
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome)

        then:
        pathExp == pathAct
    }

    void testFilePathToDirectoryWrongPath() {
        given:
        setupTest()
        File wrongPath = new File("test")

        when:
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome)

        then:
        wrongPath != pathAct
    }

    void testFilePathToDirectoryRefGenIsNull() {
        given:
        setupTest()
        referenceGenome = null

        when:
        referenceGenomeService.referenceGenomeDirectory(referenceGenome)

        then:
        thrown IllegalArgumentException
    }

    void testFilePathToDirectoryCanNotReadDirectory() {
        given:
        setupTest()
        boolean deletion = directory.deleteDir()

        when:
        referenceGenomeService.referenceGenomeDirectory(referenceGenome)

        then:
        deletion == true
        directory.exists() == false
        thrown RuntimeException
    }

    void testFilePathToDirectory_FileNotExistButNoFileCheck() {
        given:
        setupTest()
        boolean deletion = directory.deleteDir()
        File pathExp = directory

        when:
        File pathAct = referenceGenomeService.referenceGenomeDirectory(referenceGenome, false)

        then:
        deletion == true
        pathExp == pathAct
    }

    void testFilePath() {
        given:
        setupTest()
        File pathExp = new File(directory, "prefixName.fa")

        when:
        File pathAct = referenceGenomeService.fastaFilePath(referenceGenome)

        then:
        pathExp == pathAct
    }

    void testFingerPrintingFile() {
        given:
        setupTest()
        File pathExp = new File(directory, "fingerPrinting/fingerPrinting.bed")

        when:
        File pathAct = referenceGenomeService.fingerPrintingFile(referenceGenome, false)

        then:
        pathExp == pathAct
    }

    void testFingerPrintingFileReferenceGenomeIsNull() {
        given:
        setupTest()
        referenceGenome = null

        when:
        referenceGenomeService.fingerPrintingFile(referenceGenome, false)

        then:
        thrown IllegalArgumentException
    }

    void testChromosomesInReferenceGenome() {
        given:
        setupTest()
        List<ReferenceGenomeEntry> referenceGenomeEntriesExp = [referenceGenomeEntry]

        when:
        List<ReferenceGenomeEntry> referenceGenomeEntriesAct = referenceGenomeService.chromosomesInReferenceGenome(referenceGenome)

        then:
        referenceGenomeEntriesExp == referenceGenomeEntriesAct
    }

    void testPathToChromosomeSizeFilesPerReference_ReferenceGenomeIsNull_ShouldFail() {
        given:
        setupTest()

        when:
        referenceGenomeService.pathToChromosomeSizeFilesPerReference(null)

        then:
        Exception e = thrown IllegalArgumentException
        e.message.contains('The reference genome is not specified')
    }

    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithExistCheck_ShouldFail() {
        given:
        setupTest()

        when:
        referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome, true)

        then:
        Exception e = thrown RuntimeException
        e.message.contains(ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
    }

    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithoutExistCheck_AllFine() {
        given:
        setupTest()
        File pathExp = new File(directory, ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)

        when:
        File pathAct = referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome, false)

        then:
        pathExp == pathAct
    }

    void testPathToChromosomeSizeFilesPerReference_DirectoryExist_WithExistCheck_AllFine() {
        given:
        setupTest()
        File pathExp = new File(directory, ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        CreateFileHelper.createFile(pathExp)

        when:
        File pathAct = referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome, true)

        then:
        pathExp == pathAct
    }

    void test_cytosinePositionIndexFilePath_AllFine() {
        given:
        setupTest()
        File pathExp = new File(directory, referenceGenome.cytosinePositionsIndex)

        when:
        CreateFileHelper.createFile(pathExp)

        then:
        pathExp == referenceGenomeService.cytosinePositionIndexFilePath(referenceGenome)
    }

    void test_cytosinePositionIndexFilePath_fileDoesntExist_shouldFail() {
        given:
        setupTest()

        when:
        referenceGenomeService.cytosinePositionIndexFilePath(referenceGenome)

        then:
        Exception e = thrown RuntimeException
        e.message.contains(referenceGenome.cytosinePositionsIndex)
    }
}
