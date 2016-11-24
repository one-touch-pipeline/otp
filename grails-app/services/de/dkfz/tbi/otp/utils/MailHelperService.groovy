package de.dkfz.tbi.otp.utils

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import grails.plugin.mail.MailService

class MailHelperService {

    @Autowired
    GrailsApplication grailsApplication

    MailService mailService

    String getOtrsRecipient() {
        return grailsApplication.config.otp.mail.notification.fasttrack.to
    }

    void sendNotificationEmail(String subject, String content) {
        sendEmail(subject, content, (String)grailsApplication.config.otp.mail.notification.to)
    }

    void sendEmail(String emailSubject, String content, String recipient) {
       sendEmail(emailSubject, content, Arrays.asList(recipient))
    }

    void sendEmail(String emailSubject, String content, List<String> recipients) {
        assert emailSubject
        assert content
        assert recipients
        assert recipients.every { it.contains('@') }
        log.info "Send email: subject: '${emailSubject}', to: '${recipients}', content: '${content}'"
        mailService.sendMail {
            from grailsApplication.config.otp.mail.sender
            to recipients
            subject emailSubject
            body content
        }
    }
}
