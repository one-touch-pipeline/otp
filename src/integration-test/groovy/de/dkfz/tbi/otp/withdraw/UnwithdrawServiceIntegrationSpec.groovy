/*
 * Copyright 2011-2025 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqLinkFileService
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqWorkFileService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelLinkFileService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaLinkFileService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaWorkFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvLinkFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvWorkFileService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaLinkFileService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.infrastructure.FileService
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

    @TempDir
    Path tempDir

    TestConfigService configService

    void setup() {
        configService.addOtpProperties(tempDir)
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

        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(
                processingState: AnalysisProcessingStates.FINISHED, withdrawn: true)

        Path linkDir = indelService.getWorkDirectory(indelCallingInstance)
        CreateFileHelper.createFile(linkDir)

        // Create properly configured AnalysisLinkFileServiceFactoryService with all required services
        AnalysisLinkFileServiceFactoryService analysisLinkFileServiceFactory = new AnalysisLinkFileServiceFactoryService(
                aceseqLinkFileService: Mock(AceseqLinkFileService) {
                    getDirectoryPath(_) >> linkDir
                },
                indelLinkFileService: Mock(IndelLinkFileService) {
                    getDirectoryPath(_) >> linkDir
                },
                runYapsaLinkFileService: Mock(RunYapsaLinkFileService) {
                    getDirectoryPath(_) >> linkDir
                },
                snvLinkFileService: Mock(SnvLinkFileService) {
                    getDirectoryPath(_) >> linkDir
                },
                sophiaLinkFileService: Mock(SophiaLinkFileService) {
                    getDirectoryPath(_) >> linkDir
                },
        )

        // Create properly configured AnalysisWorkFileServiceFactoryService with all required services
        AnalysisWorkFileServiceFactoryService analysisWorkFileServiceFactory = new AnalysisWorkFileServiceFactoryService(
                aceseqWorkFileService: Mock(AceseqWorkFileService) {
                    getDirectoryPath(_) >> tempDir
                },
                indelWorkFileService: Mock(IndelWorkFileService) {
                    getDirectoryPath(_) >> tempDir
                },
                runYapsaWorkFileService: Mock(RunYapsaWorkFileService) {
                    getDirectoryPath(_) >> tempDir
                },
                snvWorkFileService: Mock(SnvWorkFileService) {
                    getDirectoryPath(_) >> tempDir
                },
                sophiaWorkFileService: Mock(SophiaWorkFileService) {
                    getDirectoryPath(_) >> tempDir
                },
        )

        WithdrawAnalysisService withdrawAnalysisService = new WithdrawAnalysisService(
                analysisLinkFileServiceFactoryService: analysisLinkFileServiceFactory,
                analysisWorkFileServiceFactoryService: analysisWorkFileServiceFactory
        )
        UnwithdrawService service = new UnwithdrawService([
                fileSystemService      : fileSystemService,
                withdrawAnalysisService: withdrawAnalysisService,
        ])

        List<AbstractBamFile> bamFiles = [indelCallingInstance.sampleType1BamFile, indelCallingInstance.sampleType2BamFile]
        state.bamFiles = bamFiles

        service.withdrawAnalysisService.fileService = Mock(FileService) {
            fileExists(_) >> true
        }

        when:
        service.unwithdrawAnalysis(state)

        then:
        state.linksToCreate == [:]
        state.pathsToChangeGroup == [(linkDir.toString()): indelCallingInstance.project.unixGroup]
        state.bamFiles == bamFiles
        [indelCallingInstance].every { !it.withdrawn }
    }
}
