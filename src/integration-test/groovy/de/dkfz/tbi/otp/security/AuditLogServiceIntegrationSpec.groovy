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
package de.dkfz.tbi.otp.security

import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationTrustResolver
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.time.*

@Rollback
@Integration
class AuditLogServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore, UserDomainFactory {

    @Autowired
    GrailsApplication grailsApplication

    AuditLogService auditLogService

    void setupData() {
        createUserAndRoles()
        auditLogService = new AuditLogService([
                processingOptionService: new ProcessingOptionService()
        ])
        auditLogService.securityService = new SecurityService()
        auditLogService.securityService.springSecurityService = new SpringSecurityService()
        auditLogService.securityService.springSecurityService.grailsApplication = grailsApplication
        auditLogService.securityService.springSecurityService.authenticationTrustResolver = Mock(AuthenticationTrustResolver) {
            isAnonymous(_) >> false
        }
    }

    void "new ActionLogs only get a date and lose their time"() {
        given:
        setupData()

        AuditLog actionLog = null

        when:
        SpringSecurityUtils.doWithAuth(USER) {
            actionLog = auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Test")
        }
        ZonedDateTime stamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(actionLog.timestamp.time), ZoneId.systemDefault())

        then:
        stamp.hour == 0
        stamp.minute == 0
        stamp.second == 0
    }

    void "logAction uses the original user, even if switched to another one"() {
        given:
        setupData()

        AuditLog actionLog = null
        User admin = CollectionUtils.exactlyOneElement(User.findAllByUsername(ADMIN))

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            doAsSwitchedToUser(USER) {
                actionLog = auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Test")
            }
        }

        then:
        actionLog.user == admin
    }

    void "logActionWithSystemUser, when log a message, then the AuditLog should use the user defined in the ProcessingOption"() {
        given:
        setupData()
        User user = createUser()
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, user.username)
        AuditLog actionLog

        when:
        SpringSecurityUtils.doWithAuth(USER) {
            actionLog = auditLogService.logActionWithSystemUser(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Test")
        }

        then:
        actionLog.user == user
    }
}
