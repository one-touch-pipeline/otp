/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.qcTrafficLight

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

@Transactional
class QcTrafficLightNotificationService {

    OtrsTicketService otrsTicketService
    CreateNotificationTextService createNotificationTextService
    ProcessingOptionService processingOptionService
    UserProjectRoleService userProjectRoleService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService

    private String createResultsAreBlockedSubject(AbstractMergedBamFile bamFile, boolean toBeSent) {
        StringBuilder subject = new StringBuilder()
        if (toBeSent) {
            subject << 'TO BE SENT: '
        }

        subject << messageSourceService.createMessage(
                "notification.template.alignment.qcTrafficBlockedSubject",
                [
                        bamFile: bamFile,
                ]
        )

        return subject.toString()
    }

    private String createResultsAreBlockedMessage(AbstractMergedBamFile bamFile) {
        return messageSourceService.createMessage(
                "notification.template.alignment.qcTrafficBlockedMessage",
                [
                        bamFile              : bamFile,
                        link                 : createNotificationTextService.createOtpLinks([bamFile.project], 'alignmentQualityOverview', 'index'),
                        emailSenderSalutation: processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION),
                ]
        )
    }

    void informResultsAreBlocked(AbstractMergedBamFile bamFile) {
        boolean projectNotification = bamFile.project.qcTrafficLightNotification
        boolean ticketNotification = otrsTicketService.findAllOtrsTickets(bamFile.containedSeqTracks).find {
            !it.finalNotificationSent && it.automaticNotification
        } as boolean
        boolean shouldSendEmailToProjectReceiver = projectNotification && ticketNotification
        List<String> recipients = shouldSendEmailToProjectReceiver ? userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(bamFile.project) : []
        String subject = createResultsAreBlockedSubject(bamFile, recipients.empty)
        String content = createResultsAreBlockedMessage(bamFile)
        recipients << processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION)
        mailHelperService.sendEmail(subject, content, recipients)
    }
}
