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
package de.dkfz.tbi.otp.withdraw

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.IndividualService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Path

@Rollback
@Integration
class UnwithdrawServiceIntegrationSpec extends Specification implements DomainFactoryCore, IsRoddy {

    @Rule
    TemporaryFolder temporaryFolder

    TestConfigService configService

    void setup() {
        configService.addOtpProperties(temporaryFolder.newFolder().toPath())
    }

    void cleanup() {
        configService.clean()
    }

    // this test didn't work as unit test because of problems with validating indelCallingInstance.config correctly
    void "test unwithdrawAnalysis, unwithdraws successfully"() {
        given:
        UnwithdrawStateHolder state = new UnwithdrawStateHolder()

        IndelCallingService indelService = new IndelCallingService()
        indelService.individualService = new IndividualService()
        indelService.individualService.projectService = new ProjectService()
        indelService.individualService.projectService.configService = configService
        indelService.individualService.projectService.fileSystemService = new TestFileSystemService()

        FileSystemService fileSystemService = new TestFileSystemService()
        WithdrawAnalysisService withdrawAnalysisService = new WithdrawAnalysisService(
                bamFileAnalysisServiceFactoryService: new BamFileAnalysisServiceFactoryService(
                        indelCallingService: indelService
                )
        )
        UnwithdrawService service = new UnwithdrawService([
                fileSystemService      : fileSystemService,
                withdrawAnalysisService: withdrawAnalysisService,
        ])

        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(
                processingState: AnalysisProcessingStates.FINISHED, withdrawn: true)

        Path workDir = indelService.getWorkDirectory(indelCallingInstance)
        CreateFileHelper.createFile(workDir)

        List<AbstractMergedBamFile> bamFiles = [indelCallingInstance.sampleType1BamFile, indelCallingInstance.sampleType2BamFile]
        state.mergedBamFiles = bamFiles

        when:
        service.unwithdrawAnalysis(state)

        then:
        state.linksToCreate == [:]
        state.pathsToChangeGroup == [(workDir.toString()): indelCallingInstance.project.unixGroup]
        state.mergedBamFiles == bamFiles
        [indelCallingInstance].every { !it.withdrawn }
    }
}
