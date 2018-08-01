package de.dkfz.tbi.otp.dataprocessing

import asset.pipeline.grails.LinkGenerator
import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*
import org.junit.*
import org.junit.rules.*
import org.codehaus.groovy.grails.context.support.*
import org.springframework.beans.factory.annotation.Autowired

class LinkFilesToFinalDestinationServiceIntegrationSpec extends IntegrationSpec {
    LinkFilesToFinalDestinationService service

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    RnaRoddyBamFile roddyBamFile
    Realm realm
    TestConfigService configService
    String fileName

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
        TestCase.withMockedremoteShellHelper(service.remoteShellHelper, {
            service.linkNewRnaResults(roddyBamFile, realm)
        })

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
        TestCase.withMockedremoteShellHelper(service.remoteShellHelper, {
            service.linkNewRnaResults(roddyBamFile2, realm)
        })
        assert roddyBamFile.workDirectory.exists()

        when:
        TestCase.withMockedremoteShellHelper(service.remoteShellHelper, {
            service.cleanupOldRnaResults(roddyBamFile, realm)
        })

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
            1* link(_) >> 'link'
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

    void "test informResultsAreBlocked"() {
        given:
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        service.createNotificationTextService.linkGenerator= Mock(LinkGenerator) {
            1 * link(_) >> 'link'
        }

        service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _) >> {String emailSubject, String content, List<String> recipients ->
                assert emailSubject == 'TO BE SENT: HEADER'
                assert content == 'BODY'
                assert recipients
            }
        }

        service.createNotificationTextService.messageSource = Mock(PluginAwareResourceBundleMessageSource) {
            1 * getMessageInternal('notification.template.alignment.qcTrafficBlockedSubject', [], _) >> '''HEADER'''
            1 * getMessageInternal('notification.template.alignment.qcTrafficBlockedMessage', [], _) >> '''BODY'''
            0 * _
        }
        DomainFactory.createProcessingOptionForNotificationRecipient()

        expect:
        service.informResultsAreBlocked(roddyBamFile)
    }
}
