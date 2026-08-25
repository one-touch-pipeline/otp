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
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationScope
import de.dkfz.tbi.otp.notification.NotificationState
import de.dkfz.tbi.otp.notification.NotificationStatus
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

@Transactional
class NotificationHandlerService {

    MailHelperService mailHelperService
    NotificationMessageService notificationMessageService
    UserProjectRoleService userProjectRoleService

    private Map<String, WorkflowNotification> workflowNotificationsByName = [:]

    @Autowired
    void setWorkflowNotifications(List<WorkflowNotification> workflowNotifications) {
        Map<String, WorkflowNotification> notificationsByName = [:]
        workflowNotifications.each { WorkflowNotification workflowNotification ->
            String workflowName = workflowNotification.workflowName()
            if (notificationsByName.containsKey(workflowName)) {
                throw new IllegalStateException("Multiple notification providers configured for workflow '${workflowName}'")
            }
            notificationsByName[workflowName] = workflowNotification
        }
        workflowNotificationsByName = notificationsByName
    }

    void createNotification(Notification notification) {
        // only a READY notification still needs a mail, so a second scheduler pick or a manual re-trigger must not create a duplicate
        if (notification.notificationState != NotificationState.READY) {
            log.info("Skip notification ${notification.id}: it is in state '${notification.notificationState}' instead of " +
                    "'${NotificationState.READY}'")
            return
        }

        if (notification.notificationScope == NotificationScope.TICKET) {
            if (notification.workflow) {
                createNotificationForTicketAndWorkflow(notification)
                return
            }
            createNotificationForTicketAndAllWorkflow(notification)
            return
        }

        log.warn("Skip notification ${notification.id}: unsupported scope '${notification.notificationScope}'")
        mailHelperService.saveMail(
                notificationMessageService.createUnsupportedSubject(),
                notificationMessageService.createUnsupportedBody(notification),
                [],
        )
        notification.notificationState = NotificationState.SKIPPED
        notification.save(flush: true)
    }

    void createNotificationStatus(NotificationStatus notificationStatus) {
        if (notificationStatus.finalSend) {
            log.info("Skip notification status ${notificationStatus.id}: the final status update was already created")
            return
        }

        Ticket ticket = notificationStatus.ticket
        assert ticket
        Set<Notification> notifications = notificationStatus.notifications ?: [] as Set<Notification>
        List<WorkflowRun> workflowRuns = []
        notifications.each { Notification notification ->
            workflowRuns.addAll(notification.workflowRuns ?: [] as Set<WorkflowRun>)
        }
        int finishedCount = NotificationService.finishedNotificationCount(notificationStatus)
        boolean finalStatus = finishedCount == notifications.size()
        mailHelperService.saveMail(
                notificationMessageService.createStatusSubject(ticket, finalStatus),
                notificationMessageService.createStatusBody(ticket, workflowRuns),
                [],
        )

        // persisted even when not final, so the scheduler can detect drift and know a status mail is still due
        notificationStatus.finishedWorkflowCount = finishedCount
        if (finalStatus) {
            notificationStatus.finalSend = true
        }
        notificationStatus.save(flush: true)
    }

    void createNotificationForTicketAndAllWorkflow(Notification notification) {
        assert notification.notificationScope == NotificationScope.TICKET
        assert !notification.workflow

        Ticket ticket = findTicket(notification.notificationScopeId)
        assert ticket
        String subject = notificationMessageService.createAllWorkflowsSubject(ticket, notification)
        Set<Notification> dependingNotifications = notification.dependingNotification ?: [] as Set<Notification>
        List<String> workflowContents = dependingNotifications
                .findAll { Notification dependingNotification ->
                    dependingNotification.workflow
                }
                .sort { Notification dependingNotification ->
                    "${dependingNotification.workflow.displayName}\u0000${dependingNotification.workflow.name}"
                }
                .collect { Notification dependingNotification ->
                    WorkflowNotification workflowNotification = findWorkflowNotification(dependingNotification.workflow)
                    if (!workflowNotification) {
                        log.info("Skip depending notification ${dependingNotification.id} of notification ${notification.id}: " +
                                "no notification provider for workflow '${dependingNotification.workflow.name}'")
                        return null
                    }
                    return notificationMessageService.createWorkflowContent(dependingNotification, workflowNotification)
                }
                .findAll()
        String body = notificationMessageService.createAllWorkflowsBody(workflowContents)

        mailHelperService.saveMail(subject, body, [])
        notification.notificationState = NotificationState.CREATED
        notification.save(flush: true)
    }

    void createNotificationForTicketAndWorkflow(Notification notification) {
        assert notification.notificationScope == NotificationScope.TICKET
        assert notification.workflow

        WorkflowNotification workflowNotification = findWorkflowNotification(notification.workflow)
        if (!workflowNotification) {
            log.info("Skip notification ${notification.id}: no notification provider for workflow '${notification.workflow.name}'")
            notification.notificationState = NotificationState.SKIPPED
            notification.save(flush: true)
            return
        }

        Ticket ticket = findTicket(notification.notificationScopeId)
        assert ticket
        List<String> recipients = ticket.automaticNotification && notification.project.processingNotification ?
                userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([notification.project]) : []
        String subject = notificationMessageService.createWorkflowSubject(ticket, notification, recipients)
        String body = notificationMessageService.createWorkflowBody(notification, workflowNotification)

        mailHelperService.saveMail(subject, body, recipients)
        notification.notificationState = NotificationState.CREATED
        notification.save(flush: true)
    }

    private WorkflowNotification findWorkflowNotification(Workflow workflow) {
        return workflowNotificationsByName[workflow.name]
    }

    @CompileDynamic
    private static Ticket findTicket(long id) {
        return Ticket.get(id)
    }
}
