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
package de.dkfz.tbi.otp.domainFactory.notification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.notification.CreateNotification
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationScope
import de.dkfz.tbi.otp.notification.NotificationState
import de.dkfz.tbi.otp.notification.NotificationStatus
import de.dkfz.tbi.otp.workflow.WorkflowCreateState

trait NotificationDomainFactory implements WorkflowSystemDomainFactory {

    Notification createNotification(Map properties = [:]) {
        return createDomainObject(Notification, [
                notificationState  : NotificationState.CHECKING,
                notificationScope  : NotificationScope.TICKET,
                notificationScopeId: { createTicket().id },
                project            : { createProject() },
        ], properties)
    }

    NotificationStatus createNotificationStatus(Map properties = [:]) {
        return createDomainObject(NotificationStatus, [
                ticket               : { createTicket() },
                finishedWorkflowCount: 0,
                finalSend            : false,
        ], properties)
    }

    CreateNotification createCreateNotification(Map properties = [:]) {
        return createDomainObject(CreateNotification, [
                ticket: { createTicket() },
                state : WorkflowCreateState.WAITING,
        ], properties)
    }
}
