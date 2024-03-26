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
package de.dkfz.tbi.otp.administration

import grails.gorm.transactions.Transactional
import grails.plugins.mail.MailService
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.util.TimeFormats

import java.time.ZonedDateTime

@Transactional
class MailHelperService {

    MailService mailService
    ConfigService configService
    ProcessingOptionService processingOptionService
    SecurityService securityService

    @Transactional(readOnly = true)
    List<Mail> fetchMailsInWaiting() {
        return Mail.findAllWhere([state: Mail.State.WAITING], [sort: 'id'])
    }

    Mail saveErrorMailInNewTransaction(String subject, String content) {
        return SessionUtils.withNewTransaction {
            saveMail(subject, content)
        }
    }

    Mail saveMail(String subject, String body, List<String> to = [], List<String> cc = [], List<String> bcc = []) {
        if (!subject) {
            throw new IllegalArgumentException("mail subject needs to be given")
        }
        if (!body) {
            throw new IllegalArgumentException("mail body needs to be given")
        }

        String ticketSystemMail = ticketSystemMailAddress
        if (!to.contains(ticketSystemMail) && !cc.contains(ticketSystemMail)) {
            if (to) {
                cc.add(ticketSystemMail)
            } else {
                to.add(ticketSystemMail)
            }
        }

        Mail mail = new Mail([
                subject: subject,
                body   : body,
                to     : to,
                cc     : cc,
                bcc    : bcc,
        ])

        mail.save(flush: true)

        log.info("Save Mail: ${mail.id}, '${mail.subject}', to: '${mail.to.join("' & '")}', cc: '${mail.cc.join("' & '")}'")

        return mail
    }

    void sendMailByScheduler(Mail mail) {
        mail.refresh()
        logMail(mail)

        String footer = footerText
        int toCount = mail.to.size() + mail.cc.size() + mail.bcc.size()
        String mailBody = (toCount > 1 && footer) ? (mail.body + '\n\n' + footer) : mail.body
        String senderAddressFetch = senderAddress
        String replyToAddressFetched = replyToAddress

        mailService.sendMail {
            from senderAddressFetch
            replyTo replyToAddressFetched
            // the dsl require list, otherwise exception are thrown
            to mail.to.toList()
            if (mail.cc) {
                cc mail.cc.toList()
            }
            if (mail.bcc) {
                bcc mail.bcc.toList()
            }
            subject mail.subject
            body mailBody
        }

        if (mail.id) {
            mail.sendDateTime = configService.zonedDateTime
            mail.save(flush: true)
            changeMailState(mail, Mail.State.SENT)
        }
    }

    void changeMailState(Mail mail, Mail.State state) {
        mail.refresh()
        mail.state = state
        mail.save(flush: true)
    }

    /**
     * Returns the EMAIL_SENDER_SALUTATION if the current user is an operator or admin. Otherwise it returns the name of the current user.
     */
    String getSenderName() {
        if (securityService.hasCurrentUserAdministrativeRoles()) {
            return processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME)
        }

        return securityService.currentUser.realName
    }

    String getTicketSystemMailAddress() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM)
    }

    private String getReplyToAddress() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_REPLY_TO)
    }

    private String getSenderAddress() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER)
    }

    private String getFooterText() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_FOOTER)
    }

    private void logMail(Mail mail) {
        log.info """
            |Sent mail (id: ${mail.id},  created: ${TimeFormats.DATE_TIME.getFormattedDate(mail.dateCreated)}):
            |to: ${mail.to.join(', ')}
            |cc: ${mail.cc.join(', ')}
            |bcc: ${mail.bcc.join(', ')}
            |subject: '${mail.subject}'
            |content:\n${mail.body}
        """.stripMargin().toString()
    }

    @CompileDynamic
    void deleteOldMails() {
        int delay = processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.DELETE_OLD_MAILS_DELAY)
        List<Mail> mails = Mail.findAllByStateAndSendDateTimeLessThan(Mail.State.SENT, ZonedDateTime.now().minusDays(delay))
        log.info("Deleting ${mails.size()} old mails.")
        Mail.deleteAll(mails)
    }
}
