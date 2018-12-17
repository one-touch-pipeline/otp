package de.dkfz.tbi.otp.notification

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.ProcessingStatus
import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep.*

class CreateNotificationTextServiceIntegrationSpec extends IntegrationSpec {

    static final String CONTROLLER = 'controller'
    static final String ACTION = 'action'
    static final String NOTE = "Testnote\nwith wordwrap"
    static final String NOTIFICATION_MESSAGE = '''
base notification
stepInformation:${stepInformation}
seqCenterComment:${seqCenterComment}
addition:${addition}
phabricatorAlias:${phabricatorAlias}
'''


    CreateNotificationTextService createNotificationTextService

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    LinkGenerator linkGenerator

    void "createOtpLinks, when input invalid, should throw assert"() {
        when:
        createNotificationTextService.createOtpLinks(projects, controller, action)

        then:
        AssertionError e = thrown()
        e.message.contains('assert ' + errorMessage)

        where:
        projects        | controller | action || errorMessage
        null            | CONTROLLER | ACTION || 'projects'
        []              | CONTROLLER | ACTION || 'projects'
        [new Project()] | null       | ACTION || CONTROLLER
        [new Project()] | CONTROLLER | null   || ACTION
    }


    void "createOtpLinks, when input valid, return sorted URLs of the projects"() {
        given:
        List<Project> projects = [
                DomainFactory.createProject(name: 'project3'),
                DomainFactory.createProject(name: 'project5'),
                DomainFactory.createProject(name: 'project2'),
        ]

        String expected = [
                "${linkGenerator.getServerBaseURL()}/${CONTROLLER}/${ACTION}?project=project2",
                "${linkGenerator.getServerBaseURL()}/${CONTROLLER}/${ACTION}?project=project3",
                "${linkGenerator.getServerBaseURL()}/${CONTROLLER}/${ACTION}?project=project5",
        ].join('\n')

        expect:
        expected == createNotificationTextService.createOtpLinks(projects, CONTROLLER, ACTION)
    }

    @Unroll
    void "notification, return message (#processingStep, otrs comment: #otrsTicketSeqCenterComment, default: #generalSeqCenterComment)"() {
        given:
        Project project = DomainFactory.createProject()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        OtrsTicket ticket = DomainFactory.createOtrsTicket(
                seqCenterComment: otrsTicketSeqCenterComment,
        )
        DomainFactory.createDataFile(
                runSegment: DomainFactory.createRunSegment(
                        otrsTicket: ticket,
                ),
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                type: CollectionUtils.exactlyOneElement(ticket.findAllSeqTracks()*.seqCenter.unique()).name,
                value: generalSeqCenterComment
        )

        ProcessingStatus processingStatus = new ProcessingStatus()

        CreateNotificationTextService createNotificationTextService = Spy(CreateNotificationTextService) {
            (processingStep == INSTALLATION ? 1 : 0) *
                    installationNotification(processingStatus) >> INSTALLATION.toString()
            (processingStep == ALIGNMENT ? 1 : 0) *
                    alignmentNotification(processingStatus) >> ALIGNMENT.toString()
            (processingStep == SNV ? 1 : 0) *
                    snvNotification(processingStatus) >> SNV.toString()
            (processingStep == INDEL ? 1 : 0) *
                    indelNotification(processingStatus) >> INDEL.toString()
            (processingStep == SOPHIA ? 1 : 0) *
                    sophiaNotification(processingStatus) >> SOPHIA.toString()
            (processingStep == ACESEQ ? 1 : 0) *
                    aceseqNotification(processingStatus) >> ACESEQ.toString()
            (processingStep == RUN_YAPSA ? 1 : 0) *
                    runYapsaNotification(processingStatus) >> RUN_YAPSA.toString()
        }
        createNotificationTextService.messageSource = Mock(PluginAwareResourceBundleMessageSource) {
            _ * getMessageInternal("notification.template.seqCenterNote.${CollectionUtils.exactlyOneElement(ticket.findAllSeqTracks()*.seqCenter.unique()).name.toLowerCase()}", [], _) >> generalSeqCenterComment
            _ * getMessageInternal("notification.template.base", [], _) >> NOTIFICATION_MESSAGE
        }
        createNotificationTextService.processingOptionService = new ProcessingOptionService()

        String expectedSeqCenterComment = ""

        if (otrsTicketSeqCenterComment || generalSeqCenterComment) {
            if (otrsTicketSeqCenterComment?.contains(generalSeqCenterComment)) {
                expectedSeqCenterComment = """

******************************
Note from sequencing center:
${otrsTicketSeqCenterComment}
******************************"""
            } else {
                expectedSeqCenterComment = """

******************************
Note from sequencing center:
${otrsTicketSeqCenterComment}${otrsTicketSeqCenterComment ? "\n" : ""}${generalSeqCenterComment}
******************************"""
            }
        }

        String expected = """
base notification
stepInformation:${processingStep.toString()}
seqCenterComment:${expectedSeqCenterComment}
addition:
phabricatorAlias:
"""
        if (processingStep == INSTALLATION) {
            expected = expected + "!project #\$${project.phabricatorAlias}\n"
        }

        when:
        String message = createNotificationTextService.notification(ticket, processingStatus, processingStep, project)

        then:
        expected == message

        where:
        processingStep | otrsTicketSeqCenterComment | generalSeqCenterComment
        INSTALLATION   | null                       | ''
        ALIGNMENT      | null                       | ''
        SNV            | null                       | ''
        INDEL          | null                       | ''
        RUN_YAPSA      | null                       | ''
        INSTALLATION   | 'Some comment'             | ''
        ALIGNMENT      | 'Some comment'             | ''
        SNV            | 'Some comment'             | ''
        INDEL          | 'Some comment'             | ''
        SOPHIA         | 'Some comment'             | ''
        ACESEQ         | 'Some comment'             | ''
        RUN_YAPSA      | 'Some comment'             | ''
        INSTALLATION   | ''                         | 'Some general comment'
        INSTALLATION   | 'Some otrs comment'        | 'Some general comment'
        INSTALLATION   | NOTE                       | NOTE
    }

