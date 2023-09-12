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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Principal

@Rollback
@Integration
class ProjectPermissionEvaluatorIntegrationSpec extends Specification implements UserAndRoles, UserDomainFactory {

    @Autowired
    ProjectPermissionEvaluator permissionEvaluator

    Authentication authentication
    User user
    Project project
    UserProjectRole userProjectRole

    ProjectService projectService

    void setupData() {
        createUserAndRoles()
        user = createUser()
        project = createProject()
        userProjectRole = createUserProjectRole(
                user: user,
                project: project,
                manageUsers: true,
        )
        authentication = new UsernamePasswordAuthenticationToken(new Principal(user.username, [], user.enabled), null, [])
    }

    void "test evaluation of hasPermission in annotation (otpAccess = true)"() {
        given:
        setupData()

        userProjectRole.accessToOtp = true
        userProjectRole.save(flush: true)

        expect:
        project == doWithAuth(user.username) {
            projectService.getProject(project.id)
        }
    }

    void "test evaluation of hasPermission in annotation (otpAccess = false)"() {
        given:
        setupData()

        userProjectRole.accessToOtp = false
        userProjectRole.save(flush: true)

        when:
        doWithAuth(user.username) {
            projectService.getProject(project.id)
        }

        then:
        thrown(AccessDeniedException)
    }

