package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.web.mapping.*
import org.springframework.beans.factory.annotation.*
import spock.lang.*

import static de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class CreateNotificationTextServiceIntegrationSpec extends IntegrationSpec {

    static final String CONTROLLER = 'controller'
    static final String ACTION = 'action'
    static final String NOTE = "Testnote\nwith wordwrap"


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
    void "notification, return message"() {
        given:
        DomainFactory.createNotificationProcessingOptions()
        Project project = DomainFactory.createProject()
        OtrsTicket ticket = DomainFactory.createOtrsTicket(
                seqCenterComment: otrsTicketSeqCenterComment,
        )
        DomainFactory.createDataFile(
                runSegment: DomainFactory.createRunSegment(
                        otrsTicket: ticket,
                ),
        )
        ProcessingStatus processingStatus = new ProcessingStatus()
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                type: exactlyOneElement(ticket.findAllSeqTracks()*.seqCenter.unique()).name,
                value: generalSeqCenterComment
        )

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
        }

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
stepInformation: ${processingStep.toString()}
seqCenterComment: ${expectedSeqCenterComment}
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
        INSTALLATION   | 'Some comment'             | ''
        ALIGNMENT      | 'Some comment'             | ''
        SNV            | 'Some comment'             | ''
        INDEL          | 'Some comment'             | ''
        SOPHIA         | 'Some comment'             | ''
        ACESEQ         | 'Some comment'             | ''
        INSTALLATION   | ''                         | 'Some general comment'
        INSTALLATION   | 'Some otrs comment'        | 'Some general comment'
        INSTALLATION   | NOTE                       | NOTE
    }
}
