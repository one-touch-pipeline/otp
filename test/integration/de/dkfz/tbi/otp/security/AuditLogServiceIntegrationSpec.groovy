package de.dkfz.tbi.otp.security

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import org.joda.time.DateTime
import spock.lang.Specification

class AuditLogServiceIntegrationSpec extends Specification implements UserAndRoles {

    AuditLogService auditLogService
    SpringSecurityService springSecurityService

    void setup() {
        createUserAndRoles()
        springSecurityService = new SpringSecurityService()
        auditLogService = new AuditLogService()
        auditLogService.springSecurityService = springSecurityService
    }

    void "new ActionLogs only get a date and lose their time"() {
        given:
        AuditLog actionLog = null

        when:
        SpringSecurityUtils.doWithAuth(USER) {
            actionLog = auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Test")
        }
        DateTime stamp = new DateTime(actionLog.timestamp)

        then:
        0 == stamp.getHourOfDay()
        0 == stamp.getMinuteOfHour()
        0 == stamp.getSecondOfMinute()
    }

    void "logAction uses the original user, even if switched to another one"() {
        given:
        AuditLog actionLog = null
        User admin = User.findByUsername(ADMIN)

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            doAsSwitchedToUser(USER) {
                actionLog = auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Test")
            }
        }

        then:
        actionLog.user == admin
    }
}
