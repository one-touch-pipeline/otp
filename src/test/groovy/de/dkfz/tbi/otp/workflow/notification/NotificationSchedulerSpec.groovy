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

import grails.async.Promise
import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.notification.CreateNotification
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationStatus
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

class NotificationSchedulerSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        // needed so SessionUtils.withNewSession (used by scheduleCreateNotificationMail/-StatusMail) has a datastore to work with
        return [Project]
    }

    private static class RecordingNotificationScheduler extends NotificationScheduler {
        List<Long> asyncCalls = []

        @Override
        protected Promise<Void> createNotificationsAsync(long createNotificationId) {
            asyncCalls << createNotificationId
            return null
        }
    }

    RecordingNotificationScheduler scheduler
    NotificationService notificationService
    NotificationHandlerService notificationHandlerService
    ProcessingOptionService processingOptionService
    WorkflowSystemService workflowSystemService

    void setup() {
        notificationService = Mock(NotificationService)
        notificationHandlerService = Mock(NotificationHandlerService)
        processingOptionService = Mock(ProcessingOptionService)
        workflowSystemService = Mock(WorkflowSystemService)

        scheduler = new RecordingNotificationScheduler()
        scheduler.notificationService = notificationService
        scheduler.notificationHandlerService = notificationHandlerService
        scheduler.processingOptionService = processingOptionService
        scheduler.workflowSystemService = workflowSystemService
    }

    void "scheduleCreateNotification does nothing if the job system is disabled"() {
        when:
        scheduler.scheduleCreateNotification()

        then:
        1 * workflowSystemService.enabled >> false
        0 * notificationService._
        scheduler.asyncCalls.empty
    }

    void "scheduleCreateNotification does nothing if already at the configured parallel limit"() {
        when:
        scheduler.scheduleCreateNotification()

        then:
        1 * workflowSystemService.enabled >> true
        1 * processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.NOTIFICATION_CREATE_MAX_PARALLEL) >> 10
        1 * notificationService.countProcessingCreateNotifications() >> 10
        0 * notificationService.nextWaitingCreateNotification()
        scheduler.asyncCalls.empty
    }

    void "scheduleCreateNotification does nothing if nothing is waiting"() {
        when:
        scheduler.scheduleCreateNotification()

        then:
        1 * workflowSystemService.enabled >> true
        1 * processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.NOTIFICATION_CREATE_MAX_PARALLEL) >> 10
        1 * notificationService.countProcessingCreateNotifications() >> 0
        1 * notificationService.nextWaitingCreateNotification() >> null
        0 * notificationService.updateCreateNotificationState(_, _)
        scheduler.asyncCalls.empty
    }

    void "scheduleCreateNotification marks the found entry PROCESSING and triggers the async task"() {
        given:
        CreateNotification createNotification = Mock(CreateNotification) {
            getId() >> 7L
        }

        when:
        scheduler.scheduleCreateNotification()

        then:
        1 * workflowSystemService.enabled >> true
        1 * processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.NOTIFICATION_CREATE_MAX_PARALLEL) >> 10
        1 * notificationService.countProcessingCreateNotifications() >> 0
        1 * notificationService.nextWaitingCreateNotification() >> createNotification
        1 * notificationService.updateCreateNotificationState(7L, WorkflowCreateState.PROCESSING)
        scheduler.asyncCalls == [7L]
    }

    void "createNotificationsTask only calls createNotifications when it succeeds"() {
        when:
        scheduler.createNotificationsTask(7L)

        then:
        1 * notificationService.createNotifications(7L)
        0 * notificationService.sendNotificationCreateErrorMail(_, _)
        0 * notificationService.updateCreateNotificationState(_, _)
    }

    void "createNotificationsTask sends an error mail and marks FAILED when creation throws"() {
        given:
        RuntimeException exception = new RuntimeException('boom')

        when:
        scheduler.createNotificationsTask(7L)

        then:
        1 * notificationService.createNotifications(7L) >> { throw exception }
        1 * notificationService.sendNotificationCreateErrorMail(7L, exception)
        1 * notificationService.updateCreateNotificationState(7L, WorkflowCreateState.FAILED)
    }

    void "createNotificationsTask still marks the entry FAILED when sending the error mail throws"() {
        given:
        RuntimeException exception = new RuntimeException('boom')
        RuntimeException mailException = new RuntimeException('mail down')

        when:
        scheduler.createNotificationsTask(7L)

        then:
        1 * notificationService.createNotifications(7L) >> { throw exception }
        // the state is written before the mail is attempted, so a broken mail system cannot wedge the entry in PROCESSING
        1 * notificationService.updateCreateNotificationState(7L, WorkflowCreateState.FAILED)
        1 * notificationService.sendNotificationCreateErrorMail(7L, exception) >> { throw mailException }
        RuntimeException thrown = thrown(RuntimeException)
        thrown.is(mailException)
    }

    void "createNotificationsTask rethrows and skips the error mail when marking the entry FAILED throws"() {
        given:
        RuntimeException exception = new RuntimeException('boom')
        RuntimeException stateException = new RuntimeException('database down')

        when:
        scheduler.createNotificationsTask(7L)

        then:
        1 * notificationService.createNotifications(7L) >> { throw exception }
        1 * notificationService.updateCreateNotificationState(7L, WorkflowCreateState.FAILED) >> { throw stateException }
        // the mail is saved to the database as well, so it would not get through either
        0 * notificationService.sendNotificationCreateErrorMail(_, _)
        RuntimeException thrown = thrown(RuntimeException)
        thrown.is(stateException)
    }

    @Unroll
    void "scheduleUpdateRestartedWorkflowRun delegates only if the job system is enabled (enabled=#enabled)"() {
        when:
        scheduler.scheduleUpdateRestartedWorkflowRun()

        then:
        1 * workflowSystemService.enabled >> enabled
        callCount * notificationService.updateNotificationsReferencingRestartedWorkflowRun()

        where:
        enabled || callCount
        true    || 1
        false   || 0
    }

    @Unroll
    void "scheduleCheckNotificationReady delegates only if the job system is enabled (enabled=#enabled)"() {
        when:
        scheduler.scheduleCheckNotificationReady()

        then:
        1 * workflowSystemService.enabled >> enabled
        callCount * notificationService.markCheckingNotificationsReady()

        where:
        enabled || callCount
        true    || 1
        false   || 0
    }

    void "scheduleCreateNotificationMail does nothing if disabled"() {
        when:
        scheduler.scheduleCreateNotificationMail()

        then:
        1 * workflowSystemService.enabled >> false
        0 * notificationService.nextReadyNotification()
        0 * notificationHandlerService.createNotification(_)
    }

    void "scheduleCreateNotificationMail does nothing if nothing is ready"() {
        when:
        scheduler.scheduleCreateNotificationMail()

        then:
        1 * workflowSystemService.enabled >> true
        1 * notificationService.nextReadyNotification() >> null
        0 * notificationHandlerService.createNotification(_)
    }

    void "scheduleCreateNotificationMail renders the mail for the next ready notification"() {
        given:
        Notification notification = Mock(Notification)

        when:
        scheduler.scheduleCreateNotificationMail()

        then:
        1 * workflowSystemService.enabled >> true
        1 * notificationService.nextReadyNotification() >> notification
        1 * notificationHandlerService.createNotification(notification)
    }

    void "scheduleCreateNotificationStatusMail does nothing if disabled"() {
        when:
        scheduler.scheduleCreateNotificationStatusMail()

        then:
        1 * workflowSystemService.enabled >> false
        0 * notificationService.notificationStatusesNeedingMail()
        0 * notificationHandlerService.createNotificationStatus(_)
    }

    void "scheduleCreateNotificationStatusMail renders the status mail for every drifted status"() {
        given:
        NotificationStatus firstStatus = Mock(NotificationStatus)
        NotificationStatus secondStatus = Mock(NotificationStatus)

        when:
        scheduler.scheduleCreateNotificationStatusMail()

        then:
        1 * workflowSystemService.enabled >> true
        1 * notificationService.notificationStatusesNeedingMail() >> [firstStatus, secondStatus]
        1 * notificationHandlerService.createNotificationStatus(firstStatus)
        1 * notificationHandlerService.createNotificationStatus(secondStatus)
    }

    void "scheduleDeleteOldNotifications does nothing if disabled"() {
        when:
        scheduler.scheduleDeleteOldNotifications()

        then:
        1 * workflowSystemService.enabled >> false
        0 * processingOptionService.findOptionAsInteger(_)
        0 * notificationService.deleteOldNotifications(_)
    }

    void "scheduleDeleteOldNotifications deletes old notifications using the configured delay"() {
        when:
        scheduler.scheduleDeleteOldNotifications()

        then:
        1 * workflowSystemService.enabled >> true
        1 * processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.NOTIFICATION_DELETE_OLD_DELAY) >> 30
        1 * notificationService.deleteOldNotifications(30)
    }
}
