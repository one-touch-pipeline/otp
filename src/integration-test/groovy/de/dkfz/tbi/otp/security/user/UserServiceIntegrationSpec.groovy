/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.security.user

import grails.gorm.transactions.Rollback
import grails.plugin.springsecurity.SpringSecurityService
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class UserServiceIntegrationSpec extends Specification implements UserAndRoles {
    UserService userService = new UserService()

    void setupData() {
        createUserAndRoles()
    }

    void "getCurrentUserAsUser, simply returns the current user of spring security as a User object"() {
        given:
        User currentUser = DomainFactory.createUser()
        userService = new UserService(
                springSecurityService: Mock(SpringSecurityService) {
                    getCurrentUser() >> currentUser
                }
        )

        expect:
        userService.currentUser == currentUser
    }

    void "test updateEmail valid input"() {
        given:
        setupData()
        User user = CollectionUtils.exactlyOneElement(User.findAllByUsername(TESTUSER))
        String newMail = "dummy@dummy.de"

        when:
        doWithAuth(OPERATOR) {
            userService.updateEmail(user, newMail)
        }

        then:
        user.email == newMail
    }

    void "test updateEmail invalid input"() {
        given:
        setupData()
        User user = CollectionUtils.exactlyOneElement(User.findAllByUsername(TESTUSER))

        when:
        doWithAuth(OPERATOR) {
            userService.updateEmail(user, "")
        }

        then:
        thrown(AssertionError)
    }

    void "test updateRealName valid input"() {
        given:
        setupData()
        User user = CollectionUtils.exactlyOneElement(User.findAllByUsername(TESTUSER))

        when:
        doWithAuth(OPERATOR) {
            userService.updateRealName(user, "newName")
        }

        then:
        user == CollectionUtils.atMostOneElement(User.findAllByRealName("newName"))
        !CollectionUtils.atMostOneElement(User.findAllByRealName("testuser name"))
    }

    void "test updateRealName invalid input no name"() {
        given:
        setupData()
        User user = CollectionUtils.exactlyOneElement(User.findAllByUsername(TESTUSER))

        when:
        doWithAuth(OPERATOR) {
            userService.updateRealName(user, "")
        }

        then:
        thrown(AssertionError)
    }

    void "getRolesOfCurrentUser, return the role of the current user"() {
        given:
        setupData()
        User user = CollectionUtils.exactlyOneElement(User.findAllByUsername(username))
        List<Role> roles

        when:
        roles = doWithAuth(username) {
            userService.rolesOfCurrentUser
        }

        then:
        user.authorities == roles as Set

        where:
        username << [
                OPERATOR,
                ADMIN,
                TESTUSER,
        ]
    }

    void "checkRolesContainsAdministrativeRole, check if roles includes an administrative role"() {
        given:
        setupData()
        List<Role> roles = authorities ? Role.findAllByAuthorityInList(authorities) : []

        when:
        boolean check = userService.checkRolesContainsAdministrativeRole(roles)

        then:
        check == expectedValue

        where:
        expectedValue | authorities
        true          | [Role.ROLE_ADMIN, Role.ROLE_OPERATOR, Role.ROLE_SWITCH_USER, Role.ROLE_SWITCH_USER]
        true          | [Role.ROLE_SWITCH_USER, Role.ROLE_OPERATOR, Role.ROLE_SWITCH_USER]
        false         | [Role.ROLE_SWITCH_USER]
        false         | []
    }

    void "hasCurrentUserAdministrativeRoles, check if current user has an administrative role"() {
        given:
        setupData()
        boolean check

        when:
        check = doWithAuth(username) {
            userService.hasCurrentUserAdministrativeRoles()
        }

        then:
        check == expectedValue

        where:
        username | expectedValue
        OPERATOR | true
        ADMIN    | true
        TESTUSER | false
    }
}
