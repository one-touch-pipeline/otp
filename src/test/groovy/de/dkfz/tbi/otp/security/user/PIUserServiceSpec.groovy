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

    PIUserService piUserService

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
        piUserService = new PIUserService(auditLogService: Mock(AuditLogService),)
        User departmentHead = createUser()
        Department department = createDepartment([departmentHeads: [departmentHead, createUser()]])
        createPIUser([pi: departmentHead, dateRightsGranted: new Date()])

        when:
        piUserService.cleanUpPIUser()

        then:
        PIUser.all.size() == 1

        when: 'remove Department'
        department.delete(flush: true)
        piUserService.cleanUpPIUser()

        then:
        PIUser.all.size() == 0
    }

    void "grantDeputyPIRights, when PI is in the list of allowed users"() {
        given:
        UserService userService = Mock(UserService)
        piUserService = new PIUserService(
                auditLogService: Mock(AuditLogService),
                userService: userService,
        )
        User departmentHead = createUser()
        User deputyPI = createUser()

        when:
        piUserService.grantDeputyPIRights(departmentHead, deputyPI)

        then:
        1 * piUserService.auditLogService.logAction(_, _) >> _
        1 * userService.isDepartmentHead(departmentHead) >> true
        PIUser.findAllByDeputyPI(deputyPI).size() == 1
    }

    void "grantDeputyPIRights, when PI is NOT in the list of allowed users"() {
        given:
        UserService userService = Mock(UserService)
        piUserService = new PIUserService(auditLogService: Mock(AuditLogService), userService: userService)
        User user = createUser()
        User deputyPI = createUser()

        when:
        piUserService.grantDeputyPIRights(user, deputyPI)

        then:
        1 * userService.isDepartmentHead(user) >> false
        thrown(RightsNotGrantedException)
    }

    void "revokeDeputyPIRights, when called with departmentHead and deputyPI"() {
        given:
        piUserService = new PIUserService(auditLogService: Mock(AuditLogService),)
        User departmentHead = createUser()
        createDepartment([departmentHeads: [departmentHead, createUser()]])
        User deputyPI = createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        piUserService.revokeDeputyPIRights(departmentHead, deputyPI)

        then:
        1 * piUserService.auditLogService.logAction(_, _) >> _
        PIUser.findAllByPiAndDeputyPI(departmentHead, deputyPI).size() == 0
    }

    void "revokeDeputyPIRights, when called with deputyPI"() {
        given:
        piUserService = new PIUserService(auditLogService: Mock(AuditLogService),)
        User departmentHead = createUser()
        User deputyPI = createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        piUserService.revokeDeputyPIRights(deputyPI)

        then:
        1 * piUserService.auditLogService.logAction(_, _) >> _
        PIUser.findAllByDeputyPI(deputyPI).size() == 0
    }

    void "revokeDeputyPIRightsForHead, when called with departmentHead"() {
        given:
        piUserService = new PIUserService(auditLogService: Mock(AuditLogService),)
        User departmentHead = createUser()
        User deputyPI = createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        when:
        piUserService.revokeDeputyPIRightsForHead(departmentHead)

        then:
        PIUser.findAllByPi(departmentHead).size() == 0
    }

    void "isPI, returns true when user is the head of department"() {
        given:
        UserService userService = Mock(UserService) {
            1 * isDepartmentHead(_) >> true
        }
        piUserService = new PIUserService(userService: userService)
        User departmentHead = createUser()

        expect:
        piUserService.isPI(departmentHead)
    }

    void "isPI, returns true when user is the deputy PI"() {
        given:
        UserService userService = Mock(UserService) {
            1 * isDepartmentHead(_) >> false
        }
        piUserService = new PIUserService(userService: userService)
        User departmentHead = createUser()
        User deputyPI = createUser()
        createPIUser([pi: departmentHead, deputyPI: deputyPI, dateRightsGranted: new Date()])

        expect:
        piUserService.isPI(deputyPI)
    }

    void "isPI, returns false when user is neither department head nor deputy PI"() {
        given:
        UserService userService = Mock(UserService) {
            1 * isDepartmentHead(_) >> false
        }
        piUserService = new PIUserService(userService: userService)
        User user = createUser()

        expect:
        !piUserService.isPI(user)
    }
}
