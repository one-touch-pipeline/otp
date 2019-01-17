package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.security.*
import grails.plugin.springsecurity.*
import spock.lang.*

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
