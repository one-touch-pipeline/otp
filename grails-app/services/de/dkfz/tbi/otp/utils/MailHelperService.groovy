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

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.plugins.mail.MailService
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

@Transactional
class MailHelperService {

    @Autowired
    GrailsApplication grailsApplication

    MailService mailService
    ConfigService configService
    ProcessingOptionService processingOptionService

    void sendEmail(String emailSubject, String content, String recipient) {
        assert recipient
        sendEmail(emailSubject, content, Arrays.asList(recipient))
    }

    void sendEmail(String emailSubject, String content, String recipient, String replyToAddress) {
        sendEmail(emailSubject, content, Arrays.asList(recipient), replyToAddress)
    }

    void sendEmail(String emailSubject, String content, List<String> recipients) {
        sendEmail(emailSubject, content, recipients, defaultReplyToAddress)
    }

    void sendEmail(String emailSubject, String content, String recipient, List<String> ccs) {
        sendEmail(emailSubject, content, Arrays.asList(recipient), ccs)
    }

    void sendEmail(String emailSubject, String content, List<String> recipients, List<String> ccs) {
        sendEmail(emailSubject, content, recipients, ccs, [], defaultReplyToAddress)
    }

    void sendEmail(String emailSubject, String content, List<String> recipients, String replyToAddress) {
        sendEmail(emailSubject, content, recipients, [], [], replyToAddress)
    }

    void sendEmail(String emailSubject, String content, List<String> recipients, List<String> ccs, List<String> bccs, String replyToAddress) {
        assert emailSubject
        assert content
        assert recipients
        [recipients, ccs, bccs].each {
            assert it.every(isValidMail)
        }

        log.info """
            Send email:
            to: '${recipients}'
            cc: '${ccs}'
            bcc: '${bccs}'
            subject: '${emailSubject}'
            content:
            ${content}
        """.stripIndent().toString()

        if (configService.otpSendsMails()) {
            mailService.sendMail {
                from processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER)
                replyTo replyToAddress
                to recipients
                if (ccs) { cc ccs }
                if (bccs) { bcc bccs }
                subject emailSubject
                body content
            }
        }
    }

    private static Closure<Boolean> isValidMail = { String mail ->
        return mail.contains("@")
    }

    String getDefaultReplyToAddress() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_REPLY_TO)
    }

    String getEmailRecipientNotification() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION)
    }
}
