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
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class UserServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                Role,
                User,
                UserRole,
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
}
