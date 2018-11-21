package de.dkfz.tbi.otp.dataprocessing


import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*
import org.codehaus.groovy.grails.context.support.*
import org.junit.*
import org.junit.rules.*
import org.springframework.beans.factory.annotation.*

class LinkFilesToFinalDestinationServiceIntegrationSpec extends IntegrationSpec implements RoddyRnaFactory {
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
        linkFilesToFinalDestinationService.qcTrafficLightNotificationService = Mock(QcTrafficLightNotificationService) {
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
        linkFilesToFinalDestinationService.qcTrafficLightNotificationService = Mock(QcTrafficLightNotificationService) {
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
