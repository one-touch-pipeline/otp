/*
 * Copyright 2011-2024 The OTP authors
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
import de.dkfz.tbi.otp.tracking.Ticket.ProcessingStep
import de.dkfz.tbi.otp.administration.MailHelperService
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
    AbstractBamFileService abstractBamFileService
    TicketService ticketService

    List<PreparedNotification> prepareNotifications(NotificationCommand cmd) {
        return ticketService.findAllSeqTracks(cmd.ticket).groupBy { it.project }.collect { Project project, List<SeqTrack> seqTracksOfProject ->
            ProcessingStatus status = notificationCreator.getProcessingStatus(seqTracksOfProject)
            return new PreparedNotification(
                    project                 : project,
                    seqTracks               : seqTracksOfProject,
                    toBeNotifiedProjectUsers: userProjectRoleService.getProjectUsersToBeNotified([project]).sort { it.user.username },
                    processingStatus        : status,
                    subject                 : buildNotificationSubject(cmd.ticket, seqTracksOfProject, project),
                    notification            : buildNotificationDigest(cmd, status),
            )
        }.sort { it.project.name }
    }

    String buildNotificationSubject(Ticket ticket, List<SeqTrack> seqTracks, Project project) {
        String ilseIdentifier = ilseSubmissionService.buildIlseIdentifierFromSeqTracks(seqTracks)
        return "[${ticketService.getPrefixedTicketNumber(ticket)}]${ilseIdentifier} - ${project.name} - OTP processing completed"
    }

    String buildNotificationDigest(NotificationCommand cmd, ProcessingStatus status) {
        List<String> content = []
        List<ProcessingStep> processingSteps = cmd.steps.findAll()
        if (processingSteps) {
            String stepContent = createNotificationTextService.buildStepNotifications(processingSteps, status)
            if (stepContent) {
                content << stepContent
                content << createNotificationTextService.buildSeqCenterComment(cmd.ticket)
            }
        }

        if (!content) {
            return ""
        }

        return messageSourceService.createMessage('notification.template.digest', [
                content              : content.join("\n\n"),
                emailSenderSalutation: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
                faq                  : createNotificationTextService.faq,
        ])
    }

    List<PreparedNotification> prepareAndSendNotifications(NotificationCommand cmd) {
        return sendPreparedNotification(prepareNotifications(cmd))
    }

    List<PreparedNotification> sendPreparedNotification(List<PreparedNotification> preparedNotifications) {
        return preparedNotifications.each { PreparedNotification preparedNotification ->
            sendPreparedNotification(preparedNotification)
        }
    }

    void sendPreparedNotification(PreparedNotification preparedNotification) {
        List<String> recipients = preparedNotification.toBeNotifiedProjectUsers*.user*.email ?: []
        mailHelperService.saveMail(preparedNotification.subject, preparedNotification.notification, recipients)
    }
}
