package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.*
import grails.plugin.mail.*
import org.codehaus.groovy.grails.commons.*
import org.springframework.beans.factory.annotation.*

class MailHelperService {

    @Autowired
    GrailsApplication grailsApplication

    MailService mailService

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
            from ProcessingOptionService.findOption(ProcessingOption.OptionName.EMAIL_SENDER, null, null)
            to recipients
            subject emailSubject
            body content
        }
    }
}
