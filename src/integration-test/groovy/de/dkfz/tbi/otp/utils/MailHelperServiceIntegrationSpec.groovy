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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugins.mail.MailService
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.security.RolesService
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.security.User

@Rollback
@Integration
class MailHelperServiceIntegrationSpec extends Specification implements DomainFactoryCore, UserDomainFactory, UserAndRoles {

    static final String SUBJECT = 'subject'
    static final String BODY = 'body'
    static final String RECIPIENT = HelperUtils.randomEmail
    static final String TICKET_EMAIL = HelperUtils.randomEmail
    static final String SENDER_SALUTATION = "OTP team"

    MailHelperService mailHelperService
    TestConfigService configService

    void setupData() {
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM, value: TICKET_EMAIL)
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION, value: SENDER_SALUTATION)

        mailHelperService = new MailHelperService()
        mailHelperService.processingOptionService = new ProcessingOptionService()
        mailHelperService.rolesService = new RolesService()
        mailHelperService.configService = configService
        mailHelperService.mailService = Mock(MailService)
    }

    void "sendEmail with valid parameters"() {
        given:
        setupData()

        expect:
        mailHelperService.sendEmail(SUBJECT, BODY, RECIPIENT)
    }

    @Unroll
    void "sendEmail test null assertions ( subject=#subject body=#body recipient=#recipient )"() {
        given:
        setupData()

        when:
        mailHelperService.sendEmail(subject, body, recipient)

        then:
        thrown(AssertionError)

        where:
        subject | body | recipient
        null    | BODY | RECIPIENT
        SUBJECT | null | RECIPIENT
        SUBJECT | BODY | null
    }

    void "sendEmail with recipients list"() {
        given:
        setupData()

        expect:
        mailHelperService.sendEmail(SUBJECT, BODY, ["a@b.c", "d@e.f"])
    }

    @Unroll
    void "sendEmail with recipients list, when list #testSubject"() {
        given:
        setupData()

        when:
        mailHelperService.sendEmail(SUBJECT, BODY, receiverList)

        then:
        thrown(AssertionError)

        where:
        receiverList     | testSubject
        []               | "is empty list"
        ["a@b.c", "abc"] | "contains non-email strings"
    }

    void "getSenderName, should return correct sender salutation"() {
        given:
        setupData()
        createUserAndRoles()

        mailHelperService.securityService = Mock(SecurityService) {
            1 * getCurrentUserAsUser() >> {
                User user = getUser(user)
                user.realName = TESTUSER
                return user
            }
        }

        when:
        String sender = ""

        SpringSecurityUtils.doWithAuth(user) {
            sender = mailHelperService.senderName
        }

        then:
        sender == resultName

        where:
        user      || resultName
        OPERATOR  || SENDER_SALUTATION
        ADMIN     || SENDER_SALUTATION
        TESTUSER  || TESTUSER
    }
}
