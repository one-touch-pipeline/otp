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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.exceptions.RightsNotGrantedException

class UserServiceSpec extends Specification implements DataTest, DomainFactoryCore, UserDomainFactory {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                Role,
                User,
                UserRole,
                UserProjectRole,
                PIUser,
                Department,
        ]
    }

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

    void "getAllUsersInAdList"() {
        given:
        DomainFactory.createUser()
        User user1 = DomainFactory.createUser()
        DomainFactory.createUser()
        User user2 = DomainFactory.createUser()
        DomainFactory.createUser()

        List<String> names = [
                "user ${nextId}",
                user1.username,
                "user ${nextId}",
                user2.username,
                "user ${nextId}",
        ]

        when:
        Set<String> users = new UserService().getAllUserNamesOfOtpUsers(names)

        then:
        TestCase.assertContainSame([user1, user2]*.username, users)
    }

    void "grantDeputyPIRights, when PI is in the list of allowed users"() {
        given:
        User departmentHead = DomainFactory.createUser()
        createDepartment([departmentHead: departmentHead])
        User deputyPI = DomainFactory.createUser()

        when:
        new UserService().grantDeputyPIRights(departmentHead, deputyPI)

        then:
        PIUser.findAllByDeputyPI(deputyPI).size() == 1
    }

    void "grantDeputyPIRights, when PI is NOT in the list of allowed users"() {
        given:
        User user = DomainFactory.createUser()
        User anotherHead = DomainFactory.createUser()
        createDepartment([departmentHead: anotherHead])
        User deputyPI = DomainFactory.createUser()

        when:
        new UserService().grantDeputyPIRights(user, deputyPI)

        then:
        thrown(RightsNotGrantedException)
    }

    void "revokeDeputyPIRights, when called with departmentHead and deputyPI"() {
        given:
        User departmentHead = DomainFactory.createUser()
        createDepartment([departmentHead: departmentHead])
        User deputyPI = DomainFactory.createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        new UserService().revokeDeputyPIRights(departmentHead, deputyPI)

        then:
        PIUser.findAllByPiAndDeputyPI(departmentHead, deputyPI).size() == 0
    }

    void "revokeDeputyPIRights, when called with deputyPI"() {
        given:
        User departmentHead = DomainFactory.createUser()
        User deputyPI = DomainFactory.createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        new UserService().revokeDeputyPIRights(deputyPI)

        then:
        PIUser.findAllByDeputyPI(deputyPI).size() == 0
    }

    void "revokeDeputyPIRightsForHead, when called with departmentHead"() {
        given:
        User departmentHead = DomainFactory.createUser()
        User deputyPI = DomainFactory.createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        new UserService().revokeDeputyPIRightsForHead(departmentHead)

        then:
        PIUser.findAllByPi(departmentHead).size() == 0
    }

    void "isPI, returns true when user is the head of department"() {
        given:
        User departmentHead = DomainFactory.createUser()
        createDepartment([departmentHead: departmentHead])

        expect:
        new UserService().isPI(departmentHead)
    }

    void "isPI, returns true when user is the deputy PI"() {
        given:
        User departmentHead = DomainFactory.createUser()
        User deputyPI = DomainFactory.createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        expect:
        new UserService().isPI(deputyPI)
    }

    void "isPI, returns false when user is neither department head nor deputy PI"() {
        given:
        User user = DomainFactory.createUser()

        expect:
        !new UserService().isPI(user)
    }
}
