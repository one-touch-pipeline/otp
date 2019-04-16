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

/**
 * Auto generated class by spring security plugin.
 */
class User implements Entity {

    /**
     * the Ad account
     */
    String username
    String password
    boolean enabled
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired
    String email
    String realName // with format '<first_name> <last_name>'
    String asperaAccount

    boolean acceptedPrivacyPolicy

    static constraints = {
        username(blank: false, unique: true, nullable: true)
        password(blank: false)
        email(nullable: false, unique: true, email: true)
        realName(nullable: true, blank: false)
        asperaAccount(blank: false, nullable: true)
    }

    static mapping = {
        table 'users'
        password column: '`password`'
    }

    Set<Role> getAuthorities() {
        UserRole.findAllByUser(this).collect { it.role } as Set
    }

    def beforeInsert() {
        encodePassword()
    }

    def beforeUpdate() {
        if (isDirty('password')) {
            encodePassword()
        }
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
}
