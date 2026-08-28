/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.notification

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

@Transactional
class NotificationMessageService {

    private static final int MAX_ILSE_NUMBERS = 5

    private static final Set<WorkflowRun.State> FINISHED_WORKFLOW_RUN_STATES = [
            WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
            WorkflowRun.State.SUCCESS,
            WorkflowRun.State.FAILED_FINAL,
    ] as Set

    private static final Set<WorkflowRun.State> DELIVERED_WORKFLOW_RUN_STATES = [WorkflowRun.State.SUCCESS] as Set

    private static final Set<WorkflowRun.State> PROBLEM_WORKFLOW_RUN_STATES = [
            WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
            WorkflowRun.State.FAILED_FINAL,
    ] as Set

    @Autowired
    LinkGenerator linkGenerator
    MessageSourceService messageSourceService
    ProcessingOptionService processingOptionService
    TicketService ticketService
    WorkflowNotificationContentService workflowNotificationContentService

    String createUnsupportedSubject() {
        return messageSourceService.createMessage('notification.template.unsupported.subject')
    }

    String createUnsupportedBody(Notification notification) {
        return messageSourceService.createMessage('notification.template.unsupported.body', [
                notificationId: notification.id,
                scope         : notification.notificationScope,
        ])
    }

    String createStatusSubject(Ticket ticket, boolean finalStatus) {
        return messageSourceService.createMessage(
                finalStatus ? 'notification.template.status.finalSubject' : 'notification.template.status.subject',
                [ticketNumber: ticketService.getPrefixedTicketNumber(ticket)],
        )
    }

    String createStatusBody(Ticket ticket, List<WorkflowRun> workflowRuns) {
        String workflowStatus = workflowRuns ?
                messageSourceService.createMessage('notification.template.status.overviewSection', [
                        ticketNumber: ticket.ticketNumber,
                        overview    : createStatusOverview(workflowRuns),
                ]).trim() :
                messageSourceService.createMessage('notification.template.status.noWorkflows')
        List<String> sections = [workflowStatus, createImportLinks(ticket)]
        return messageSourceService.createMessage('notification.template.status.body', [
                content: sections.findAll().join('\n\n\n'),
        ])
    }

    String createAllWorkflowsSubject(Ticket ticket, Notification notification) {
        return messageSourceService.createMessage('notification.template.allWorkflows.subject', [
                ticketNumber: ticketService.getPrefixedTicketNumber(ticket),
                toBeSent    : createToBeSentFlag([]),
                ilse        : createIlseBlock(ticket),
                project     : notification.project.name,
        ])
    }

    String createAllWorkflowsBody(List<String> workflowContents) {
        String divider = messageSourceService.createMessage('notification.template.allWorkflows.divider')
        return messageSourceService.createMessage('notification.template.allWorkflows.body', [
                content: workflowContents.join("\n\n${divider}\n\n"),
                faq    : createFaq(),
        ])
    }

    String createWorkflowSubject(Ticket ticket, Notification notification, Collection<String> recipients) {
        return messageSourceService.createMessage('notification.template.workflow.subject', [
                ticketNumber: ticketService.getPrefixedTicketNumber(ticket),
                toBeSent   : createToBeSentFlag(recipients),
                ilse       : createIlseBlock(ticket),
                project    : notification.project.name,
                workflow   : notification.workflow.displayName,
        ])
    }

    String createWorkflowBody(Notification notification, WorkflowNotification workflowNotification) {
        return messageSourceService.createMessage('notification.template.workflow.body', [
                content: createWorkflowContent(notification, workflowNotification),
                faq    : createFaq(),
        ])
    }

    String createWorkflowContent(Notification notification, WorkflowNotification workflowNotification) {
        Collection<WorkflowRun> workflowRuns = notification.workflowRuns ?: []
        Collection<WorkflowRun> deliveredRuns = workflowRuns.findAll { it.state in DELIVERED_WORKFLOW_RUN_STATES }
        Collection<WorkflowRun> problemRuns = workflowRuns.findAll { it.state in PROBLEM_WORKFLOW_RUN_STATES }
        Collection<WorkflowRun> unexpectedRuns = workflowRuns - deliveredRuns - problemRuns
        if (unexpectedRuns) {
            log.warn("Notification ${notification.id}: ignoring runs in an unexpected state: ${unexpectedRuns*.id}")
        }
        NotificationContent allRunsContent = workflowNotification.buildContent(workflowRuns)
        NotificationContent problemContent = workflowNotification.buildContent(problemRuns)
        List<String> sections = [
                messageSourceService.createMessage('notification.template.workflow.intro', [
                        workflow: notification.workflow.displayName,
                        project : notification.project.name,
                ]),
        ]

        addSection(sections, 'notification.template.workflow.processedData', allRunsContent.notificationTexts)
        addFailureSection(sections, 'notification.template.workflow.skipped', problemRuns, problemContent,
                WorkflowRun.State.SKIPPED_MISSING_PRECONDITION)
        addFailureSection(sections, 'notification.template.workflow.finalFailure', problemRuns, problemContent,
                WorkflowRun.State.FAILED_FINAL)
        addSection(sections, 'notification.template.workflow.guiUrls', allRunsContent.guiUrls)
        addSection(sections, 'notification.template.workflow.filePatterns', allRunsContent.filePatterns)
        addSection(sections, 'notification.template.workflow.configurations',
                (deliveredRuns + problemRuns)*.notificationText.findAll().unique())
        if (workflowNotification.pipelineAcknowledgementTemplate()) {
            sections << messageSourceService.createMessage(workflowNotification.pipelineAcknowledgementTemplate()).trim()
        }
        return sections.findAll().join('\n\n\n')
    }

