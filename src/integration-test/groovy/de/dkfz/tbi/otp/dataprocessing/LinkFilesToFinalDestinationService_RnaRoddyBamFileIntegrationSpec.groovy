/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightCheckService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightNotificationService
import de.dkfz.tbi.otp.utils.*

import java.nio.file.FileSystems
import java.nio.file.Path

@Rollback
@Integration
class LinkFilesToFinalDestinationService_RnaRoddyBamFileIntegrationSpec extends Specification implements RoddyRnaFactory {

    LinkFilesToFinalDestinationService service

    @Autowired
    RemoteShellHelper remoteShellHelper

    @TempDir
    Path tempDir
    RnaRoddyBamFile roddyBamFile
    TestConfigService configService

    void setupData() {
        service = new LinkFilesToFinalDestinationService()
        service.remoteShellHelper = remoteShellHelper
        service.linkFileUtils = new LinkFileUtils()
        service.lsdfFilesService = new LsdfFilesService()
        service.lsdfFilesService.createClusterScriptService = new CreateClusterScriptService()
        service.lsdfFilesService.remoteShellHelper = remoteShellHelper
        service.linkFileUtils.fileService = new FileService()
        service.linkFileUtils.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem() >> FileSystems.default
            0 * _
        }
        service.executeRoddyCommandService = new ExecuteRoddyCommandService()
        service.executeRoddyCommandService.remoteShellHelper = service.remoteShellHelper

        roddyBamFile = createBamFile()

