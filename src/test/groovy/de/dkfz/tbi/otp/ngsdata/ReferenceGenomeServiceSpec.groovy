/*
 * Copyright 2011-2026 The OTP authors
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
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.*

class ReferenceGenomeServiceSpec extends Specification implements DataTest, ServiceUnitTest<ReferenceGenomeService>, TaxonomyFactory, DomainFactoryCore {

    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntry
    Project project

    Path directory
    Path file

    @TempDir
    Path tempDir

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                ProcessingOption,
                ProcessingPriority,
                Project,
                ReferenceGenome,
                ReferenceGenomeEntry,
                StatSizeFileName,
        ]
    }

    void setupTest() {
        service.configService = new TestConfigService()
        service.processingOptionService = new ProcessingOptionService()
        service.fileSystemService = new TestFileSystemService()
        service.fileService = new FileService([
                configService    : service.configService,
                remoteShellHelper: Mock(RemoteShellHelper) {
                    _ * executeCommandReturnProcessOutput(_) >> { String command ->
                        LocalShellHelper.executeAndWait(command)
                    }
                },
        ])
        service.configService.processingOptionService = service.processingOptionService

        directory = tempDir.resolve("reference_genomes/referenceGenome")
        DomainFactory.createProcessingOptionBasePathReferenceGenome(directory.parent.toString())

        file = CreateFileHelper.createFile(directory.resolve("prefixName.fa"), "test")

        project = createProject()
        project.save(flush: true)

        referenceGenome = createReferenceGenome(
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

    void testFilePathToDirectoryCorrect() {
        given:
        setupTest()
        Path pathExp = directory

        when:
        Path pathAct = service.referenceGenomeDirectory(referenceGenome)

        then:
        pathExp == pathAct
    }

    void testFilePathToDirectoryNullRefGen() {
        given:
        setupTest()
        referenceGenome = null

        when:
        service.referenceGenomeDirectory(referenceGenome)

        then:
        thrown IllegalArgumentException
    }

    void testFilePathToDirectoryCanNotReadDirectory2() {
        given:
        setupTest()
        directory.deleteDir()

        when:
        service.referenceGenomeDirectory(referenceGenome)

        then:
        !Files.exists(directory)
        thrown RuntimeException
    }

    void testFilePathToDirectoryCanNotReadDirectory_NoFileCheck() {
        given:
        setupTest()
        boolean deletion = directory.deleteDir()
        Path pathExp = directory

        when:
        Path pathAct = service.referenceGenomeDirectory(referenceGenome, false)

        then:
        deletion
        pathExp == pathAct
    }

    void testFilePathToDirectory() {
        given:
        setupTest()
        Path pathExp = directory

        when:
        Path pathAct = service.referenceGenomeDirectory(referenceGenome)

        then:
        pathExp == pathAct
    }

    void testFilePathToDirectoryWrongPath() {
        given:
        setupTest()
        Path wrongPath = Paths.get("test")

        when:
        Path pathAct = service.referenceGenomeDirectory(referenceGenome)

        then:
        wrongPath != pathAct
    }

    void testFilePathToDirectoryRefGenIsNull() {
        given:
        setupTest()
        referenceGenome = null

        when:
        service.referenceGenomeDirectory(referenceGenome)

        then:
        thrown IllegalArgumentException
    }

    void testFilePathToDirectoryCanNotReadDirectory() {
        given:
        setupTest()
        boolean deletion = directory.deleteDir()

        when:
        service.referenceGenomeDirectory(referenceGenome)

        then:
        deletion
        !Files.exists(directory)
        thrown RuntimeException
    }

    void testFilePathToDirectory_FileNotExistButNoFileCheck() {
        given:
        setupTest()
        boolean deletion = directory.deleteDir()
        Path pathExp = directory

        when:
        Path pathAct = service.referenceGenomeDirectory(referenceGenome, false)

        then:
        deletion
        pathExp == pathAct
    }

    void testFilePath() {
        given:
        setupTest()
        Path pathExp = directory.resolve("prefixName.fa")

        when:
        Path pathAct = service.fastaFilePath(referenceGenome)

        then:
        pathExp == pathAct
    }

    void testFingerPrintingFile() {
        given:
        setupTest()
        Path pathExp = directory.resolve("fingerPrinting/fingerPrinting.bed")

        when:
        Path pathAct = service.fingerPrintingFile(referenceGenome, false)

        then:
        pathExp == pathAct
    }

    void testFingerPrintingFileReferenceGenomeIsNull() {
        given:
        setupTest()
        referenceGenome = null

        when:
        service.fingerPrintingFile(referenceGenome, false)

        then:
        thrown IllegalArgumentException
    }

    void testChromosomesInReferenceGenome() {
        given:
        setupTest()
        List<ReferenceGenomeEntry> referenceGenomeEntriesExp = [referenceGenomeEntry]

        when:
        List<ReferenceGenomeEntry> referenceGenomeEntriesAct = service.chromosomesInReferenceGenome(referenceGenome)

        then:
        referenceGenomeEntriesExp == referenceGenomeEntriesAct
    }

    void testPathToChromosomeSizeFilesPerReference_ReferenceGenomeIsNull_ShouldFail() {
        given:
        setupTest()

        when:
        service.pathToChromosomeSizeFilesPerReference(null)

        then:
        Exception e = thrown IllegalArgumentException
        e.message.contains('The reference genome is not specified')
    }

    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithExistCheck_ShouldFail() {
        given:
        setupTest()

        when:
        service.pathToChromosomeSizeFilesPerReference(referenceGenome, true)

        then:
        Exception e = thrown RuntimeException
        e.message.contains(ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
    }

    void testPathToChromosomeSizeFilesPerReference_DirectoryDoesNotExist_WithoutExistCheck_AllFine() {
        given:
        setupTest()
        Path pathExp = directory.resolve(ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)

        when:
        Path pathAct = service.pathToChromosomeSizeFilesPerReference(referenceGenome, false)

        then:
        pathExp == pathAct
    }

    void testPathToChromosomeSizeFilesPerReference_DirectoryExist_WithExistCheck_AllFine() {
        given:
        setupTest()
        Path pathExp = directory.resolve(ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        Files.createFile(pathExp)

        when:
        Path pathAct = service.pathToChromosomeSizeFilesPerReference(referenceGenome, true)

        then:
        pathExp == pathAct
    }

    void test_cytosinePositionIndexFilePath_AllFine() {
        given:
        setupTest()
        Path pathExp = directory.resolve(referenceGenome.cytosinePositionsIndex)

        when:
        Files.createFile(pathExp)

        then:
        pathExp == service.cytosinePositionIndexFilePath(referenceGenome)
    }

    void test_cytosinePositionIndexFilePath_fileDoesntExist_shouldFail() {
        given:
        setupTest()

        when:
        service.cytosinePositionIndexFilePath(referenceGenome)

        then:
        Exception e = thrown RuntimeException
        e.message.contains(referenceGenome.cytosinePositionsIndex)
    }

    void "getAllSpeciesWithStrains, should return all the reference genomes species with strains including the ones from species"() {
        given:
        Species species = createSpecies()
        SpeciesWithStrain speciesWithStrain1 = createSpeciesWithStrain([species: species])
        SpeciesWithStrain speciesWithStrain2 = createSpeciesWithStrain([species: species])
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain([species: species])
        SpeciesWithStrain speciesWithStrain4 = createSpeciesWithStrain()
        SpeciesWithStrain speciesWithStrain5 = createSpeciesWithStrain()
        referenceGenome = createReferenceGenome([
                species          : [species],
                speciesWithStrain: [speciesWithStrain4, speciesWithStrain5],
        ])
        createSpeciesWithStrain()

        expect:
        TestCase.assertContainSame(
                service.getAllSpeciesWithStrains([referenceGenome] as Set),
                [speciesWithStrain1, speciesWithStrain2, speciesWithStrain3, speciesWithStrain4, speciesWithStrain5]
        )
    }

    void "getAllSpeciesWithStrains, should return empty list for null or empty reference genomes list"() {
        given:
        Species species = createSpecies()
        createSpeciesWithStrain([species: species])
        createSpeciesWithStrain()

        expect:
        service.getAllSpeciesWithStrains(null) == [] as Set
        service.getAllSpeciesWithStrains([] as Set) == [] as Set
    }

    void "getSpeciesWithStrainCombinations, should return the ordered possible speciesWithStrain for a reference Genome including species"() {
        given:
        Species species = createSpecies()
        SpeciesWithStrain speciesWithStrain1 = createSpeciesWithStrain([species: species])
        SpeciesWithStrain speciesWithStrain2 = createSpeciesWithStrain([species: species])
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain([species: species])
        SpeciesWithStrain speciesWithStrain4 = createSpeciesWithStrain()
        SpeciesWithStrain speciesWithStrain5 = createSpeciesWithStrain()
        referenceGenome = createReferenceGenome([
                species          : [species],
                speciesWithStrain: [speciesWithStrain4, speciesWithStrain5],
        ])
        createSpeciesWithStrain()

        expect:
        TestCase.assertContainSame(
                service.getSpeciesWithStrainCombinations(referenceGenome),
                [[speciesWithStrain1, speciesWithStrain2, speciesWithStrain3], [speciesWithStrain4], [speciesWithStrain5]],
        )
    }

    void "getSpeciesWithStrainCombinations, should return the ordered possible speciesWithStrain for a reference Genome even when no species is defined"() {
        given:
        Species species = createSpecies()
        createSpeciesWithStrain([species: species])
        SpeciesWithStrain speciesWithStrain2 = createSpeciesWithStrain()
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain()
        referenceGenome = createReferenceGenome([
                species          : [],
                speciesWithStrain: [speciesWithStrain2, speciesWithStrain3],
        ])
        createSpeciesWithStrain()

        expect:
        TestCase.assertContainSame(
                service.getSpeciesWithStrainCombinations(referenceGenome),
                [[speciesWithStrain2], [speciesWithStrain3]],
        )
    }

    void "getSpeciesWithStrainCombinations, should return the ordered possible speciesWithStrain for a reference Genome, when only species are defined"() {
        given:
        Species species1 = createSpecies()
        Species species2 = createSpecies()
        SpeciesWithStrain speciesWithStrain1 = createSpeciesWithStrain([species: species1])
        SpeciesWithStrain speciesWithStrain2 = createSpeciesWithStrain([species: species1])
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain([species: species2])
        createSpeciesWithStrain()
        referenceGenome = createReferenceGenome([
                species          : [species1, species2],
                speciesWithStrain: [],
        ])
        createSpeciesWithStrain()

        expect:
        TestCase.assertContainSame(
                service.getSpeciesWithStrainCombinations(referenceGenome),
                [[speciesWithStrain1, speciesWithStrain2], [speciesWithStrain3]],
        )
    }

    void "getRemainingSpeciesWithStrainOptions, should only return the species with strain that are remaining for a ref genome"() {
        given:
        Species species1 = createSpecies()
        Species species2 = createSpecies()
        SpeciesWithStrain speciesWithStrain1 = createSpeciesWithStrain([species: species1])
        createSpeciesWithStrain([species: species1])
        createSpeciesWithStrain([species: species1])
        SpeciesWithStrain speciesWithStrain2 = createSpeciesWithStrain([species: species2])
        SpeciesWithStrain speciesWithStrain3 = createSpeciesWithStrain([species: species2])
        SpeciesWithStrain speciesWithStrain4 = createSpeciesWithStrain()
        SpeciesWithStrain speciesWithStrain5 = createSpeciesWithStrain()
        referenceGenome = createReferenceGenome([
                species          : [species1, species2],
                speciesWithStrain: [speciesWithStrain4, speciesWithStrain5],
        ])
        createSpeciesWithStrain()

        expect:
        TestCase.assertContainSame(
                service.getSpeciesWithStrainOptions(referenceGenome, [speciesWithStrain1, speciesWithStrain4]),
                [speciesWithStrain1, speciesWithStrain2, speciesWithStrain3, speciesWithStrain4, speciesWithStrain5],
        )
    }
}
