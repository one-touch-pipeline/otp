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

import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.exceptions.RightsNotGrantedException

class PIUserServiceSpec extends Specification implements DataTest, UserDomainFactory {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                User,
                PIUser,
                Department,
        ]
    }

    void "cleanUpPIUser, when PI is still department head, keep deputy, then remove Department, remove deputy"() {
        given:
        User departmentHead = createUser()
        Department department = createDepartment([departmentHead: departmentHead])
        createPIUser([pi: departmentHead, dateRightsGranted: new Date()])

        when:
        new PIUserService().cleanUpPIUser()

        then:
        PIUser.all.size() == 1

        when: 'remove Department'
        department.delete(flush: true)
        new PIUserService().cleanUpPIUser()

        then:
        PIUser.all.size() == 0
    }

    void "grantDeputyPIRights, when PI is in the list of allowed users"() {
        given:
        User departmentHead = createUser()
        createDepartment([departmentHead: departmentHead])
        User deputyPI = createUser()

        when:
        new PIUserService().grantDeputyPIRights(departmentHead, deputyPI)

        then:
        PIUser.findAllByDeputyPI(deputyPI).size() == 1
    }

    void "grantDeputyPIRights, when PI is NOT in the list of allowed users"() {
        given:
        User user = createUser()
        User anotherHead = createUser()
        createDepartment([departmentHead: anotherHead])
        User deputyPI = createUser()

        when:
        new PIUserService().grantDeputyPIRights(user, deputyPI)

        then:
        thrown(RightsNotGrantedException)
    }

    void "revokeDeputyPIRights, when called with departmentHead and deputyPI"() {
        given:
        User departmentHead = createUser()
        createDepartment([departmentHead: departmentHead])
        User deputyPI = createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        new PIUserService().revokeDeputyPIRights(departmentHead, deputyPI)

        then:
        PIUser.findAllByPiAndDeputyPI(departmentHead, deputyPI).size() == 0
    }

    void "revokeDeputyPIRights, when called with deputyPI"() {
        given:
        User departmentHead = createUser()
        User deputyPI = createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        new PIUserService().revokeDeputyPIRights(deputyPI)

        then:
        PIUser.findAllByDeputyPI(deputyPI).size() == 0
    }

    void "revokeDeputyPIRightsForHead, when called with departmentHead"() {
        given:
        User departmentHead = createUser()
        User deputyPI = createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        new PIUserService().revokeDeputyPIRightsForHead(departmentHead)

        then:
        PIUser.findAllByPi(departmentHead).size() == 0
    }

    void "isPI, returns true when user is the head of department"() {
        given:
        User departmentHead = createUser()
        createDepartment([departmentHead: departmentHead])

        expect:
        new PIUserService().isPI(departmentHead)
    }

    void "isPI, returns true when user is the deputy PI"() {
        given:
        User departmentHead = createUser()
        User deputyPI = createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        expect:
        new PIUserService().isPI(deputyPI)
    }

    void "isPI, returns false when user is neither department head nor deputy PI"() {
        given:
        User user = createUser()

        expect:
        !new PIUserService().isPI(user)
    }
}