        configService.addOtpProperties(tempDir)
    }

    void cleanup() {
        configService.clean()
    }

    void "test linkNewRnaResults"() {
        given:
        setupData()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        TestCase.withMockedremoteShellHelper(service.remoteShellHelper) {
            service.linkNewRnaResults(roddyBamFile)
        }

        then:
        [
                roddyBamFile.finalBamFile,
                roddyBamFile.finalBaiFile,
                roddyBamFile.finalMd5sumFile,
                roddyBamFile.finalExecutionStoreDirectory,
                roddyBamFile.finalQADirectory,
                new File(roddyBamFile.baseDirectory, "additionalArbitraryFile"),
        ].each {
            assert it.exists()
        }
    }

    void "test cleanupOldRnaResults"() {
        given:
        setupData()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        RnaRoddyBamFile roddyBamFile2 = createBamFile([workPackage: roddyBamFile.workPackage, config: roddyBamFile.config])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)
        TestCase.withMockedremoteShellHelper(service.remoteShellHelper) {
            service.linkNewRnaResults(roddyBamFile2)
        }
        assert roddyBamFile.workDirectory.exists()

        RemoteShellHelper remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String command ->
                return LocalShellHelper.executeAndWait(command).assertExitCodeZeroAndStderrEmpty()
            }
            executeCommand(_) >> { String command ->
                return LocalShellHelper.executeAndWait(command).assertExitCodeZeroAndStderrEmpty().stdout
            }
        }
        service.remoteShellHelper = remoteShellHelper
        service.lsdfFilesService.remoteShellHelper = remoteShellHelper

        when:
        service.cleanupOldRnaResults(roddyBamFile)

        then:
        !roddyBamFile2.workDirectory.exists()
        !roddyBamFile2.finalExecutionStoreDirectory.exists()
        !roddyBamFile2.finalQADirectory.exists()
        !roddyBamFile2.finalBamFile.exists()
        !roddyBamFile2.finalBaiFile.exists()
        !roddyBamFile2.finalMd5sumFile.exists()
        !new File(roddyBamFile.baseDirectory, "additionalArbitraryFile").exists()

        roddyBamFile.workDirectory.exists()
        !roddyBamFile.finalExecutionStoreDirectory.exists()
        !roddyBamFile.finalQADirectory.exists()
        !roddyBamFile.finalBamFile.exists()
        !roddyBamFile.finalBaiFile.exists()
        !roddyBamFile.finalMd5sumFile.exists()
    }

    void "linkToFinalDestinationAndCleanupRna, when qcTrafficLightStatus is #QC_PASSED"() {
        given:
        setupData()
        roddyBamFile = createBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.NEEDS_PROCESSING,
                qcTrafficLightStatus: AbstractBamFile.QcTrafficLightStatus.QC_PASSED,
        ])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        LinkFilesToFinalDestinationService linkFilesToFinalDestinationService = Spy {
            1 * cleanupOldRnaResults(_) >> { RoddyBamFile roddyBamFile -> }
        }
        linkFilesToFinalDestinationService.md5SumService = new Md5SumService()
        linkFilesToFinalDestinationService.md5SumService.fileService = new FileService()
        linkFilesToFinalDestinationService.md5SumService.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        linkFilesToFinalDestinationService.qcTrafficLightCheckService = new QcTrafficLightCheckService()
        linkFilesToFinalDestinationService.qcTrafficLightCheckService.qcTrafficLightNotificationService = Mock(QcTrafficLightNotificationService) {
            0 * informResultsAreWarned(_) >> { AbstractBamFile bamFile -> }
        }
        linkFilesToFinalDestinationService.executeRoddyCommandService = Mock(ExecuteRoddyCommandService) {
            1 * correctPermissionsAndGroups(_) >> { RoddyResult roddyResult -> }
        }
        linkFilesToFinalDestinationService.linkFileUtils = Mock(LinkFileUtils) {
            1 * createAndValidateLinks(_, _) >> { Map<File, File> sourceLinkMap, String unixGroup -> }
        }
        linkFilesToFinalDestinationService.abstractBamFileService = Mock(AbstractBamFileService) {
            1 * updateSamplePairStatusToNeedProcessing(_) >> { RoddyBamFile roddyBamFile -> }
        }

        when:
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanupRna(roddyBamFile)

        then:
        roddyBamFile.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum == DomainFactory.DEFAULT_MD5_SUM
        roddyBamFile.fileSize > 0
        roddyBamFile.dateFromFileSystem instanceof Date
    }

    void testValidateAndSetBamFileInProjectFolder_WhenBamFileFileOperationStatusNotInProgress_ShouldFail() {
        given:
        AbstractBamFile bamFile = createBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.DECLARED,
        ])

        when:
        new LinkFilesToFinalDestinationService().validateAndSetBamFileInProjectFolder(bamFile)

        then:
        thrown(AssertionError)
    }

    void testValidateAndSetBamFileInProjectFolder_WhenBamFileWithdrawn_ShouldFail() {
        given:
        AbstractBamFile bamFile = createBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
                withdrawn: true,
        ])

        when:
        new LinkFilesToFinalDestinationService().validateAndSetBamFileInProjectFolder(bamFile)

        then:
        thrown(AssertionError)
    }

    void testValidateAndSetBamFileInProjectFolder_WhenOtherNonWithdrawnBamFilesWithFileOperationStatusInProgressOfSameMergingWorkPackageExist_ShouldFail() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createPanCanPipeline())

        AbstractBamFile bamFile = createRoddyBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
        ], mergingWorkPackage, RoddyBamFile)

        createRoddyBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
        ], mergingWorkPackage, RoddyBamFile)

        when:
        new LinkFilesToFinalDestinationService().validateAndSetBamFileInProjectFolder(bamFile)

        then:
        thrown(AssertionError)
    }

    void testValidateAndSetBamFileInProjectFolder_WhenAllFine_ShouldSetBamFileInProjectFolder() {
        given:
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createPanCanPipeline())

        createRoddyBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.DECLARED,
        ], mergingWorkPackage, RoddyBamFile)

        createRoddyBamFile([
                withdrawn         : true,
        ], mergingWorkPackage, RoddyBamFile)

        createBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
        ])

        AbstractBamFile bamFile = createRoddyBamFile([
                fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
        ], mergingWorkPackage, RoddyBamFile)

        when:
        new LinkFilesToFinalDestinationService().validateAndSetBamFileInProjectFolder(bamFile)

        then:
        bamFile.mergingWorkPackage.bamFileInProjectFolder == bamFile
    }
}
