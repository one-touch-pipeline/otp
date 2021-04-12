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
package de.dkfz.tbi.otp.administration

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.validation.Errors
import spock.lang.Specification

import de.dkfz.tbi.otp.OtpException
import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class ShutdownServiceIntegrationSpec extends Specification implements UserAndRoles {
    ShutdownService shutdownService = new ShutdownService()
    static final String REASON = 'reason'

    void setupData() {
        createUserAndRoles()
    }

    void "test planShutdown valid input"() {
        given:
        setupData()
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
        setupData()
        shutdownService.workflowSystemService.propertiesValidationService = Mock(PropertiesValidationService) {
            1 * validateProcessingOptions() >> []
        }
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
        given:
        setupData()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            shutdownService.cancelShutdown()
        }

        then:
        thrown(OtpException)
        ShutdownInformation.findAll() == []
    }

    void "test destroy, when planned"() {
        given:
        setupData()

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
        given:
        setupData()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            shutdownService.destroy()
        }

        then:
        shutdownService.isShutdownSuccessful()
    }
}
