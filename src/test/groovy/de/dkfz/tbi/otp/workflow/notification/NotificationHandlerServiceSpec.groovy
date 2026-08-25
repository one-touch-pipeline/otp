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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationScope
import de.dkfz.tbi.otp.notification.NotificationState
import de.dkfz.tbi.otp.notification.NotificationStatus
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

class NotificationHandlerServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [Notification, Ticket]
    }

    private NotificationHandlerService createHandler(Map properties = [:]) {
        Map handlerProperties = new LinkedHashMap(properties)
        handlerProperties.notificationMessageService = handlerProperties.notificationMessageService ?: Mock(NotificationMessageService)
        return new NotificationHandlerService(handlerProperties)
    }

    void "setWorkflowNotifications fails when multiple providers use the same workflow name"() {
        given:
        WorkflowNotification firstProvider = Mock(WorkflowNotification) {
            workflowName() >> 'workflow'
        }
        WorkflowNotification secondProvider = Mock(WorkflowNotification) {
            workflowName() >> 'workflow'
        }
        NotificationHandlerService handler = new NotificationHandlerService()

        when:
        handler.workflowNotifications = [firstProvider, secondProvider]

        then:
        IllegalStateException exception = thrown()
        exception.message == "Multiple notification providers configured for workflow 'workflow'"
    }

    void "createNotification skips unsupported scopes after queuing a diagnostic operator mail"() {
        given:
        Notification notification = Mock(Notification) {
            getId() >> 12L
            getNotificationState() >> NotificationState.READY
            getNotificationScope() >> NotificationScope.PROJECT
        }
        MailHelperService mailHelperService = Mock()
        NotificationMessageService notificationMessageService = Mock()
        NotificationHandlerService handler = createHandler(
                mailHelperService: mailHelperService,
                notificationMessageService: notificationMessageService,
        )

        when:
        handler.createNotification(notification)

        then:
        1 * notificationMessageService.createUnsupportedSubject() >> 'subject'
        1 * notificationMessageService.createUnsupportedBody(notification) >> 'body'
        1 * mailHelperService.saveMail('subject', 'body', [])
        1 * notification.setNotificationState(NotificationState.SKIPPED)
        1 * notification.save([flush: true])
    }

    void "createNotification routes supported ticket cases"() {
        given:
        Ticket ticket = new Ticket(ticketNumber: '123').save(flush: true, failOnError: true)
        Project project = Mock(Project) {
            getName() >> 'Project'
            getProcessingNotification() >> false
        }
        Workflow workflow = Mock(Workflow) {
            getName() >> 'workflow'
            getDisplayName() >> 'Workflow'
        }
        Notification workflowNotification = Mock(Notification) {
            getNotificationState() >> NotificationState.READY
            getNotificationScope() >> NotificationScope.TICKET
            getNotificationScopeId() >> ticket.id
            getProject() >> project
            getWorkflow() >> workflow
            getWorkflowRuns() >> ([] as Set)
        }
        Notification allWorkflowsNotification = Mock(Notification) {
            getNotificationState() >> NotificationState.READY
            getNotificationScope() >> NotificationScope.TICKET
            getNotificationScopeId() >> ticket.id
            getProject() >> project
            getDependingNotification() >> ([] as Set)
        }
        WorkflowNotification provider = Mock(WorkflowNotification) {
            workflowName() >> 'workflow'
        }
        MailHelperService mailHelperService = Mock()
        NotificationMessageService notificationMessageService = Mock()
        NotificationHandlerService handler = createHandler(
                workflowNotifications: [provider],
                userProjectRoleService: Mock(UserProjectRoleService),
                mailHelperService: mailHelperService,
                notificationMessageService: notificationMessageService,
        )

        when:
        handler.createNotification(workflowNotification)

        then:
        1 * notificationMessageService.createWorkflowSubject(ticket, workflowNotification, []) >> 'workflow subject'
        1 * notificationMessageService.createWorkflowBody(workflowNotification, provider) >> 'workflow body'
        1 * mailHelperService.saveMail('workflow subject', 'workflow body', [])

        when:
        handler.createNotification(allWorkflowsNotification)

        then:
        1 * notificationMessageService.createAllWorkflowsSubject(ticket, allWorkflowsNotification) >> 'all subject'
        1 * notificationMessageService.createAllWorkflowsBody([]) >> 'all body'
        1 * mailHelperService.saveMail('all subject', 'all body', [])
    }

    void "createNotification skips a notification that is not ready"() {
        given:
        Notification notification = Mock(Notification) {
            getId() >> 12L
            getNotificationState() >> notificationState
        }
        MailHelperService mailHelperService = Mock()
        NotificationHandlerService handler = new NotificationHandlerService(mailHelperService: mailHelperService)

        when:
        handler.createNotification(notification)

        then:
        0 * mailHelperService.saveMail(_, _, _)
        0 * notification.setNotificationState(_)
        0 * notification.save([flush: true])

        where:
        notificationState << [
                NotificationState.CHECKING,
                NotificationState.CREATED,
                NotificationState.SKIPPED,
        ]
    }

    void "createNotificationForTicketAndWorkflow skips an unsupported workflow"() {
        given:
        Workflow workflow = Mock(Workflow) {
            getName() >> 'unsupported'
        }
        Notification notification = Mock(Notification) {
            getNotificationScope() >> NotificationScope.TICKET
            getWorkflow() >> workflow
        }
        NotificationHandlerService handler = new NotificationHandlerService(workflowNotifications: [])

        when:
        handler.createNotificationForTicketAndWorkflow(notification)

        then:
        1 * notification.setNotificationState(NotificationState.SKIPPED)
        1 * notification.save([flush: true])
    }

    void "createNotificationForTicketAndWorkflow queues the rendered mail for project recipients"() {
        given:
        Ticket ticket = new Ticket(ticketNumber: '123', automaticNotification: true).save(flush: true, failOnError: true)
        Project project = Mock(Project) {
            getName() >> 'Project'
            getProcessingNotification() >> true
        }
        Workflow workflow = Mock(Workflow) {
            getName() >> 'workflow'
            getDisplayName() >> 'Workflow'
        }
        Notification notification = Mock(Notification) {
            getNotificationScope() >> NotificationScope.TICKET
            getNotificationScopeId() >> ticket.id
            getProject() >> project
            getWorkflow() >> workflow
            getWorkflowRuns() >> ([] as Set)
        }
        WorkflowNotification provider = Mock(WorkflowNotification) {
            workflowName() >> 'workflow'
        }
        UserProjectRoleService userProjectRoleService = Mock()
        MailHelperService mailHelperService = Mock()
        NotificationMessageService notificationMessageService = Mock()
        NotificationHandlerService handler = createHandler(
                workflowNotifications: [provider],
                userProjectRoleService: userProjectRoleService,
                mailHelperService: mailHelperService,
                notificationMessageService: notificationMessageService,
        )

        when:
        handler.createNotificationForTicketAndWorkflow(notification)

        then:
        1 * userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([project]) >> ['user@example.com']
        1 * notificationMessageService.createWorkflowSubject(ticket, notification, ['user@example.com']) >> 'subject'
        1 * notificationMessageService.createWorkflowBody(notification, provider) >> 'body'
        1 * mailHelperService.saveMail('subject', 'body', ['user@example.com'])
        1 * notification.setNotificationState(NotificationState.CREATED)
        1 * notification.save([flush: true])
    }

    void "createNotificationForTicketAndWorkflow notifies the project users only if the ticket and the project allow it"() {
        given:
        Ticket ticket = new Ticket(ticketNumber: '123', automaticNotification: automaticNotification).save(flush: true, failOnError: true)
        Project project = Mock(Project) {
            getName() >> 'Project'
            getProcessingNotification() >> processingNotification
        }
        Workflow workflow = Mock(Workflow) {
            getName() >> 'workflow'
            getDisplayName() >> 'Workflow'
        }
        Notification notification = Mock(Notification) {
            getNotificationScope() >> NotificationScope.TICKET
            getNotificationScopeId() >> ticket.id
            getProject() >> project
            getWorkflow() >> workflow
            getWorkflowRuns() >> ([] as Set)
        }
        WorkflowNotification provider = Mock(WorkflowNotification) {
            workflowName() >> 'workflow'
        }
        UserProjectRoleService userProjectRoleService = Mock()
        MailHelperService mailHelperService = Mock()
        NotificationMessageService notificationMessageService = Mock()
        NotificationHandlerService handler = createHandler(
                workflowNotifications: [provider],
                userProjectRoleService: userProjectRoleService,
                mailHelperService: mailHelperService,
                notificationMessageService: notificationMessageService,
        )

        when:
        handler.createNotificationForTicketAndWorkflow(notification)

        then:
        expectedLookups * userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([project]) >> ['user@example.com']
        1 * notificationMessageService.createWorkflowSubject(ticket, notification, expectedRecipients) >> 'subject'
        1 * notificationMessageService.createWorkflowBody(notification, provider) >> 'body'
        1 * mailHelperService.saveMail('subject', 'body', expectedRecipients)

        where:
        automaticNotification | processingNotification || expectedLookups | expectedRecipients
        true                  | true                   || 1               | ['user@example.com']
        true                  | false                  || 0               | []
        false                 | true                   || 0               | []
        false                 | false                  || 0               | []
    }

    void "createNotificationForTicketAndWorkflow asserts ticket scope and one workflow"() {
        when:
        new NotificationHandlerService().createNotificationForTicketAndWorkflow(notification)

        then:
        thrown(AssertionError)

        where:
        notification << [
                new Notification(notificationScope: NotificationScope.PROJECT, workflow: new Workflow(name: 'workflow')),
                new Notification(notificationScope: NotificationScope.TICKET),
        ]
    }

    void "createNotificationForTicketAndAllWorkflow asserts ticket scope and no workflow"() {
        when:
        new NotificationHandlerService().createNotificationForTicketAndAllWorkflow(notification)

        then:
        thrown(AssertionError)

        where:
        notification << [
                new Notification(notificationScope: NotificationScope.PROJECT),
                new Notification(notificationScope: NotificationScope.TICKET, workflow: new Workflow(name: 'workflow')),
        ]
    }

    void "createNotificationForTicketAndAllWorkflow orders rendered workflow fragments"() {
        given:
        Ticket ticket = new Ticket(ticketNumber: '456').save(flush: true, failOnError: true)
        Workflow alphaWorkflow = Mock(Workflow) {
            getName() >> 'alpha'
            getDisplayName() >> 'Alpha'
        }
        Workflow betaWorkflow = Mock(Workflow) {
            getName() >> 'beta'
            getDisplayName() >> 'Beta'
        }
        Notification alphaNotification = Mock(Notification) {
            getWorkflow() >> alphaWorkflow
        }
        Notification betaNotification = Mock(Notification) {
            getWorkflow() >> betaWorkflow
        }
        Notification notification = Mock(Notification) {
            getNotificationScope() >> NotificationScope.TICKET
            getNotificationScopeId() >> ticket.id
            getDependingNotification() >> ([betaNotification, alphaNotification] as Set)
        }
        WorkflowNotification alphaProvider = Mock(WorkflowNotification) {
            workflowName() >> 'alpha'
        }
        WorkflowNotification betaProvider = Mock(WorkflowNotification) {
            workflowName() >> 'beta'
        }
        MailHelperService mailHelperService = Mock()
        NotificationMessageService notificationMessageService = Mock()
        NotificationHandlerService handler = createHandler(
                workflowNotifications: [betaProvider, alphaProvider],
                mailHelperService: mailHelperService,
                notificationMessageService: notificationMessageService,
        )

        when:
        handler.createNotificationForTicketAndAllWorkflow(notification)

        then:
        1 * notificationMessageService.createAllWorkflowsSubject(ticket, notification) >> 'subject'
        1 * notificationMessageService.createWorkflowContent(alphaNotification, alphaProvider) >> 'alpha'
        1 * notificationMessageService.createWorkflowContent(betaNotification, betaProvider) >> 'beta'
        1 * notificationMessageService.createAllWorkflowsBody(['alpha', 'beta']) >> 'body'
        1 * mailHelperService.saveMail('subject', 'body', [])
    }

    void "createNotificationStatus persists finalSend after queuing a final status mail"() {
        given:
        Ticket ticket = Mock(Ticket) {
            getTicketNumber() >> '123'
        }
        Workflow workflow = Mock(Workflow) {
            getName() >> 'workflow'
            getDisplayName() >> 'Workflow'
        }
        WorkflowRun workflowRun = Mock(WorkflowRun) {
            getWorkflow() >> workflow
            getState() >> WorkflowRun.State.SUCCESS
        }
        Notification notification = Mock(Notification) {
            getWorkflowRuns() >> ([workflowRun] as Set)
            getNotificationState() >> NotificationState.CREATED
        }
        NotificationStatus notificationStatus = Mock(NotificationStatus) {
            getTicket() >> ticket
            getNotifications() >> ([notification] as Set)
        }
        MailHelperService mailHelperService = Mock()
        NotificationMessageService notificationMessageService = Mock()
        NotificationHandlerService handler = createHandler(
                mailHelperService: mailHelperService,
                notificationMessageService: notificationMessageService,
        )

        when:
        handler.createNotificationStatus(notificationStatus)

        then:
        1 * notificationMessageService.createStatusSubject(ticket, true) >> 'subject'
        1 * notificationMessageService.createStatusBody(ticket, [workflowRun]) >> 'body'
        1 * mailHelperService.saveMail('subject', 'body', [])

        then:
        1 * notificationStatus.setFinishedWorkflowCount(1)
        1 * notificationStatus.setFinalSend(true)
        1 * notificationStatus.save([flush: true])
    }

    void "createNotificationStatus skips a status whose final update was already created"() {
        given:
        NotificationStatus notificationStatus = Mock(NotificationStatus) {
            getId() >> 5L
            getFinalSend() >> true
        }
        MailHelperService mailHelperService = Mock()
        NotificationHandlerService handler = createHandler(
                mailHelperService: mailHelperService,
        )

        when:
        handler.createNotificationStatus(notificationStatus)

        then:
        0 * mailHelperService.saveMail(_, _, _)
        0 * notificationStatus.setFinalSend(_)
        0 * notificationStatus.save([flush: true])
    }

    void "createNotificationStatus decides the final update by the notification state, also without any workflow run"() {
        given:
        Ticket ticket = Mock(Ticket) {
            getTicketNumber() >> '123'
        }
        Notification notification = Mock(Notification) {
            getWorkflowRuns() >> ([] as Set)
            getNotificationState() >> notificationState
        }
        NotificationStatus notificationStatus = Mock(NotificationStatus) {
            getTicket() >> ticket
            getNotifications() >> ([notification] as Set)
        }
        MailHelperService mailHelperService = Mock()
        NotificationMessageService notificationMessageService = Mock()
        NotificationHandlerService handler = createHandler(
                mailHelperService: mailHelperService,
                notificationMessageService: notificationMessageService,
        )

        when:
        handler.createNotificationStatus(notificationStatus)

        then:
        1 * notificationMessageService.createStatusSubject(ticket, finalStatus) >> 'subject'
        1 * notificationMessageService.createStatusBody(ticket, []) >> 'body'
        1 * mailHelperService.saveMail('subject', 'body', [])
        1 * notificationStatus.setFinishedWorkflowCount(finishedCount)
        finalSendCount * notificationStatus.setFinalSend(true)
        1 * notificationStatus.save([flush: true])

        where:
        notificationState          || finalStatus | finalSendCount | finishedCount
        NotificationState.CHECKING || false       | 0              | 0
        NotificationState.READY    || false       | 0              | 0
        NotificationState.CREATED  || true        | 1              | 1
        NotificationState.SKIPPED  || true        | 1              | 1
    }

    void "all-workflows body skips depending notifications without a notification provider"() {
        given:
        Ticket ticket = new Ticket(ticketNumber: '456').save(flush: true, failOnError: true)
        Project project = Mock(Project) {
            getName() >> 'Project'
        }
        Workflow supportedWorkflow = Mock(Workflow) {
            getName() >> 'alpha'
            getDisplayName() >> 'Alpha'
        }
        Workflow unsupportedWorkflow = Mock(Workflow) {
            getName() >> 'gamma'
            getDisplayName() >> 'Gamma'
        }
        Notification supportedNotification = Mock(Notification) {
            getProject() >> project
            getWorkflow() >> supportedWorkflow
            getWorkflowRuns() >> ([] as Set)
        }
        Notification unsupportedNotification = Mock(Notification) {
            getProject() >> project
            getWorkflow() >> unsupportedWorkflow
            getWorkflowRuns() >> ([] as Set)
        }
        Notification notification = Mock(Notification) {
            getNotificationScope() >> NotificationScope.TICKET
            getNotificationScopeId() >> ticket.id
            getProject() >> project
            getDependingNotification() >> ([unsupportedNotification, supportedNotification] as Set)
        }
        WorkflowNotification provider = Mock(WorkflowNotification) {
            workflowName() >> 'alpha'
        }
        MailHelperService mailHelperService = Mock()
        NotificationMessageService notificationMessageService = Mock()
        NotificationHandlerService handler = createHandler(
                workflowNotifications: [provider],
                mailHelperService: mailHelperService,
                notificationMessageService: notificationMessageService,
        )

        when:
        handler.createNotificationForTicketAndAllWorkflow(notification)

        then:
        1 * notificationMessageService.createAllWorkflowsSubject(ticket, notification) >> 'subject'
        1 * notificationMessageService.createWorkflowContent(supportedNotification, provider) >> 'content'
        0 * notificationMessageService.createWorkflowContent(unsupportedNotification, _)
        1 * notificationMessageService.createAllWorkflowsBody(['content']) >> 'body'
        1 * mailHelperService.saveMail('subject', 'body', [])
    }
}
