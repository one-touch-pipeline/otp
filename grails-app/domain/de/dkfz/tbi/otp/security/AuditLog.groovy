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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity

import java.sql.Date
import java.time.LocalDate

@ManagedEntity
class AuditLog implements Entity {

    enum Action {
        PROJECT_USER_CHANGED_ACCESS_TO_OTP,
        PROJECT_USER_CHANGED_ACCESS_TO_FILES,
        PROJECT_USER_CHANGED_MANAGE_USER,
        PROJECT_USER_CHANGED_DELEGATE_MANAGE_USER,
        PROJECT_USER_CHANGED_RECEIVES_NOTIFICATION,
        PROJECT_USER_CHANGED_ENABLED,
        PROJECT_USER_SENT_MAIL,
        PROJECT_USER_CREATED_PROJECT_USER,
        PROJECT_REQUEST,
        LDAP_BASED_CHANGED_ACCESS_TO_FILES,
        /**
         * @deprecated In the beginning permissions were granted via roles. Therefore it was logged. Now the permissions are no longer
         * connected to a role but defined per user. The value is still needed for the data logged before this time. But
         * no new entries with this Action should be created.
         */
        PROJECT_USER_CHANGED_PROJECT_ROLE,
        GRANT_DEPUTY_PI_RIGHTS,
        REVOKE_DEPARTMENT_DEPUTY_RIGHTS,
        CREATED_DEPARTMENT,
        UPDATED_DEPARTMENT,
        DELETED_DEPARTMENT,
    }

    User user

    // suppressing because changing this would involve refactoring the code as well as the database columns
    @SuppressWarnings("GrailsDomainReservedSqlKeywordName")
    Date timestamp = Date.valueOf(LocalDate.now())

    Action action
    String description

    static mapping = {
        description type: "text"
    }

    static constraints = {
        description(nullable: true)
    }

    @Override
    String toString() {
        return "[${timestamp}] ${user}: ${description}"
    }
}
