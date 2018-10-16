package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.utils.Entity

import java.sql.Date
import java.time.LocalDate

class AuditLog implements Entity {

    enum Action {
        PROJECT_USER_CHANGED_ACCESS_TO_OTP,
        PROJECT_USER_CHANGED_ACCESS_TO_FILES,
        PROJECT_USER_CHANGED_MANAGE_USER,
        PROJECT_USER_CHANGED_DELEGATE_MANAGE_USER,
        PROJECT_USER_CHANGED_RECEIVES_NOTIFICATION,
        PROJECT_USER_CHANGED_ENABLED,
        PROJECT_USER_SENT_MAIL,
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
