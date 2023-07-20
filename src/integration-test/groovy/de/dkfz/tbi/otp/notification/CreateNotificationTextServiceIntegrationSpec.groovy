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
package de.dkfz.tbi.otp.notification

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import grails.web.mapping.LinkGenerator
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.tracking.ProcessingStatus
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MessageSourceService

import static de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep.*

@Rollback
@Integration
class CreateNotificationTextServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    static final String CONTROLLER = 'controller'
    static final String ACTION = 'action'
    static final String NOTE = "Testnote\nwith wordwrap"

    @SuppressWarnings('GStringExpressionWithinString')
    static final String NOTIFICATION_MESSAGE = '''
base notification
stepInformation:${stepInformation}
seqCenterComment:${seqCenterComment}
faq:${faq}
'''

    CreateNotificationTextService createNotificationTextService
    OtrsTicketService otrsTicketService

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
                createProject(name: 'project3'),
                createProject(name: 'project5'),
                createProject(name: 'project2'),
        ]

        String expected = [
                "${linkGenerator.serverBaseURL}/${CONTROLLER}/${ACTION}?${ProjectSelectionService.PROJECT_SELECTION_PARAMETER}=project2",
                "${linkGenerator.serverBaseURL}/${CONTROLLER}/${ACTION}?${ProjectSelectionService.PROJECT_SELECTION_PARAMETER}=project3",
                "${linkGenerator.serverBaseURL}/${CONTROLLER}/${ACTION}?${ProjectSelectionService.PROJECT_SELECTION_PARAMETER}=project5",
        ].join('\n')

        expect:
        expected == createNotificationTextService.createOtpLinks(projects, CONTROLLER, ACTION)
    }

    void "test: notification if faq link is set"() {
        given:
        Project project = createProject()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        OtrsTicket ticket = createOtrsTicket()
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK, "some_link")
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.EMAIL_REPLY_TO, "a.b@c.de")
        ProcessingStatus processingStatus = new ProcessingStatus()

        CreateNotificationTextService createNotificationTextService = Spy(CreateNotificationTextService) {
            1 * alignmentNotification(_) >> "something"
        }
        createNotificationTextService.processingOptionService = new ProcessingOptionService()
        createNotificationTextService.otrsTicketService = new OtrsTicketService()

        createNotificationTextService.messageSourceService = new MessageSourceService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    1 * getMessageInternal("notification.template.base.faq", [], _) >> "FAQs"
                    _ * getMessageInternal("notification.template.base", [], _) >> NOTIFICATION_MESSAGE
                }
        )

        when:
        String message = createNotificationTextService.notification(ticket, processingStatus, ALIGNMENT, project)

        then:
        message.contains("FAQs")
    }

    void "test: notification if faq link is not set"() {
        given:
        Project project = createProject()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        OtrsTicket ticket = createOtrsTicket()
        ProcessingStatus processingStatus = new ProcessingStatus()

        CreateNotificationTextService createNotificationTextService = Spy(CreateNotificationTextService) {
            1 * alignmentNotification(processingStatus) >> "something"
        }
        createNotificationTextService.processingOptionService = new ProcessingOptionService()
        createNotificationTextService.otrsTicketService = new OtrsTicketService()

        createNotificationTextService.messageSourceService = new MessageSourceService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    0 * getMessageInternal("notification.template.base.faq", [], _) >> "FAQs"
                    _ * getMessageInternal("notification.template.base", [], _) >> NOTIFICATION_MESSAGE
                }
        )

        when:
        String message = createNotificationTextService.notification(ticket, processingStatus, ALIGNMENT, project)

        then:
        !message.contains("FAQs")
    }

    @Unroll
    void "notification, return message (#processingStep, otrs comment: #otrsTicketSeqCenterComment, default: #generalSeqCenterComment)"() {
        given:
        Project project = createProject()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        OtrsTicket ticket = createOtrsTicket(
                seqCenterComment: otrsTicketSeqCenterComment,
        )
        createFastqFile(
                fastqImportInstance: createFastqImportInstance(
                        otrsTicket: ticket,
                ),
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                type: CollectionUtils.exactlyOneElement(otrsTicketService.findAllSeqTracks(ticket)*.seqCenter.unique()).name,
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
        createNotificationTextService.messageSourceService = new MessageSourceService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    _ * getMessageInternal("notification.template.seqCenterNote.${CollectionUtils.exactlyOneElement(otrsTicketService.findAllSeqTracks(ticket)*.seqCenter.unique()).name.toLowerCase()}", [], _) >> generalSeqCenterComment
                    _ * getMessageInternal("notification.template.base", [], _) >> NOTIFICATION_MESSAGE
                }
        )
        createNotificationTextService.processingOptionService = new ProcessingOptionService()
        createNotificationTextService.otrsTicketService = new OtrsTicketService()

        String expectedSeqCenterComment = ""

        if (otrsTicketSeqCenterComment || generalSeqCenterComment) {
            if (otrsTicketSeqCenterComment?.contains(generalSeqCenterComment)) {
                expectedSeqCenterComment = """\
******************************
Note from sequencing center:
${otrsTicketSeqCenterComment}
******************************"""
            } else {
                expectedSeqCenterComment = """\
******************************
Note from sequencing center:
${otrsTicketSeqCenterComment}${otrsTicketSeqCenterComment ? "\n" : ""}${generalSeqCenterComment}
******************************"""
            }
        }

        String expected = """
base notification
stepInformation:${processingStep}
seqCenterComment:${expectedSeqCenterComment}
faq:
"""

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
        Project project = createProject()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()
        OtrsTicket ticket = createOtrsTicket(
                seqCenterComment: otrsTicketSeqCenterComment,
        )
        RawSequenceFile rawSequenceFile1 = createFastqFile(
                fastqImportInstance: createFastqImportInstance(
                        otrsTicket: ticket,
                ),
        )
        RawSequenceFile rawSequenceFile2 = createFastqFile(
                fastqImportInstance: createFastqImportInstance(
                        otrsTicket: ticket,
                ),
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                type: rawSequenceFile1.run.seqCenter.name,
                value: seqCenterMessage1
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                type: rawSequenceFile2.run.seqCenter.name,
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
        createNotificationTextService.messageSourceService = new MessageSourceService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    _ * getMessageInternal("notification.template.base", [], _) >> NOTIFICATION_MESSAGE
                }
        )
        createNotificationTextService.processingOptionService = new ProcessingOptionService()
        createNotificationTextService.otrsTicketService = new OtrsTicketService()

        String expectedSeqCenterComment

        if (otrsTicketSeqCenterComment) {
            expectedSeqCenterComment = """\
******************************
Note from sequencing center:
${otrsTicketSeqCenterComment}
******************************"""
        } else {
            expectedSeqCenterComment = ""
        }

        String expected = """
base notification
stepInformation:${processingStep}
seqCenterComment:${expectedSeqCenterComment}
faq:
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

    void "test LinkGenerator.link() method, when input valid, return well formed otp URL"() {
        given:
        Map linkProperties = [
                (LinkGenerator.ATTRIBUTE_CONTROLLER): "metadataImport",
                (LinkGenerator.ATTRIBUTE_ACTION)    : "details",
                (LinkGenerator.ATTRIBUTE_ID)        : 12345,
        ]

        String baseUrl = createNotificationTextService.linkGenerator.serverBaseURL
        String expected = "${baseUrl}/${linkProperties.values().join('/')}"

        linkProperties.put(LinkGenerator.ATTRIBUTE_ABSOLUTE, true)

        expect:
        expected == createNotificationTextService.linkGenerator.link(linkProperties)
    }
}
