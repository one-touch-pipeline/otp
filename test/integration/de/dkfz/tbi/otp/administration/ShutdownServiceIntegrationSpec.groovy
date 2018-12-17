package de.dkfz.tbi.otp.administration

import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.validation.Errors
import spock.lang.Specification

import de.dkfz.tbi.otp.OtpException
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

class ShutdownServiceIntegrationSpec extends Specification implements UserAndRoles {
    ShutdownService shutdownService = new ShutdownService()
    static final String REASON = 'reason'

    void setup() {
        createUserAndRoles()
    }

    void "test planShutdown valid input"() {
        given:
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            errors = shutdownService.planShutdown(REASON)
        }

        then:
        ShutdownInformation shutdownInformation = CollectionUtils.exactlyOneElement(ShutdownInformation.findAll())
        shutdownInformation.reason == REASON
        shutdownInformation.canceled == null
        errors == null
    }

    void "test cancelShutdown, when planned"() {
        given:
        Errors errors

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            shutdownService.planShutdown(REASON)
            errors = shutdownService.cancelShutdown()
        }

        then:
        ShutdownInformation shutdownInformation = CollectionUtils.exactlyOneElement(ShutdownInformation.findAll())
        shutdownInformation.reason == REASON
        shutdownInformation.canceled != null
        errors == null
    }

    void "test cancelShutdown, when not planned"() {
        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            shutdownService.cancelShutdown()
        }

        then:
        thrown(OtpException)
        ShutdownInformation.findAll() == []
    }

    void "test destroy, when planned"() {
        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            shutdownService.planShutdown(REASON)
            shutdownService.destroy()
        }

        then:
        ShutdownInformation shutdownInformation = CollectionUtils.exactlyOneElement(ShutdownInformation.findAll())
        shutdownInformation.reason == REASON
        shutdownInformation.succeeded != null
        shutdownService.isShutdownSuccessful()
    }

    void "test destroy, when not planned"() {
        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            shutdownService.destroy()
        }

        then:
        shutdownService.isShutdownSuccessful()
    }
}
