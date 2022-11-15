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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.referencegenome.FastaEntry
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector

@Rollback
@Integration
class ReferenceGenomeServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore, TaxonomyFactory {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    ReferenceGenomeService referenceGenomeService
    TestConfigService configService

    ReferenceGenome referenceGenome

    void setupData() {
        referenceGenomeService = new ReferenceGenomeService(
                configService          : configService,
                fileService            : new FileService(),
                fileSystemService      : new TestFileSystemService(),
                processingOptionService: new ProcessingOptionService(),
        )
        referenceGenomeService.configService.processingOptionService = referenceGenomeService.processingOptionService
        referenceGenomeService.fileService.configService = referenceGenomeService.configService
        referenceGenome = createReferenceGenome()

        File referenceGenomeDirectory = temporaryFolder.newFolder("reference_genomes", referenceGenome.path)
        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomeDirectory.parent)
        DomainFactory.createDefaultRealmWithProcessingOption()
    }

    void "createReferenceGenomeMetafile, file is created with expected content"() {
        given:
        setupData()

        [1, 2, 3].each {
            DomainFactory.createReferenceGenomeEntry(
                    referenceGenome: referenceGenome,
                    name           : "chr_${it}",
                    length         : it,
                    lengthWithoutN : it,
            )
        }

        File expectedFile = referenceGenomeService.referenceGenomeMetaInformationPath(referenceGenome)
        String expectedContent = """\
            |chr_1\t1\t1
            |chr_2\t2\t2
            |chr_3\t3\t3""".stripMargin()

        when:
        referenceGenomeService.createReferenceGenomeMetafile(referenceGenome)

        then:
        expectedFile.exists()
        expectedFile.text == expectedContent
    }

    void "test loadReferenceGenome"() {
        given:
        setupData()
        createUserAndRoles()

        String name = "my_reference_gnome"
        String path = "bwa06_my_reference_gnome"
        String fileNamePrefix = "my_reference_gnome"
        String cytosinePositionsIndex = null
        String fingerPrintingFileName = "my_fingerprint.bed"
        String statSizeFileName = "my_reference_gnome.fa.chrLenOnlyACGT.tab"
        String chromosomePrefix = ""
        String chromosomeSuffix = ""
        Set<Species> species = [createSpecies(), createSpecies()] as Set
        Set<SpeciesWithStrain> speciesWithStrain = [createSpeciesWithStrain(), createSpeciesWithStrain()] as Set

        DomainFactory.createDefaultRealmWithProcessingOption()

        temporaryFolder.newFolder("reference_genomes", path).toPath()

        String fastaName = "chr21"
        String fastaAlias = "21"
        long fastaLength = 249250621
        long fastaLengthWithoutN = 238204518
        ReferenceGenomeEntry.Classification fastaClassification = ReferenceGenomeEntry.Classification.CHROMOSOME

        List<FastaEntry> fastaEntries = [
                new FastaEntry(fastaName, fastaAlias, fastaLength, fastaLengthWithoutN, fastaClassification),
        ]

        when:
        doWithAuth(OPERATOR) {
            referenceGenomeService.loadReferenceGenome(name, species, speciesWithStrain, path, fileNamePrefix,
                    cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
                    fastaEntries, fingerPrintingFileName, statSizeFileName, [])
        }

        then:
        ReferenceGenome referenceGenome = CollectionUtils.exactlyOneElement(ReferenceGenome.findAllByName(name))
        referenceGenome.path == path
        referenceGenome.fileNamePrefix == fileNamePrefix
        referenceGenome.cytosinePositionsIndex == cytosinePositionsIndex
        referenceGenome.fingerPrintingFileName == fingerPrintingFileName
        referenceGenome.species == species
        referenceGenome.speciesWithStrain == speciesWithStrain

        ReferenceGenomeEntry entry = CollectionUtils.exactlyOneElement(ReferenceGenomeEntry.findAllByName(fastaName))
        entry.referenceGenome == referenceGenome
        entry.alias == fastaAlias
        entry.length == fastaLength
        entry.lengthWithoutN == fastaLengthWithoutN
        entry.classification == fastaClassification

        StatSizeFileName statSizeFileName1 = CollectionUtils.exactlyOneElement(StatSizeFileName.findAllByName(statSizeFileName))
        statSizeFileName1.referenceGenome == referenceGenome

        ExternalWorkflowConfigSelector selector = CollectionUtils.exactlyOneElement(ExternalWorkflowConfigSelector.all)
        selector.referenceGenomes == [referenceGenome] as Set
        selector.fragments.first().configValues.contains(statSizeFileName)
    }
}