    void "notification, when ticket has more than one seq center, ignore seq center default message"() {
        given:
        String seqCenterMessage1 = "Message of seq center 1"
        String seqCenterMessage2 = "Message of seq center 2"
        Project project = DomainFactory.createProject()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        OtrsTicket ticket = DomainFactory.createOtrsTicket(
                seqCenterComment: otrsTicketSeqCenterComment,
        )
        DataFile dataFile1 = DomainFactory.createDataFile(
                runSegment: DomainFactory.createRunSegment(
                        otrsTicket: ticket,
                ),
        )
        DataFile dataFile2 = DomainFactory.createDataFile(
                runSegment: DomainFactory.createRunSegment(
                        otrsTicket: ticket,
                ),
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                type: dataFile1.run.seqCenter.name,
                value: seqCenterMessage1
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                type: dataFile2.run.seqCenter.name,
                value: seqCenterMessage2
        )
        ProcessingStatus processingStatus = new ProcessingStatus()

        CreateNotificationTextService createNotificationTextService = Spy(CreateNotificationTextService) {
            0 * installationNotification(processingStatus) >> INSTALLATION.toString()
            1 * alignmentNotification(processingStatus) >> ALIGNMENT.toString()
            0 * snvNotification(processingStatus) >> SNV.toString()
            0 * indelNotification(processingStatus) >> INDEL.toString()
            0 * sophiaNotification(processingStatus) >> SOPHIA.toString()
            0 * aceseqNotification(processingStatus) >> ACESEQ.toString()
        }
        createNotificationTextService.messageSource = Mock(PluginAwareResourceBundleMessageSource) {
            _ * getMessageInternal("notification.template.base", [], _) >> NOTIFICATION_MESSAGE
        }
        createNotificationTextService.processingOptionService = new ProcessingOptionService()

        String expectedSeqCenterComment

        if (otrsTicketSeqCenterComment) {
            expectedSeqCenterComment = """

******************************
Note from sequencing center:
${otrsTicketSeqCenterComment}
******************************"""
        } else {
            expectedSeqCenterComment = ""
        }


        String expected = """
base notification
stepInformation:${processingStep.toString()}
seqCenterComment:${expectedSeqCenterComment}
addition:
phabricatorAlias:
"""

        when:
        String message = createNotificationTextService.notification(ticket, processingStatus, processingStep, project)

        then:
        expected == message

        where:
        processingStep | otrsTicketSeqCenterComment
        ALIGNMENT      | ''
        ALIGNMENT      | 'Some comment'
    }
}
