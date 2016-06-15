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
        sendEmail(subject, content, grailsApplication.config.otp.mail.notification.to)
    }

    void sendEmail(String emailSubject, String content, String recipient) {
        assert emailSubject
        assert content
        assert recipient
        log.info "Send email: subject: '${emailSubject}', to: '${recipient}', content: '${content}'"
        mailService.sendMail {
            from grailsApplication.config.otp.mail.sender
            to recipient
            subject emailSubject
            body content
        }
    }
}
