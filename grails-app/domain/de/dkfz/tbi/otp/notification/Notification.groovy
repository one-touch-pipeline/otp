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
package de.dkfz.tbi.otp.notification

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

/**
 * Helper object referencing data needed for a notification.
 */
@ManagedEntity
class Notification implements Entity {

    /** the state of the notification */
    NotificationState notificationState

    /** the scope of the notification */
    NotificationScope notificationScope

    /** id of the scope object used for the notification (for scope TICKET, it would be the id of a ticket) */
    long notificationScopeId

    /** the workflow the notification belongs to, or null, if it is for all workflows */
    Workflow workflow

    /** references the workflow runs to use for this notification */
    Set<WorkflowRun> workflowRuns

    /** set of notifications, which should be sent before this notification */
    Set<Notification> dependingNotification

    /** the project to get the recipient for the notification */
    Project project

    static hasMany = [
            workflowRuns         : WorkflowRun,
            dependingNotification: Notification,
    ]

    static constraints = {
        workflow nullable: true
    }

    static mapping = {
        workflow index: "notification_workflow_idx"
        project index: "notification_project_idx"
        dependingNotification joinTable: [name: "notification_depending_notification", key: "notification_id", column: "depending_notification_id"]
    }
}
