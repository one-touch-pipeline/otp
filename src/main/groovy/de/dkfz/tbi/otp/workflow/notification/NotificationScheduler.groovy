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
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.notification.CreateNotification
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationStatus
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import static grails.async.Promises.task

// only triggers NotificationService/NotificationHandlerService on a timer; no business logic or queries here
@Slf4j
@Component
class NotificationScheduler {

    @Autowired
    NotificationService notificationService

    @Autowired
    NotificationHandlerService notificationHandlerService

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    WorkflowSystemService workflowSystemService

    // runs async since building the notification graph can take a while and must not block the jobs below
    @Scheduled(fixedDelay = 60000L, initialDelay = 120000L)
    void scheduleCreateNotification() {
        if (!workflowSystemService.enabled) {
            return
        }

        int maxParallel = processingOptionService.findOptionAsInteger(OptionName.NOTIFICATION_CREATE_MAX_PARALLEL)
        if (notificationService.countProcessingCreateNotifications() >= maxParallel) {
            return
        }

        CreateNotification createNotification = notificationService.nextWaitingCreateNotification()
        if (!createNotification) {
            return
        }
        long createNotificationId = createNotification.id

        notificationService.updateCreateNotificationState(createNotificationId, WorkflowCreateState.PROCESSING)

        createNotificationsAsync(createNotificationId)
    }

    // only protected for testing, should not be used outside this class
    protected Promise<Void> createNotificationsAsync(long createNotificationId) {
        return task {
            createNotificationsTask(createNotificationId)
        }
    }

    // only protected for testing, should not be used outside this class
    @SuppressWarnings("CatchThrowable")
    protected void createNotificationsTask(long createNotificationId) {
        try {
            notificationService.createNotifications(createNotificationId)
        } catch (Throwable throwable) {
            log.debug("Failed to create notification objects for CreateNotification ${createNotificationId}", throwable)
            // the state is set first: a failing mail must not leave the entry PROCESSING, where it would block every
            // other CreateNotification of the same ticket or projects until the next restart
            try {
                notificationService.updateCreateNotificationState(createNotificationId, WorkflowCreateState.FAILED)
            } catch (Throwable throwable2) {
                log.debug("Failed to update the state to FAILED for CreateNotification ${createNotificationId}", throwable2)
                throw throwable2
            }
            try {
                notificationService.sendNotificationCreateErrorMail(createNotificationId, throwable)
            } catch (Throwable throwable2) {
                log.debug("Failed to send the error mail for CreateNotification ${createNotificationId}", throwable2)
                throw throwable2
            }
        }
    }

    @Scheduled(fixedDelay = 60000L, initialDelay = 120000L)
    void scheduleUpdateRestartedWorkflowRun() {
        if (!workflowSystemService.enabled) {
            return
        }
        notificationService.updateNotificationsReferencingRestartedWorkflowRun()
    }

    @Scheduled(fixedDelay = 60000L, initialDelay = 120000L)
    void scheduleCheckNotificationReady() {
        if (!workflowSystemService.enabled) {
            return
        }
        notificationService.markCheckingNotificationsReady()
    }

    @Scheduled(fixedDelay = 60000L, initialDelay = 120000L)
    void scheduleCreateNotificationMail() {
        if (!workflowSystemService.enabled) {
            return
        }
        // shared session: the notification is fetched here but its lazy associations are only read inside the next transactional call
        SessionUtils.withNewSession {
            Notification notification = notificationService.nextReadyNotification()
            if (notification) {
                notificationHandlerService.createNotification(notification)
            }
        }
    }

    @Scheduled(fixedDelay = 60000L, initialDelay = 120000L)
    void scheduleCreateNotificationStatusMail() {
        if (!workflowSystemService.enabled) {
            return
        }
        SessionUtils.withNewSession {
            notificationService.notificationStatusesNeedingMail().each { NotificationStatus notificationStatus ->
                notificationHandlerService.createNotificationStatus(notificationStatus)
            }
        }
    }

    @Scheduled(fixedDelay = 3600000L, initialDelay = 120000L)
    void scheduleDeleteOldNotifications() {
        if (!workflowSystemService.enabled) {
            return
        }
        int delayInDays = processingOptionService.findOptionAsInteger(OptionName.NOTIFICATION_DELETE_OLD_DELAY)
        notificationService.deleteOldNotifications(delayInDays)
    }
}
