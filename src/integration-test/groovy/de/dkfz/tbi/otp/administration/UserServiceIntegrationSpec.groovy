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
import spock.lang.Specification

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class UserServiceIntegrationSpec extends Specification implements UserAndRoles {
    UserService userService = new UserService()

    def setup() {
        createUserAndRoles()
    }

    void "test updateEmail valid input"() {
        given:
        User user = User.findByUsername(TESTUSER)
        String newMail = "dummy@dummy.de"

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userService.updateEmail(user, newMail)
        }

        then:
        user.email == newMail
    }

    void "test updateEmail invalid input"() {
        given:
        User user = User.findByUsername(TESTUSER)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userService.updateEmail(user, "")
        }

        then:
        thrown(AssertionError)
    }

    void "test updateAsperaAccount valid input"() {
        given:
        User user = User.findByUsername(TESTUSER)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userService.updateAsperaAccount(user, "newUser")
        }

        then:
        user.asperaAccount == "newUser"
    }

    void "test updateRealName valid input"() {
        given:
        User user = User.findByUsername(TESTUSER)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userService.updateRealName(user, "newName")
        }

        then:
        user == User.findByRealName("newName")
        !User.findByRealName("testuser name")
    }

    void "test updateRealName invalid input no name"() {
        given:
        User user = User.findByUsername(TESTUSER)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userService.updateRealName(user, "")
        }

        then:
        thrown(AssertionError)
    }
}