    private void addSection(List<String> sections, String template, Collection<String> values) {
        List<String> entries = values.findAll().sort().collect { String value ->
            String indentedValue = value.replace('\n', '\n  ')
            messageSourceService.createMessage('notification.template.workflow.listEntry', [value: indentedValue])
        }
        if (entries) {
            sections << messageSourceService.createMessage(template, [entries: entries.join('\n')])
        }
    }

    private void addFailureSection(List<String> sections, String template, Collection<WorkflowRun> problemRuns,
            NotificationContent content, WorkflowRun.State state) {
        List<String> entries = problemRuns.findAll { WorkflowRun workflowRun ->
            workflowRun.state == state && content.notificationTextsByRunId[workflowRun.id]
        }.collect { WorkflowRun workflowRun ->
            String notificationText = content.notificationTextsByRunId[workflowRun.id]
            String reason = state == WorkflowRun.State.SKIPPED_MISSING_PRECONDITION ?
                    workflowRun.skipMessage.message : workflowRun.comment?.comment ?:
                    messageSourceService.createMessage('notification.template.workflow.failureReason.default')
            String indentedReason = reason.replace('\n', '\n  ')
            return [
                    messageSourceService.createMessage('notification.template.workflow.failureLabel',
                            [value: notificationText]),
                    messageSourceService.createMessage('notification.template.workflow.failureReason',
                            [reason: indentedReason]),
            ].join('\n')
        }.sort()
        if (entries) {
            sections << messageSourceService.createMessage(template, [entries: entries.join('\n')])
        }
    }

    /**
     * Creates the closing paragraph pointing to the FAQs and the support mail address.
     *
     * It is empty as long as no FAQ link is configured, since without the link the paragraph would be pointless.
     */
    private String createFaq() {
        String faqLink = processingOptionService.findOptionAsString(ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK)
        if (!faqLink) {
            return ''
        }
        return messageSourceService.createMessage('notification.template.base.faq', [
                faqLink    : faqLink,
                contactMail: processingOptionService.findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL),
        ])
    }

    /**
     * Creates the subject flag marking a mail without recipients, which therefore has to be sent manually.
     *
     * The separating space is part of the flag and not of the subject templates, since without a flag the templates
     * have to render the following block without a leading separator.
     */
    private String createToBeSentFlag(Collection<String> recipients) {
        return recipients ? '' : (messageSourceService.createMessage('notification.template.base.toBeSent') + ' ')
    }

    private String createIlseBlock(Ticket ticket) {
        List<Integer> ilseNumbers = workflowNotificationContentService.fetchIlseNumbers(ticket)
        if (!ilseNumbers) {
            return ''
        }
        String suffix = ilseNumbers.size() > MAX_ILSE_NUMBERS ? '...' : ''
        return "[S#${ilseNumbers.take(MAX_ILSE_NUMBERS).join(',')}${suffix}] "
    }

    private String createStatusOverview(List<WorkflowRun> workflowRuns) {
        Map<Workflow, List<WorkflowRun>> runsByWorkflow = workflowRuns.groupBy { WorkflowRun workflowRun ->
            workflowRun.workflow
        }
        List<Workflow> workflows = runsByWorkflow.keySet().toList().sort { Workflow workflow ->
            "${workflow.displayName}\u0000${workflow.name}"
        }
        int padding = workflows ? workflows.collect { it.displayName.length() }.max() + 1 : 0
        return workflows.collect { Workflow workflow ->
            List<WorkflowRun> runs = runsByWorkflow[workflow]
            String progress = messageSourceService.createMessage(runs.every { it.state in FINISHED_WORKFLOW_RUN_STATES } ?
                    'notification.template.status.allDone' : 'notification.template.status.inProgress')
            String stateCounts = runs.countBy {
                it.state
            }.sort {
                it.key.ordinal()
            }.collect { WorkflowRun.State state, int count ->
                messageSourceService.createMessage('notification.template.status.stateCount', [count: count, state: state])
            }.join(', ')
            messageSourceService.createMessage('notification.template.status.overview', [
                    workflow: "${workflow.displayName}:".padRight(padding),
                    progress: progress,
                    states  : stateCounts,
            ])
        }.join('\n')
    }

    private String createImportLinks(Ticket ticket) {
        List<String> links = ticketService.getAllFastqImportInstances(ticket).sort { FastqImportInstance fastqImport ->
            fastqImport.id
        }.collect { FastqImportInstance fastqImport ->
            linkGenerator.link(controller: 'metadataImport', action: 'details', id: fastqImport.id, absolute: true)
        }
        if (!links) {
            return ''
        }
        return messageSourceService.createMessage('notification.template.status.importLinks', [
                header: messageSourceService.createMessage('notification.import.detail.link'),
                links : links.collect {
                    messageSourceService.createMessage('notification.template.status.listEntry', [value: it])
                }.join('\n'),
        ])
    }
}
