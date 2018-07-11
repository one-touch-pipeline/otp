package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.utils.Entity

import java.sql.Date
import java.time.LocalDate

class AuditLog implements Entity {

    enum Action {
        PROJECT_USER_CHANGED_MANAGE_USER,
        PROJECT_USER_CHANGED_ENABLED,
        PROJECT_USER_SENT_MAIL,
        PROJECT_USER_CHANGED_PROJECT_ROLE,
    }

    User user
    Date timestamp = Date.valueOf(LocalDate.now())
    Action action
    String description

    static mapping = {
        description type: "text"
    }

    static constraints = {
        description(nullable: true)
    }
}
