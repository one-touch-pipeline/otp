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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

@Slf4j
@Component
class MailScheduler {

    @Autowired
    ConfigService configService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    WorkflowSystemService workflowSystemService

    @SuppressWarnings("CatchThrowable")
    @Scheduled(fixedDelay = 60000L)
    void scheduleMail() {
        if (!workflowSystemService.enabled) {
            return
        }
        if (!configService.otpSendsMails()) {
            return
        }

        List<Mail> mails = mailHelperService.fetchMailsInWaiting()
        log.debug("Found ${mails.size()} mails for sending")

        mails.each { Mail mail ->
            try {
                mailHelperService.changeMailState(mail, Mail.State.SENDING)
                mailHelperService.sendMailByScheduler(mail)
            } catch (Throwable e) {
                log.error("Error while sending mail", e)
                mailHelperService.changeMailState(mail, Mail.State.FAILED)
                mailHelperService.sendMailByScheduler(new Mail([
                        subject: "Error while sending mail: [${mail.id}] ${mail.subject}",
                        body   : "Error while sending mail: ${e.message}\n\n${mail.body}",
                        to     : [mailHelperService.ticketSystemMailAddress],
                ]))
            }
        }
    }
}
