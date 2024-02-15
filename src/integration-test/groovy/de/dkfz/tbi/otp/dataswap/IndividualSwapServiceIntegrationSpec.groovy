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
import de.dkfz.tbi.otp.dataswap.parameters.IndividualSwapParameters
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactoryInstance
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper

import java.nio.file.Path
import java.nio.file.Files

@Rollback
@Integration
class IndividualSwapServiceIntegrationSpec extends Specification implements UserAndRoles, IsRoddy {

    IndividualSwapService individualSwapService
    TestConfigService configService
    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    RawSequenceDataViewFileService rawSequenceDataViewFileService

    @TempDir
    Path tempDir

    void setupData() {
        createUserAndRoles()
        Path outputFolder = Files.createDirectory(tempDir.resolve("outputFolder"))
        configService.addOtpProperties(outputFolder)
    }

    void cleanup() {
        configService.clean()
    }

    // false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "swap, succeed if parameters match existing entities and data files"() {
        given:
        setupData()
        final SeqType seqType = DomainFactory.createAllAlignableSeqTypes().first()
        MergingWorkPackage workPackage = createMergingWorkPackage(seqType: seqType)
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                workPackage: workPackage,
        ])
        Project newProject = DomainFactory.createProject()
        String scriptName = "TEST-MOVE-INDIVIDUAL"
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        Map<String,String> fastqFileLinks = [:]
        Map<String,String> fastqFilePaths = [:]
        RawSequenceFile.findAllBySeqTrack(seqTrack).each {
            Path path = rawSequenceDataWorkFileService.getFilePath(it)
            Files.createDirectories(path.parent)
            Files.createFile(path)
            fastqFileLinks[it] = rawSequenceDataViewFileService.getFilePath(it)
            fastqFilePaths[it] = rawSequenceDataWorkFileService.getFilePath(it)
        }
        File missedFile = bamFile.finalMd5sumFile
        File unexpectedFile = new File(bamFile.baseDirectory, 'notExpectedFile.txt')

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFile)
        assert missedFile.delete()
        assert unexpectedFile.createNewFile()

        File destinationDirectory = bamFile.baseDirectory

        Path scriptFolder = Files.createDirectory(tempDir.resolve("files"))

        StringBuilder outputLog = new StringBuilder()

        Individual oldIndividual = bamFile.individual
        oldIndividual.species = TaxonomyFactoryInstance.INSTANCE.createSpeciesWithStrain()

        Path cleanupPath = oldIndividual.getViewByPidPath(seqType).absoluteDataManagementPath.toPath()

        when:
        doWithAuth(ADMIN) {
            individualSwapService.swap(
                    new IndividualSwapParameters(
                            projectNameSwap: new Swap(bamFile.project.name, newProject.name),
                            pidSwap: new Swap(oldIndividual.pid, oldIndividual.pid),
                            sampleTypeSwaps: [
                                    new Swap((bamFile.sampleType.name), ""),
                            ],
                            rawSequenceFileSwaps: [
                                    new Swap('DataFileFileName_R1.gz', ""),
                                    new Swap('DataFileFileName_R2.gz', ""),
                            ],
                            bashScriptName: scriptName,
                            log: outputLog,
                            failOnMissingFiles: false,
                            scriptOutputDirectory: scriptFolder,
                            linkedFilesVerified: false,
                    )
            )
        }

        then:
        scriptFolder.toFile().listFiles().length != 0

        File alignmentScript = scriptFolder.resolve("restartAli_${scriptName}.groovy").toFile()
        alignmentScript.exists()

        File copyScript = scriptFolder.resolve("${scriptName}.sh").toFile()
        copyScript.exists()
        String copyScriptContent = copyScript.text
        copyScriptContent.contains("rm -rf ${destinationDirectory}")
        copyScriptContent.startsWith(AbstractDataSwapService.BASH_HEADER)
        RawSequenceFile.findAllBySeqTrack(seqTrack).each { RawSequenceFile it ->
            assert copyScriptContent.contains("mkdir -p -m 2750 '${rawSequenceDataWorkFileService.getFilePath(it).parent}'")
            assert copyScriptContent.contains("mv '${fastqFilePaths[it]}' \\\n   '${rawSequenceDataWorkFileService.getFilePath(it)}'")
            assert copyScriptContent.contains("mv '${fastqFilePaths[it]}.md5sum' \\\n     '${rawSequenceDataWorkFileService.getMd5sumPath(it)}'")
            assert copyScriptContent.contains("rm -f '${fastqFileLinks[it]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${rawSequenceDataViewFileService.getDirectoryPath(it)}'")
            assert copyScriptContent.contains("ln -sr '${rawSequenceDataWorkFileService.getFilePath(it)}' \\\n      '${rawSequenceDataViewFileService.getFilePath(it)}'")
            assert it.comment.comment == "Attention: Datafile swapped!"
        }
        copyScriptContent.contains("rm -rf ${cleanupPath}")
        CollectionUtils.exactlyOneElement(Individual.findAllByPid(oldIndividual.pid)).species == oldIndividual.species
    }
}