    void "test evaluation of hasRole in annotation"() {
        given:
        setupData()

        user = CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR))

        expect:
        project == doWithAuth(user.username) {
            projectService.getProject(project.id)
        }
    }

    void "hasPermission, unsupported permissions return false"() {
        given:
        setupData()

        when:
        boolean result = permissionEvaluator.hasPermission(authentication, project, permission)

        then:
        !result

        where:
        permission          | _
        'unknownPermission' | _
        true                | _
    }

    void "hasPermission, string permission but unknown target domain results in false"() {
        given:
        setupData()

        when:
        boolean access = permissionEvaluator.hasPermission(authentication, createSeqType(), 'MANAGE_USERS')

        then:
        !access
    }

    void "hasPermission, missing user for project role permissions returns false"() {
        given:
        setupData()

        authentication = new UsernamePasswordAuthenticationToken(new Principal("unknownUsername", [], true), null, [])

        when:
        boolean checkResult = permissionEvaluator.hasPermission(authentication, createProject(), "OTP_READ_ACCESS")

        then:
        !checkResult
    }

    void "hasPermission, missing userProjectRole for project role permissions returns false"() {
        given:
        setupData()

        when:
        boolean checkResult = permissionEvaluator.hasPermission(authentication, createProject(), "MANAGE_USERS")

        then:
        !checkResult
    }

    void "hasPermission, disabled user for project role permissions returns false"() {
        given:
        setupData()

        user.enabled = enabledUser
        userProjectRole.enabled = enabledUserProjectRole

        when:
        boolean checkResult = permissionEvaluator.hasPermission(authentication, project, "MANAGE_USERS")

        then:
        checkResult == access

        where:
        enabledUser | enabledUserProjectRole || access
        false       | false                  || false
        false       | true                   || false
        true        | false                  || false
        true        | true                   || true
    }

    @Unroll
    void "hasPermission, test conditions for project role permissions"() {
        given:
        setupData()

        userProjectRole.manageUsers = manageUsers
        userProjectRole.accessToOtp = accessToOtp
        userProjectRole.manageUsersAndDelegate = manageUsersAndDelegate

        when:
        boolean checkResult = permissionEvaluator.checkProjectRolePermission(authentication, project, permission)

        then:
        checkResult == access

        where:
        manageUsers | accessToOtp | manageUsersAndDelegate | permission                 || access
        false       | false       | false                  | "OTP_READ_ACCESS"          || false
        false       | true        | false                  | "OTP_READ_ACCESS"          || true
        true        | false       | false                  | "MANAGE_USERS"             || true
        false       | false       | true                   | "MANAGE_USERS"             || false
        true        | false       | true                   | "MANAGE_USERS"             || true
        false       | false       | true                   | "DELEGATE_USER_MANAGEMENT" || true
    }

    @Unroll
    void "hasPermission, test conditions for project request role permissions"() {
        given:
        setupData()
        ProjectRequest projectRequest = createProjectRequest([:], [
                currentOwner            : userIsCurrentOwner ? user : createUser(),
                usersThatNeedToApprove  : userNeedsToApprove ? [user, createUser()] : [createUser()],
                usersThatAlreadyApproved: userAlreadyApproved ? [user, createUser()] : [createUser()],
        ])

        when:
        boolean checkResult = permissionEvaluator.checkProjectRequestRolePermission(authentication, projectRequest, permission)

        then:
        checkResult == access

        where:
        userNeedsToApprove | userAlreadyApproved | userIsCurrentOwner | permission                      || access
        true               | false               | false              | "PROJECT_REQUEST_NEEDED_PIS"    || true
        false              | true                | false              | "PROJECT_REQUEST_NEEDED_PIS"    || false
        true               | true                | false              | "PROJECT_REQUEST_CURRENT_OWNER" || false
        false              | true                | true               | "PROJECT_REQUEST_CURRENT_OWNER" || true
        true               | false               | false              | "PROJECT_REQUEST_PI"            || true
        false              | true                | false              | "PROJECT_REQUEST_PI"            || true
        false              | false               | false              | "PROJECT_REQUEST_PI"            || false
    }

    @Unroll
    void "hasPermission, different combinations for ADD_USER permission (access = #access)"() {
        given:
        setupData()

        userProjectRole.manageUsersAndDelegate = manageUsersAndDelegate
        userProjectRole.manageUsers = manageUsers
        userProjectRole.save(flush: true)

        when:
        boolean checkResult = permissionEvaluator.hasPermission(authentication, null, "ADD_USER")

        then:
        checkResult == access

        where:
        manageUsers | manageUsersAndDelegate || access
        false       | false                  || false
        true        | false                  || true
        false       | true                   || true
        true        | true                   || true
    }

    @Unroll
    void "hasPermission, ADD_USER only considers enabled UserProjectRole entries (enabled=#enabled)"() {
        given:
        setupData()

        userProjectRole.enabled = enabled
        3.times {
            DomainFactory.createUserProjectRole(user: userProjectRole.user, manageUsersAndDelegate: true, enabled: false)
        }

        expect:
        expected == permissionEvaluator.hasPermission(authentication, null, "ADD_USER")

        where:
        enabled || expected
        true    || true
        false   || false
    }

    void "hasPermission, ADD_USER takes all enabled UserProjectRoles of the user into account"() {
        given:
        setupData()

        3.times {
            DomainFactory.createUserProjectRole(user: userProjectRole.user, manageUsersAndDelegate: true, enabled: true)
        }

        expect:
        permissionEvaluator.hasPermission(authentication, null, "ADD_USER")
    }

    void "hasPermission, IS_USER takes the according user of the UserProjectRole into account"() {
        given:
        setupData()

        expect:
        permissionEvaluator.hasPermission(authentication, userProjectRole, "IS_USER")
    }

    void "hasPermission, IS_USER restricts for different users then the UserProjectRoles user"() {
        given:
        setupData()
        User otherUser = createUser()
        UserProjectRole otherUserProjectRole = createUserProjectRole(
                user: otherUser
        )

        expect:
        !permissionEvaluator.hasPermission(authentication, otherUserProjectRole, "IS_USER")
    }

    void "hasPermission, IS_DEPARTMENT_HEAD allows permission when user is head of an department"() {
        given:
        setupData()
        createDepartment([
                departmentHeads: [user, createUser()],
        ])
        createDepartment()

        expect:
        permissionEvaluator.hasPermission(authentication, user, "IS_DEPARTMENT_HEAD")
    }

    void "hasPermission, IS_DEPARTMENT_HEAD restricts when user is not head of any department"() {
        given:
        setupData()

        expect:
        !permissionEvaluator.hasPermission(authentication, user, "IS_DEPARTMENT_HEAD")
    }

    void "hasPermission, IS_DEPARTMENT_HEAD restricts when authenticated user is not passed user"() {
        given:
        setupData()
        User otherUser = createUser()
        createDepartment([
                departmentHeads: [otherUser, createUser()],
        ])

        expect:
        !permissionEvaluator.hasPermission(authentication, otherUser, "IS_DEPARTMENT_HEAD")
    }

    void "hasPermission, IS_DEPARTMENT_HEAD restricts when authenticated user is not head of any department"() {
        given:
        setupData()

        expect:
        !permissionEvaluator.hasPermission(authentication, null, "IS_DEPARTMENT_HEAD")
    }
}
