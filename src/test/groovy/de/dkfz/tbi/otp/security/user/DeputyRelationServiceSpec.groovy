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

class DeputyRelationServiceSpec extends Specification implements DataTest, UserDomainFactory {

    DeputyRelationService deputyRelationService

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                User,
                DeputyRelation,
                Department,
        ]
    }

    void "cleanUpDeputyRelations, when granting user is still department head, keep deputy, when remove Department, remove deputy"() {
        given:
        deputyRelationService = new DeputyRelationService(auditLogService: Mock(AuditLogService),)
        User departmentHead = createUser()
        Department department = createDepartment([departmentHeads: [departmentHead, createUser()]])
        createDeputyRelation([grantingDeputyUser: departmentHead, dateDeputyGranted: new Date()])

        when:
        deputyRelationService.cleanUpDeputyRelations()

        then:
        DeputyRelation.all.size() == 1

        when: 'remove Department'
        department.delete(flush: true)
        deputyRelationService.cleanUpDeputyRelations()

        then:
        DeputyRelation.all.size() == 0
    }

    void "grantDepartmentDeputyRights, when granting user is in the list of allowed users"() {
        given:
        UserService userService = Mock(UserService)
        deputyRelationService = new DeputyRelationService(
                auditLogService: Mock(AuditLogService),
                userService: userService,
        )
        User departmentHead = createUser()
        User deputyUser = createUser()

        when:
        deputyRelationService.grantDepartmentDeputyRights(departmentHead, deputyUser)

        then:
        1 * deputyRelationService.auditLogService.logAction(_, _) >> _
        1 * userService.isDepartmentHead(departmentHead) >> true
        DeputyRelation.findAllByDeputyUser(deputyUser).size() == 1
    }

    void "grantDepartmentDeputyRights, when granting user is NOT in the list of allowed users"() {
        given:
        UserService userService = Mock(UserService)
        deputyRelationService = new DeputyRelationService(auditLogService: Mock(AuditLogService), userService: userService)
        User grantingUser = createUser()
        User deputyUser = createUser()

        when:
        deputyRelationService.grantDepartmentDeputyRights(grantingUser, deputyUser)

        then:
        1 * userService.isDepartmentHead(grantingUser) >> false
        thrown(RightsNotGrantedException)
    }

    void "revokeDepartmentDeputyRights, when called with departmentHead and deputyUser"() {
        given:
        deputyRelationService = new DeputyRelationService(auditLogService: Mock(AuditLogService),)
        User departmentHead = createUser()
        createDepartment([departmentHeads: [departmentHead, createUser()]])
        User deputyUser = createUser()
        createDeputyRelation([grantingDeputyUser: departmentHead, deputyUser: deputyUser, dateDeputyGranted: new Date()])

        when:
        deputyRelationService.revokeDepartmentDeputyRights(departmentHead, deputyUser)

        then:
        1 * deputyRelationService.auditLogService.logAction(_, _) >> _
        DeputyRelation.findAllByGrantingDeputyUserAndDeputyUser(departmentHead, deputyUser).size() == 0
    }

    void "revokeDepartmentDeputyRights, when called with deputyUser"() {
        given:
        deputyRelationService = new DeputyRelationService(auditLogService: Mock(AuditLogService),)
        User departmentHead = createUser()
        User deputyUser = createUser()
        createDeputyRelation([grantingDeputyUser: departmentHead, deputyUser: deputyUser, dateDeputyGranted: new Date()])

        when:
        deputyRelationService.revokeDepartmentDeputyRights(deputyUser)

        then:
        1 * deputyRelationService.auditLogService.logAction(_, _) >> _
        DeputyRelation.findAllByDeputyUser(deputyUser).size() == 0
    }

    void "revokedeputyUserRightsForHead, when called with departmentHead"() {
        given:
        deputyRelationService = new DeputyRelationService(auditLogService: Mock(AuditLogService),)
        User departmentHead = createUser()
        User deputyUser = createUser()
        createDeputyRelation([grantingDeputyUser: departmentHead, deputyUser: deputyUser, dateDeputyGranted: new Date()])

        when:
        deputyRelationService.revokeAllDeputiesForDepartmentHead(departmentHead)

        then:
        1 * deputyRelationService.auditLogService.logAction(_, _) >> _
        DeputyRelation.findAllByGrantingDeputyUser(departmentHead).size() == 0
    }

    void "hasDepartmentRights, returns true when user is the head of department"() {
        given:
        UserService userService = Mock(UserService) {
            1 * isDepartmentHead(_) >> true
        }
        deputyRelationService = new DeputyRelationService(userService: userService)
        User departmentHead = createUser()

        expect:
        deputyRelationService.hasDepartmentRights(departmentHead)
    }

    void "hasDepartmentRights, returns true when user has granted deputy rights"() {
        given:
        UserService userService = Mock(UserService) {
            1 * isDepartmentHead(_) >> false
        }
        deputyRelationService = new DeputyRelationService(userService: userService)
        User departmentHead = createUser()
        User deputyUser = createUser()
        createDeputyRelation([grantingDeputyUser: departmentHead, deputyUser: deputyUser, dateDeputyGranted: new Date()])

        expect:
        deputyRelationService.hasDepartmentRights(deputyUser)
    }

    void "hasDepartmentRights, returns false when user is neither department head nor granted deputy rights"() {
        given:
        UserService userService = Mock(UserService) {
            1 * isDepartmentHead(_) >> false
        }
        deputyRelationService = new DeputyRelationService(userService: userService)
        User user = createUser()

        expect:
        !deputyRelationService.hasDepartmentRights(user)
    }
}
