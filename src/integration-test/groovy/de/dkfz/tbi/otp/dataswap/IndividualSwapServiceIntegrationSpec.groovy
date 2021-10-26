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
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataswap.parameters.IndividualSwapParameters
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper

import java.nio.file.Path

@Rollback
@Integration
class IndividualSwapServiceIntegrationSpec extends Specification implements UserAndRoles {

    IndividualSwapService individualSwapService
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

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "swap, succeed if parameters match existing entities and data files"() {
        given:
        setupData()
        DomainFactory.createDefaultRealmWithProcessingOption()
        DomainFactory.createAllAlignableSeqTypes()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        Project newProject = DomainFactory.createProject(realm: bamFile.project.realm)
        String scriptName = "TEST-MOVE-INDIVIDUAL"
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        List<String> dataFilePaths = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
            dataFilePaths.add(lsdfFilesService.getFileFinalPath(it))
        }
        File missedFile = bamFile.finalMd5sumFile
        File unexpectedFile = new File(bamFile.baseDirectory, 'notExpectedFile.txt')

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFile)
        assert missedFile.delete()
        assert unexpectedFile.createNewFile()

        File destinationDirectory = bamFile.baseDirectory

        Path scriptFolder = temporaryFolder.newFolder("files").toPath()

        StringBuilder outputLog = new StringBuilder()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            individualSwapService.swap(
                    new IndividualSwapParameters(
                            projectNameSwap: new Swap(bamFile.project.name, newProject.name),
                            pidSwap: new Swap(bamFile.individual.pid, bamFile.individual.pid),
                            sampleTypeSwaps: [
                                    new Swap((bamFile.sampleType.name), ""),
                            ],
                            dataFileSwaps: [
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
        copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        copyScriptContent.startsWith(DataSwapService.BASH_HEADER)
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileFinalPath(it)).parent}'")
            assert copyScriptContent.contains("mv '${dataFilePaths[i]}' \\\n   '${lsdfFilesService.getFileFinalPath(it)}'")
            assert copyScriptContent.contains("mv '${dataFilePaths[i]}.md5sum' \\\n     '${lsdfFilesService.getFileFinalPath(it)}.md5sum'")
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).parent}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' \\\n      '${lsdfFilesService.getFileViewByPidPath(it)}'")
            assert it.comment.comment == "Attention: Datafile swapped!"
        }
    }
}
