/*
 * Copyright 2011-2019 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightCheckService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightNotificationService
import de.dkfz.tbi.otp.utils.*

@Rollback
@Integration
class LinkFilesToFinalDestinationServiceIntegrationSpec extends Specification implements RoddyRnaFactory {
    LinkFilesToFinalDestinationService service

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    RnaRoddyBamFile roddyBamFile
    Realm realm
    TestConfigService configService
    String fileName

    @Override
    void setup() {
        service = new LinkFilesToFinalDestinationService()
        service.remoteShellHelper = remoteShellHelper
        service.linkFileUtils = new LinkFileUtils()
        service.lsdfFilesService = new LsdfFilesService()
        service.lsdfFilesService.createClusterScriptService = new CreateClusterScriptService()
        service.lsdfFilesService.remoteShellHelper = remoteShellHelper
        service.linkFileUtils.createClusterScriptService = new CreateClusterScriptService()
        service.linkFileUtils.lsdfFilesService = service.lsdfFilesService
        service.linkFileUtils.remoteShellHelper = remoteShellHelper
        service.executeRoddyCommandService = new ExecuteRoddyCommandService()
        service.executeRoddyCommandService.remoteShellHelper = service.remoteShellHelper

        roddyBamFile = createBamFile()

        realm = roddyBamFile.project.realm
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
    }

    void cleanup() {
        configService.clean()
    }

    void "test linkNewRnaResults"() {
        given:
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        TestCase.withMockedremoteShellHelper(service.remoteShellHelper) {
            service.linkNewRnaResults(roddyBamFile, realm)
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
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        RnaRoddyBamFile roddyBamFile2 = createBamFile([workPackage: roddyBamFile.workPackage, config: roddyBamFile.config])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)
        TestCase.withMockedremoteShellHelper(service.remoteShellHelper) {
            service.linkNewRnaResults(roddyBamFile2, realm)
        }
        assert roddyBamFile.workDirectory.exists()

        when:
        TestCase.withMockedremoteShellHelper(service.remoteShellHelper) {
            service.cleanupOldRnaResults(roddyBamFile, realm)
        }

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




    private void assertBamFileIsFine() {
        assert roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        assert roddyBamFile.md5sum == DomainFactory.DEFAULT_MD5_SUM
        assert roddyBamFile.fileSize > 0
        assert roddyBamFile.fileExists
        assert roddyBamFile.dateFromFileSystem != null && roddyBamFile.dateFromFileSystem instanceof Date
    }


    void "linkToFinalDestinationAndCleanupRna, when qcTrafficLightStatus is #QC_PASSED"() {
        given:
        roddyBamFile = createBamFile([fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        LinkFilesToFinalDestinationService linkFilesToFinalDestinationService = Spy() {
            1 * cleanupOldRnaResults(_, _) >> { RoddyBamFile roddyBamFile, Realm realm -> }
            0 * cleanupWorkDirectory(_, _)
        }
        linkFilesToFinalDestinationService.md5SumService = new Md5SumService()
        linkFilesToFinalDestinationService.qcTrafficLightCheckService = new QcTrafficLightCheckService()
        linkFilesToFinalDestinationService.qcTrafficLightCheckService.qcTrafficLightNotificationService = Mock(QcTrafficLightNotificationService) {
            0 * informResultsAreBlocked(_) >> { AbstractMergedBamFile bamFile -> }
        }
        linkFilesToFinalDestinationService.executeRoddyCommandService = Mock(ExecuteRoddyCommandService) {
            1 * correctPermissionsAndGroups(_, _) >> { RoddyResult roddyResult, Realm realm -> }
        }
        linkFilesToFinalDestinationService.linkFileUtils = Mock(LinkFileUtils) {
            1 * createAndValidateLinks(_, _) >> { Map<File, File> sourceLinkMap, Realm realm -> }
        }
        linkFilesToFinalDestinationService.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            1 * setSamplePairStatusToNeedProcessing(_) >> { RoddyBamFile roddyBamFile -> }
        }

        when:
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanupRna(roddyBamFile, realm)

        then:
        assertBamFileIsFine()
    }

    void "linkToFinalDestinationAndCleanupRna, when qcTrafficLightStatus is #BLOCKED"() {
        given:
        roddyBamFile = createBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                qcTrafficLightStatus: AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED,
        ])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        LinkFilesToFinalDestinationService linkFilesToFinalDestinationService = Spy() {
            1 * cleanupOldRnaResults(_, _) >> { RoddyBamFile roddyBamFile, Realm realm -> }
            0 * cleanupWorkDirectory(_, _)
        }
        linkFilesToFinalDestinationService.md5SumService = new Md5SumService()
        linkFilesToFinalDestinationService.qcTrafficLightCheckService = new QcTrafficLightCheckService()
        linkFilesToFinalDestinationService.qcTrafficLightCheckService.qcTrafficLightNotificationService = Mock(QcTrafficLightNotificationService) {
            1 * informResultsAreBlocked(_) >> { AbstractMergedBamFile bamFile -> }
        }
        linkFilesToFinalDestinationService.executeRoddyCommandService = Mock(ExecuteRoddyCommandService) {
            1 * correctPermissionsAndGroups(_, _) >> { RoddyResult roddyResult, Realm realm -> }
        }
        linkFilesToFinalDestinationService.linkFileUtils = Mock(LinkFileUtils) {
            0 * createAndValidateLinks(_, _) >> { Map<File, File> sourceLinkMap, Realm realm -> }
        }
        linkFilesToFinalDestinationService.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            1 * setSamplePairStatusToNeedProcessing(_) >> { RoddyBamFile roddyBamFile -> }
        }

        when:
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanupRna(roddyBamFile, realm)

        then:
        assertBamFileIsFine()
    }
}
