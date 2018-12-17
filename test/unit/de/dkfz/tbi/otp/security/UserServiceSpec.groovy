package de.dkfz.tbi.otp.security

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Mock([
        Role,
        User,
        UserRole,
])
class UserServiceSpec extends Specification {

    void "createFirstAdminUserIfNoUserExists, if no user exists, create one"() {
        given:
        DomainFactory.createRoleAdminLazy()
        assert User.count == 0

        when:
        UserService.createFirstAdminUserIfNoUserExists()

        then:
        User.count == 1

    }

    void "createFirstAdminUserIfNoUserExists, if user already exists, don't create one"() {
        given:
        DomainFactory.createRoleAdminLazy()
        DomainFactory.createUser()
        assert User.count == 1

        when:
        UserService.createFirstAdminUserIfNoUserExists()

        then:
        User.count == 1
    }

}
