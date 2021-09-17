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

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.util.TimeFormats

/** This table is used externally. Please discuss a change in the team */
class User implements Entity {

    // the AD account
    String username
    String password
    /** This attribute is used externally. Please discuss a change in the team */
    boolean enabled
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired
    /** This attribute is used externally. Please discuss a change in the team */
    String email
    /** This attribute is used externally. Please discuss a change in the team */
    String realName // with format '<first_name> <last_name>'

    boolean acceptedPrivacyPolicy

    // the date a user should be deactivated in OTP
    Date plannedDeactivationDate

    static Closure constraints = {
        username(blank: false, unique: true, nullable: true)
        password(blank: false)
        email(nullable: false, email: true, validator: { val, obj ->
            if (!obj.username && User.findAllByEmailAndUsernameIsNullAndIdNotEqual(val, obj.id)) {
                return 'user.email.unique.extern'
            }
        })
        realName(nullable: true, blank: false)
        plannedDeactivationDate(nullable: true)
    }

    static mapping = {
        table 'users'
        password column: '`password`'
    }

    Set<Role> getAuthorities() {
        return UserRole.findAllByUser(this)*.role as Set
    }

    def beforeInsert() {
        encodePassword()
        return
    }

    def beforeUpdate() {
        if (isDirty('password')) {
            encodePassword()
        }
        return
    }

    protected void encodePassword() {
        // Password is set to a character which does not map to any character in an SHA sum
        // Set to invalid as password is in ldap
        password = "*"
    }

    /**
     * @return User without any security relevant information.
     */
    User sanitizedUser() {
        return new User(id: this.id, username: this.username, email: this.email)
    }

    /**
     * @return Map with information about User without any security relevant information.
     */
    Map sanitizedUserMap() {
        return [id: this.id, username: this.username, email: this.email]
    }

    String getFormattedPlannedDeactivationDate() {
        return TimeFormats.asTimestamp(plannedDeactivationDate)["full"]
    }

    @Override
    String toString() {
        return "${username} (${realName})"
    }
}
