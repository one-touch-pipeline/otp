package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import grails.plugin.mail.*
import org.codehaus.groovy.grails.commons.*
import org.springframework.beans.factory.annotation.*

class MailHelperService {

    @Autowired
    GrailsApplication grailsApplication

    MailService mailService
    ConfigService configService
    ProcessingOptionService processingOptionService

    void sendEmail(String emailSubject, String content, String recipient) {
        sendEmail(emailSubject, content, Arrays.asList(recipient))
    }

    void sendEmail(String emailSubject, String content, String recipient, String sender) {
        sendEmail(emailSubject, content, Arrays.asList(recipient), sender)
    }

    void sendEmail(String emailSubject, String content, List<String> recipients) {
        sendEmail(emailSubject, content, recipients, processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER))
    }

    void sendEmail(String emailSubject, String content, List<String> recipients, String sender) {
        assert emailSubject
        assert content
        assert recipients
        assert recipients.every { it.contains('@') }
        log.info "Send email: subject: '${emailSubject}', to: '${recipients}', content: '${content}'"
        if (configService.otpSendsMails()) {
            mailService.sendMail {
                from sender
                to recipients
                subject emailSubject
                body content
            }
        }
    }
}
