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
package de.dkfz.tbi.otp.notification

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightNotificationService
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

@Transactional
class NotificationDigestService {

    UserProjectRoleService userProjectRoleService
    NotificationCreator notificationCreator
    CreateNotificationTextService createNotificationTextService
    QcTrafficLightNotificationService qcTrafficLightNotificationService
    ProcessingOptionService processingOptionService
    MessageSourceService messageSourceService
    MailHelperService mailHelperService
    IlseSubmissionService ilseSubmissionService
    AbstractMergedBamFileService abstractMergedBamFileService

    List<PreparedNotification> prepareNotifications(NotificationCommand cmd) {
        return cmd.otrsTicket.findAllSeqTracks().groupBy { it.project }.collect { Project project, List<SeqTrack> seqTracksOfProject ->
            ProcessingStatus status = notificationCreator.getProcessingStatus(seqTracksOfProject)
            List<AbstractMergedBamFile> blockedBams = abstractMergedBamFileService.getActiveBlockedBamsContainingSeqTracks(seqTracksOfProject)
            return new PreparedNotification(
                    project                 : project,
                    seqTracks               : seqTracksOfProject,
                    bams                    : blockedBams,
                    toBeNotifiedProjectUsers: userProjectRoleService.getProjectUsersToBeNotified(project).sort { it.user.username },
                    processingStatus        : status,
                    subject                 : buildNotificationSubject(cmd.otrsTicket, seqTracksOfProject, project),
                    notification            : buildNotificationDigest(cmd, status, blockedBams),
            )
        }.sort { it.project.name }
    }

    String buildNotificationSubject(OtrsTicket otrsTicket, List<SeqTrack> seqTracks, Project project) {
        String ilseIdentifier = ilseSubmissionService.buildIlseIdentifierFromSeqTracks(seqTracks)
        return "[${otrsTicket.prefixedTicketNumber}]${ilseIdentifier} - ${project.name} - OTP processing completed"
    }

    String buildNotificationDigest(NotificationCommand cmd, ProcessingStatus status, List<AbstractMergedBamFile> bams) {
        List<String> content = []
        List<ProcessingStep> processingSteps = cmd.steps.findAll()
        if (processingSteps) {
            String stepContent = createNotificationTextService.buildStepNotifications(processingSteps, status)
            if (stepContent) {
                content << stepContent
                content << createNotificationTextService.buildSeqCenterComment(cmd.otrsTicket)
            }
        }

        if (cmd.notifyQcThresholds && bams) {
            content << qcTrafficLightNotificationService.buildContentForMultipleBamsBlockedMessage(bams)
        }

        if (!content) {
            return ""
        }

        return messageSourceService.createMessage('notification.template.digest', [
                content              : content.join("\n\n"),
                emailSenderSalutation: processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION),
                faq                  : createNotificationTextService.faq,
        ])
    }

    List<PreparedNotification> prepareAndSendNotifications(NotificationCommand cmd) {
        return sendPreparedNotification(prepareNotifications(cmd))
    }

    List<PreparedNotification> sendPreparedNotification(List<PreparedNotification> preparedNotifications) {
        preparedNotifications.each { PreparedNotification preparedNotification ->
            sendPreparedNotification(preparedNotification)
        }
    }

    void sendPreparedNotification(PreparedNotification preparedNotification) {
        List<String> recipients = userProjectRoleService.getMails(preparedNotification.toBeNotifiedProjectUsers) ?: []
        recipients << processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION)

        mailHelperService.sendEmail(preparedNotification.subject, preparedNotification.notification, recipients)
    }
}
