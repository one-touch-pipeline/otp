package de.dkfz.tbi.otp.qcTrafficLight


import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        AbstractMergedBamFile,
        DataFile,
        Comment,
        ExomeSeqTrack,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProcessingOption,
        OtrsTicket,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RnaRoddyBamFile,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqType,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqCenter,
        SeqTrack,
        SoftwareTool,
])
class QcTrafficLightNotificationServiceSpec extends Specification {


    @Unroll
    void "test informResultsAreBlocked (#name)"() {
        given:
        final String HEADER = 'HEADER'
        final String BODY = 'BODY'
        final String LINK = 'LINK'

        AbstractMergedBamFile bamFile = DomainFactory.createRoddyBamFile([:])
        String emailSenderSalutation = DomainFactory.createProcessingOptionForEmailSenderSalutation().value
        DomainFactory.createProcessingOptionForNotificationRecipient()

        QcTrafficLightNotificationService service = new QcTrafficLightNotificationService([
                processingOptionService: new ProcessingOptionService(),
        ])

        service.createNotificationTextService = Mock(CreateNotificationTextService) {
            1 * createMessage('notification.template.alignment.qcTrafficBlockedSubject', _) >> { String templateName, Map properties ->
                assert properties.size() == 1
                assert properties['bamFile'] == bamFile
                return HEADER

            }
            1 * createMessage('notification.template.alignment.qcTrafficBlockedMessage', _) >> { String templateName, Map properties ->
                assert properties.size() == 3
                assert properties['bamFile'] == bamFile
                assert properties['emailSenderSalutation'] == emailSenderSalutation
                assert properties['link'] == LINK
                return BODY
            }
            1 * createOtpLinks([bamFile.project], 'alignmentQualityOverview', 'index') >> LINK
            0 * _
        }

        service.userProjectRoleService = Mock(UserProjectRoleService) {
            1 * getEmailsOfToBeNotifiedProjectUsers(_) >> emails
        }

        service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _) >> { String emailSubject, String content, List<String> recipients ->
                assert emailSubject == subjectHeader + HEADER
                assert content == BODY
                assert recipients
                assert recipients.size() == recipientsCount
                assert !recipients.contains(null)
            }
        }

        expect:
        service.informResultsAreBlocked(bamFile)

        where:
        name                      | emails    | subjectHeader  || recipientsCount
        'without userProjectRole' | []        | 'TO BE SENT: ' || 1
        'with userProjectRole'    | ['email'] | ''             || 2
    }
}
