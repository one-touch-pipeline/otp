package de.dkfz.tbi.otp.dataprocessing

import asset.pipeline.grails.*
import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*
import org.codehaus.groovy.grails.context.support.*
import org.junit.*
import org.junit.rules.*
import org.springframework.beans.factory.annotation.*
import spock.lang.*

class LinkFilesToFinalDestinationServiceIntegrationSpec extends IntegrationSpec {
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
        service.processingOptionService = new ProcessingOptionService()
        service.createNotificationTextService = new CreateNotificationTextService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    _ * getMessageInternal('notification.template.alignment.qcTrafficBlockedSubject', [], _) >> '''QC traffic alignment header ${roddyBamFile.sample} ${roddyBamFile.seqType}'''
                    _ * getMessageInternal('notification.template.alignment.qcTrafficBlockedMessage', [], _) >> '''\
QC traffic alignment body
${roddyBamFile.sample} ${roddyBamFile.seqType} in project ${roddyBamFile.project}
${link}
'''
                }
        )

        roddyBamFile = DomainFactory.createRoddyBamFile([:], RnaRoddyBamFile)

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

        RnaRoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile([workPackage: roddyBamFile.workPackage, config: roddyBamFile.config], RnaRoddyBamFile)
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

    void "test createResultsAreBlockedSubject when mailing list exists"() {
        given:
        roddyBamFile.project.mailingListName = "tr_test@MailingList"
        assert roddyBamFile.project.save(flush: true)

        when:
        String result = service.createResultsAreBlockedSubject(roddyBamFile)

        then:
        result == "QC traffic alignment header ${roddyBamFile.sample} ${roddyBamFile.seqType}"
    }

    void "test createResultsAreBlockedSubject when no mailing list exists"() {
        given:
        roddyBamFile.project.mailingListName = null
        assert roddyBamFile.project.save(flush: true)

        when:
        String result = service.createResultsAreBlockedSubject(roddyBamFile)

        then:
        result == "TO BE SENT: QC traffic alignment header ${roddyBamFile.sample} ${roddyBamFile.seqType}"
    }

    void "test createResultsAreBlockedMessage "() {
        given:
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        service.createNotificationTextService.linkGenerator = Mock(LinkGenerator) {
            1 * link(_) >> 'link'
        }
        String expected = """\
QC traffic alignment body
${roddyBamFile.sample} ${roddyBamFile.seqType} in project ${roddyBamFile.project}
link
"""

        when:
        String result = service.createResultsAreBlockedMessage(roddyBamFile)

        then:
        result == expected
    }

    @Unroll
    void "test informResultsAreBlocked (#name)"() {
        given:
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        service.createNotificationTextService.linkGenerator = Mock(LinkGenerator) {
            1 * link(_) >> 'link'
        }

        service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _) >> { String emailSubject, String content, List<String> recipients ->
                assert emailSubject == subjectHeader + 'HEADER'
                assert content == 'BODY'
                assert recipients
                assert recipients.size() == recipientsCount
                assert !recipients.contains(null)
            }
        }

        service.createNotificationTextService.messageSource = Mock(PluginAwareResourceBundleMessageSource) {
            1 * getMessageInternal('notification.template.alignment.qcTrafficBlockedSubject', [], _) >> '''HEADER'''
            1 * getMessageInternal('notification.template.alignment.qcTrafficBlockedMessage', [], _) >> '''BODY'''
            0 * _
        }
        DomainFactory.createProcessingOptionForNotificationRecipient()

        roddyBamFile.project.mailingListName = projectMailingList
        roddyBamFile.project.save()

        expect:
        service.informResultsAreBlocked(roddyBamFile)

        where:
        name                   | projectMailingList              | subjectHeader  || recipientsCount
        'without mailing list' | null                            | 'TO BE SENT: ' || 1
        'with mailing list'    | "tr_${HelperUtils.randomEmail}" | ''             || 2
    }


    private RnaRoddyBamFile createRnaRoddyBamFile(Map map = [:]) {
        return DomainFactory.createRnaRoddyBamFile([
                md5sum             : null,
                fileSize           : -1,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
        ] + map)
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
        roddyBamFile = createRnaRoddyBamFile()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        LinkFilesToFinalDestinationService linkFilesToFinalDestinationService = Spy() {
            1 * cleanupOldRnaResults(_, _) >> { RoddyBamFile roddyBamFile, Realm realm -> }
            0 * informResultsAreBlocked(_) >> { RoddyBamFile roddyBamFile -> }
            0 * cleanupWorkDirectory(_, _)
        }
        linkFilesToFinalDestinationService.executeRoddyCommandService = Mock(ExecuteRoddyCommandService) {
            1 * correctPermissionsAndGroups(_, _) >> { RoddyResult roddyResult, Realm realm -> }
        }
        linkFilesToFinalDestinationService.linkFileUtils = Mock(LinkFileUtils) {
            1 * createAndValidateLinks(_, _) >> { Map<File, File> sourceLinkMap, Realm realm -> }
        }
        linkFilesToFinalDestinationService.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            1 * setSamplePairStatusToNeedProcessing(_) >> { RoddyBamFile roddyBamFile-> }
        }

        when:
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanupRna(roddyBamFile, realm)

        then:
        assertBamFileIsFine()
    }

    void "linkToFinalDestinationAndCleanupRna, when qcTrafficLightStatus is #BLOCKED"() {
        given:
        roddyBamFile = createRnaRoddyBamFile([
                qcTrafficLightStatus: AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED,
        ])
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        LinkFilesToFinalDestinationService linkFilesToFinalDestinationService = Spy() {
            1 * cleanupOldRnaResults(_, _) >> { RoddyBamFile roddyBamFile, Realm realm -> }
            0 * cleanupWorkDirectory(_, _)
            1 * informResultsAreBlocked(_) >> { RoddyBamFile roddyBamFile -> }
        }
        linkFilesToFinalDestinationService.executeRoddyCommandService = Mock(ExecuteRoddyCommandService) {
            1 * correctPermissionsAndGroups(_, _) >> { RoddyResult roddyResult, Realm realm -> }
        }
        linkFilesToFinalDestinationService.linkFileUtils = Mock(LinkFileUtils) {
            0 * createAndValidateLinks(_, _) >> { Map<File, File> sourceLinkMap, Realm realm -> }
        }
        linkFilesToFinalDestinationService.abstractMergedBamFileService = Mock(AbstractMergedBamFileService) {
            1 * setSamplePairStatusToNeedProcessing(_) >> { RoddyBamFile roddyBamFile-> }
        }

        when:
        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanupRna(roddyBamFile, realm)

        then:
        assertBamFileIsFine()
    }
}
