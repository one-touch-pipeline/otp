package de.dkfz.tbi.otp.security

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils

import de.dkfz.tbi.otp.security.AuditLog.Action

class AuditLogService {

    SpringSecurityService springSecurityService

    private AuditLog createActionLog(User user, Action action, String description) {
        AuditLog actionLog = new AuditLog(
                user: user,
                action: action,
                description: description,
        )
        actionLog.save(flush: true)
        return actionLog
    }

    AuditLog logAction(Action action, String description) {
        String username = springSecurityService.authentication.principal.username
        if (SpringSecurityUtils.isSwitched()) {
            username = SpringSecurityUtils.getSwitchedUserOriginalUsername()
        }
        return createActionLog(
                User.findByUsername(username),
                action,
                description
        )
    }

}
