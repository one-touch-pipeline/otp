/*
 * Copyright 2011-2021 The OTP authors
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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataswap.parameters.SampleSwapParameters
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper

import java.nio.file.Path

@Rollback
@Integration
class SampleSwapServiceIntegrationSpec extends Specification implements UserAndRoles, IsRoddy {

    SampleSwapService sampleSwapService
    LsdfFilesService lsdfFilesService
    TestConfigService configService

    @Rule
    TemporaryFolder temporaryFolder

    Path outputFolder

    void setupData() {
        createUserAndRoles()
        outputFolder = temporaryFolder.newFolder("outputFolder").toPath()
        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : outputFolder.toString(),
                (OtpProperty.PATH_PROCESSING_ROOT): outputFolder.toString(),
        ])
    }

    void cleanup() {
        configService.clean()
    }

    void "swap, succeed if parameters match existing entities and data files"() {
        given:
        setupData()
        DomainFactory.createDefaultRealmWithProcessingOption()
        final SeqType seqType = DomainFactory.createAllAlignableSeqTypes().first()
        MergingWorkPackage workPackage = createMergingWorkPackage(seqType: seqType)
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                workPackage                 : workPackage,
        ])
        String script = "TEST-MOVE_SAMPLE"
        Individual individual = DomainFactory.createIndividual(project: bamFile.project)

        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
        }

        String dataFileName1 = 'DataFileFileName_R1.gz'
        String dataFileName2 = 'DataFileFileName_R2.gz'

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFile)
        File destinationDirectory = bamFile.baseDirectory

        Path scriptFolder = temporaryFolder.newFolder("files").toPath()

        Individual oldIndividual = bamFile.individual
        Path cleanupPath = oldIndividual.getViewByPidPath(seqType).absoluteDataManagementPath.toPath()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            sampleSwapService.swap(
                    new SampleSwapParameters(
                            projectNameSwap: new Swap(bamFile.project.name, bamFile.project.name),
                            pidSwap: new Swap(oldIndividual.pid, individual.pid),
                            sampleTypeSwap: new Swap(bamFile.sampleType.name, bamFile.sampleType.name),
                            dataFileSwaps: [new Swap(dataFileName1, ""), new Swap(dataFileName2, "")],
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
        copyScriptContent.startsWith(DataSwapService.BASH_HEADER)
        copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).parent}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' \\\n      '${lsdfFilesService.getFileViewByPidPath(it)}'")
            assert it.comment.comment == "Attention: Datafile swapped!"
        }
        copyScriptContent.contains("rm -rf ${cleanupPath}")
    }
}
