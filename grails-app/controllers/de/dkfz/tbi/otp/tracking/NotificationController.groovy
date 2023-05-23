/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.tracking

import grails.validation.Validateable
import groovy.transform.ToString
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.NotificationDigestService
import de.dkfz.tbi.otp.project.Project

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class NotificationController implements CheckAndCall {

    OtrsTicketService otrsTicketService
    NotificationDigestService notificationDigestService

    static allowedMethods = [
            notificationPreview   : "POST",
            sendNotificationDigest: "POST",
    ]

    def notificationPreview(NotificationCommand cmd) {
        cmd.validate()
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "notification.notificationPreview.failure") as String, cmd.errors)
            return redirect(controller: "metadataImport", action: "details", id: cmd.fastqImportInstance.id)
        }
        try {
            flash.message = new FlashMessage(g.message(code: "notification.notificationPreview.success") as String)
            List<PreparedNotification> preparedNotifications = notificationDigestService.prepareNotifications(cmd)
            boolean emptyNotification = preparedNotifications.empty || preparedNotifications.every {
                it.notification.empty
            }
            return [
                    cmd                  : cmd,
                    importInstances      : otrsTicketService.getAllFastqImportInstances(cmd.otrsTicket).sort { it.dateCreated },
                    otrsTicketLink       : otrsTicketService.buildTicketDirectLinkNullPointerSave(cmd.otrsTicket),
                    prefixedTicketNumber : otrsTicketService.getPrefixedTicketNumber(cmd.fastqImportInstance.otrsTicket),
                    steps                : cmd.steps,
                    notifyQcThresholds   : cmd.notifyQcThresholds,
                    preparedNotifications: preparedNotifications,
                    emptyNotification    : emptyNotification,
            ]
        } catch (AssertionError | RuntimeException e) {
            flash.message = new FlashMessage(g.message(code: "notification.notificationPreview.failure") as String, e.message as String)
        }
        flash.cmd = cmd
        redirect(controller: "metadataImport", action: "details", id: cmd.fastqImportInstance.id)
    }

    def sendNotificationDigest(NotificationCommand cmd) {
        cmd.validate()
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "notification.sendNotificationDigest.failure") as String, cmd.errors)
            redirect(controller: "metadataImport", action: "details", id: cmd.fastqImportInstance.id)
            return
        }
        try {
            notificationDigestService.prepareAndSendNotifications(cmd)
            flash.message = new FlashMessage(g.message(code: "notification.sendNotificationDigest.success") as String)
            redirect(controller: "metadataImport", action: "details", id: cmd.fastqImportInstance.id)
            return
        } catch (AssertionError | RuntimeException e) {
            flash.message = new FlashMessage(g.message(code: "notification.sendNotificationDigest.failure") as String, e.message)
        }
        flash.cmd = cmd
        redirect(controller: "metadataImport", action: "details", id: cmd.fastqImportInstance.id)
    }
}

class PreparedNotification {
    Project project
    List<SeqTrack> seqTracks
    List<AbstractBamFile> bams
    List<UserProjectRole> toBeNotifiedProjectUsers
    ProcessingStatus processingStatus
    String subject
    String notification
}

@ToString(includeNames = true)
class NotificationCommand implements Validateable {
    OtrsTicket otrsTicket
    FastqImportInstance fastqImportInstance
    List<OtrsTicket.ProcessingStep> steps
    boolean notifyQcThresholds
}
