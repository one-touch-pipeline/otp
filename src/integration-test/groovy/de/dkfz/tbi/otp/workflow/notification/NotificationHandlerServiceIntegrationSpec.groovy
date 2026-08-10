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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.administration.Mail
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.IlseSubmission
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationScope
import de.dkfz.tbi.otp.notification.NotificationState
import de.dkfz.tbi.otp.notification.NotificationStatus
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

@Rollback
@Integration
class NotificationHandlerServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    NotificationHandlerService notificationHandlerService

    void "createNotificationForTicketAndWorkflow, single-workflow creation resolves its ticket and ILSe graph and persists the queued mail"() {
        given:
        findOrCreateProcessingOption(ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX, 'OTP')
        findOrCreateProcessingOption(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM, 'ticket@example.com')
        findOrCreateProcessingOption(ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK, 'https://otp/faq')
        findOrCreateProcessingOption(ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL, 'support@example.com')
        Project project = createProject(processingNotification: true)
        Ticket ticket = createTicket(automaticNotification: false)
        IlseSubmission ilseSubmission = createIlseSubmission(ilseNumber: 12345)
        SeqTrack seqTrack = createSeqTrack(
                sample: createSample(individual: createIndividual(project: project)),
                ilseSubmission: ilseSubmission,
        )
        FastqFile fastqFile = createFastqFile(seqTrack: seqTrack)
        FastqImportInstance fastqImportInstance = createFastqImportInstance(ticket: ticket, sequenceFiles: [fastqFile])
        fastqFile.fastqImportInstance = fastqImportInstance
        fastqFile.save(flush: true)
        Workflow workflow = createWorkflow(name: DataInstallationWorkflow.WORKFLOW)
        WorkflowRun workflowRun = createWorkflowRun(
                workflow: workflow,
                project: project,
                state: WorkflowRun.State.SUCCESS,
        )
        Notification notification = new Notification(
                notificationState: NotificationState.READY,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: workflow,
                project: project,
        ).addToWorkflowRuns(workflowRun).save(flush: true)
        int mailCount = Mail.count()

        when:
        notificationHandlerService.createNotificationForTicketAndWorkflow(notification)

        then:
        Mail.count() == mailCount + 1
        Mail mail = Mail.findBySubjectLike("%${ticket.ticketNumber}%")
        mail.subject == "[OTP#${ticket.ticketNumber}] TO BE SENT: [S#12345] ${project.name} ${workflow.displayName} finished"
        mail.body.contains('If you have any further questions please refer to our FAQs https://otp/faq and do not hesitate ' +
                'to contact us if there are still open questions: support@example.com')
        mail.to as Set == ['ticket@example.com'] as Set
        notification.refresh().notificationState == NotificationState.CREATED
    }

    void "createNotification, successful all-workflows creation queues mail and persists CREATED state"() {
        given:
        findOrCreateProcessingOption(ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX, 'OTP')
        findOrCreateProcessingOption(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM, 'ticket@example.com')
        Project project = createProject()
        Ticket ticket = createTicket()
        Workflow workflow = createWorkflow(name: DataInstallationWorkflow.WORKFLOW)
        Notification dependingNotification = new Notification(
                notificationState: NotificationState.CREATED,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: workflow,
                project: project,
        ).save(flush: true)
        Notification notification = new Notification(
                notificationState: NotificationState.READY,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                project: project,
        ).addToDependingNotification(dependingNotification).save(flush: true)
        int mailCount = Mail.count()

        when:
        notificationHandlerService.createNotification(notification)

        then:
        Mail.count() == mailCount + 1
        Mail mail = Mail.findBySubjectLike('%all workflows finished')
        mail.subject == "[OTP#${ticket.ticketNumber}] TO BE SENT: ${project.name} all workflows finished"
        mail.body.contains("initialized '${workflow.displayName}' workflows of project '${project.name}' are finished")
        !mail.body.contains('service team')
        mail.to as Set == ['ticket@example.com'] as Set
        notification.refresh().notificationState == NotificationState.CREATED
    }

    void "createNotificationStatus, final status creation links every ticket import and persists finalSend"() {
        given:
        findOrCreateProcessingOption(ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX, 'OTP')
        findOrCreateProcessingOption(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM, 'ticket@example.com')
        Project project = createProject()
        Ticket ticket = createTicket()
        FastqImportInstance firstImport = createFastqImportInstance(ticket: ticket)
        FastqImportInstance secondImport = createFastqImportInstance(ticket: ticket)
        Workflow workflow = createWorkflow(name: 'Status workflow')
        WorkflowRun workflowRun = createWorkflowRun(
                workflow: workflow,
                project: project,
                state: WorkflowRun.State.SUCCESS,
        )
        Notification notification = new Notification(
                notificationState: NotificationState.CREATED,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: workflow,
                project: project,
        ).addToWorkflowRuns(workflowRun).save(flush: true)
        NotificationStatus notificationStatus = new NotificationStatus(
                ticket: ticket,
                notifications: [notification] as Set,
        ).save(flush: true)
        int mailCount = Mail.count()

        when:
        notificationHandlerService.createNotificationStatus(notificationStatus)

        then:
        Mail.count() == mailCount + 1
        Mail mail = Mail.findBySubject("[OTP#${ticket.ticketNumber}] Final Processing Status Update")
        mail
        mail.body.startsWith("Dear data manager,\n\nThe workflows of ticket ${ticket.ticketNumber}:")
        mail.body.contains('- Status workflow: ALL_DONE: 1 SUCCESS')
        mail.body.count("/metadataImport/details/${firstImport.id}") == 1
        mail.body.count("/metadataImport/details/${secondImport.id}") == 1
        mail.body.count('Best regards,\nOTP') == 1
        mail.to as Set == ['ticket@example.com'] as Set
        notificationStatus.refresh().finalSend
    }

    void "createNotificationStatus, final status creation is not repeated for a status which already sent its final update"() {
        given:
        findOrCreateProcessingOption(ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX, 'OTP')
        findOrCreateProcessingOption(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM, 'ticket@example.com')
        Project project = createProject()
        Ticket ticket = createTicket()
        Workflow workflow = createWorkflow(name: 'Status workflow')
        WorkflowRun workflowRun = createWorkflowRun(
                workflow: workflow,
                project: project,
                state: WorkflowRun.State.SUCCESS,
        )
        Notification notification = new Notification(
                notificationState: NotificationState.CREATED,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: workflow,
                project: project,
        ).addToWorkflowRuns(workflowRun).save(flush: true)
        NotificationStatus notificationStatus = new NotificationStatus(
                ticket: ticket,
                notifications: [notification] as Set,
        ).save(flush: true)
        int mailCount = Mail.count()

        when: 'the final status update is created for the first time'
        notificationHandlerService.createNotificationStatus(notificationStatus)

        then:
        Mail.count() == mailCount + 1
        notificationStatus.refresh().finalSend

        when: 'the same status is handled again'
        notificationHandlerService.createNotificationStatus(notificationStatus)

        then: 'no further mail is queued'
        Mail.count() == mailCount + 1
    }
}
