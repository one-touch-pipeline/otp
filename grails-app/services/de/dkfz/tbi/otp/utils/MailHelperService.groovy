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
package de.dkfz.tbi.otp.utils

import grails.gorm.transactions.Transactional
import grails.plugins.mail.MailService

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.RolesService
import de.dkfz.tbi.otp.security.user.UserService

@Transactional
class MailHelperService {

    MailService mailService
    ConfigService configService
    ProcessingOptionService processingOptionService
    UserService userService
    RolesService rolesService

    void sendEmail(String emailSubject, String content, String recipient, List<String> ccs = []) {
        sendEmail(emailSubject, content, Arrays.asList(recipient), ccs)
    }

    void sendEmail(String emailSubject, String content, List<String> recipients, List<String> ccs = []) {
        sendEmail(emailSubject, content, recipients, defaultReplyToAddress, ccs)
    }

    /**
     * Send an email to the ticket system only.
     *
     * @param emailSubject of the email
     * @param content of the email
     * @param replyTo of the email
     */
    void sendEmailToTicketSystem(String emailSubject, String content, String replyTo) {
        sendEmail(emailSubject, content, [ticketSystemEmailAddress], replyTo)
    }

    /**
     * Send an email to the ticket system only.
     *
     * @param emailSubject of the email
     * @param content of the email
     */
    void sendEmailToTicketSystem(String emailSubject, String content) {
        sendEmail(emailSubject, content, ticketSystemEmailAddress)
    }

    /**
     * Send an email to the given recipients and also to the ticket system in CC.
     *
     * @param emailSubject of the email
     * @param content of the email
     * @param recipients of the email
     * @param ccs of the email
     * @param replyToAddress of the email
     */
    void sendEmail(String emailSubject, String content, List<String> recipients, String replyToAddress, List<String> ccs = []) {
        assert emailSubject
        assert content

        [recipients, ccs].each {
            assert it.every { isEmailValid(it) }
        }

        if (!recipients.contains(ticketSystemEmailAddress)) {
            if (recipients) {
                ccs.add(ticketSystemEmailAddress)
            } else {
                recipients.add(ticketSystemEmailAddress)
            }
        }

        logEmail(recipients, ccs, emailSubject, content)

        if (configService.otpSendsMails()) {
            mailService.sendMail {
                from processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER)
                replyTo replyToAddress
                to recipients
                if (ccs) { cc ccs }
                subject emailSubject
                body content
            }
        }
    }

    /**
     * Returns the EMAIL_SENDER_SALUTATION if the current user is an operator or admin. Otherwise it returns the name of the current user.
     */
    String getSenderName() {
        User currentUser = userService.currentUser

        if (rolesService.isAdministrativeUser(currentUser)) {
            return processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME)
        }

        return currentUser.realName
    }

    String getTicketSystemEmailAddress() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM)
    }

    private boolean isEmailValid(String mail) {
        return mail.contains("@")
    }

    private String getDefaultReplyToAddress() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_REPLY_TO)
    }

    private logEmail(List<String> recipients, List<String> ccs, String subject, String content) {
        String status = configService.otpSendsMails() ? "enabled" : "disabled"

        log.info("Email system is ${status}.")
        log.info """
            Triggered email:
            to: '${recipients}'
            cc: '${ccs}'
            subject: '${subject}'
            content:
            ${content}
        """.stripIndent().toString()
    }
}
