/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.dataswap

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataswap.parameters.SampleSwapParameters
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactoryInstance
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper

import java.nio.file.Path
import java.nio.file.Files

@Rollback
@Integration
class SampleSwapServiceIntegrationSpec extends Specification implements UserAndRoles, IsRoddy {

    SampleSwapService sampleSwapService
    LsdfFilesService lsdfFilesService
    TestConfigService configService

    @TempDir
    Path tempDir

    Path outputFolder

    void setupData() {
        createUserAndRoles()
        outputFolder = Files.createDirectory(tempDir.resolve("outputFolder"))
        configService.addOtpProperties(outputFolder)
    }

    void cleanup() {
        configService.clean()
    }

    void "swap, succeed if parameters match existing entities and data files"() {
        given:
        setupData()
        final SeqType seqType = DomainFactory.createAllAlignableSeqTypes().first()
        MergingWorkPackage workPackage = createMergingWorkPackage(seqType: seqType)
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                workPackage                 : workPackage,
        ])
        String script = "TEST-MOVE_SAMPLE"
        Individual individual = DomainFactory.createIndividual(project: bamFile.project)

        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        seqTrack.sample.mixedInSpecies = [
                TaxonomyFactoryInstance.INSTANCE.createSpeciesWithStrain(),
                TaxonomyFactoryInstance.INSTANCE.createSpeciesWithStrain(),
        ]

        List<String> fastqFileLinks = []
        RawSequenceFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            fastqFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
        }

        String rawSequenceFileName1 = 'DataFileFileName_R1.gz'
        String rawSequenceFileName2 = 'DataFileFileName_R2.gz'

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFile)
        File destinationDirectory = bamFile.baseDirectory

        Path scriptFolder = Files.createDirectory(tempDir.resolve("files"))

        Individual oldIndividual = bamFile.individual
        oldIndividual.species = TaxonomyFactoryInstance.INSTANCE.createSpeciesWithStrain()

        Path cleanupPath = oldIndividual.getViewByPidPath(seqType).absoluteDataManagementPath.toPath()

        SampleType sampleType = bamFile.sampleType

        when:
        doWithAuth(ADMIN) {
            sampleSwapService.swap(
                    new SampleSwapParameters(
                            projectNameSwap: new Swap(bamFile.project.name, bamFile.project.name),
                            pidSwap: new Swap(oldIndividual.pid, individual.pid),
                            sampleTypeSwap: new Swap(sampleType.name, sampleType.name),
                            rawSequenceFileSwaps: [new Swap(rawSequenceFileName1, ""), new Swap(rawSequenceFileName2, "")],
                            bashScriptName: script,
                            log: new StringBuilder(),
                            failOnMissingFiles: false,
                            scriptOutputDirectory: scriptFolder,
                            linkedFilesVerified: false,
                    )
            )
        }

        then:
        notThrown(Throwable)

        scriptFolder.toFile().listFiles().length != 0

        File alignmentScript = scriptFolder.resolve("restartAli_${script}.groovy").toFile()
        alignmentScript.exists()
        alignmentScript.text.contains("${bamFile.seqTracks.iterator().next().id},")

        File copyScript = scriptFolder.resolve("${script}.sh").toFile()
        copyScript.exists()
        String copyScriptContent = copyScript.text
        copyScriptContent.startsWith(AbstractDataSwapService.BASH_HEADER)
        copyScriptContent.contains("rm -rf ${destinationDirectory}")
        RawSequenceFile.findAllBySeqTrack(seqTrack).eachWithIndex { RawSequenceFile it, int i ->
            assert copyScriptContent.contains("rm -f '${fastqFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).parent}'")
            assert copyScriptContent.contains("ln -sr '${lsdfFilesService.getFileFinalPath(it)}' \\\n      '${lsdfFilesService.getFileViewByPidPath(it)}'")
            assert it.comment.comment == "Attention: Datafile swapped!"
        }
        copyScriptContent.contains("rm -rf ${cleanupPath}")

        individual.species == oldIndividual.species
        CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType)).mixedInSpecies.size() == 2
    }
}
