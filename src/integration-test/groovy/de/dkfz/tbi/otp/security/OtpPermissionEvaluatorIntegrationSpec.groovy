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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.Principal

@Rollback
@Integration
class OtpPermissionEvaluatorIntegrationSpec extends Specification implements UserAndRoles {

    OtpPermissionEvaluator permissionEvaluator
    Authentication authentication
    User user
    Project project
    UserProjectRole userProjectRole

    ProjectService projectService

    void setupData() {
        createUserAndRoles()
        user = DomainFactory.createUser()
        project = DomainFactory.createProject()
        userProjectRole = DomainFactory.createUserProjectRole(
                user: user,
                project: project,
                manageUsers: true,
        )
        authentication = new UsernamePasswordAuthenticationToken(new Principal(username: user.username), null, [])
    }

    @Unroll
    void "test evaluation of hasPermission in annotation (otpAccess = #otpAccess)"() {
        given:
        setupData()

        userProjectRole.accessToOtp = otpAccess

        when:
        List<Project> resultList = SpringSecurityUtils.doWithAuth(user.username) {
            projectService.allProjects
        }

        then:
        resultList.size() == expectedSize

        where:
        otpAccess || expectedSize
        false     || 0
        true      || 1
    }

    void "pass annotation via userrole"() {
        given:
        setupData()

        user = User.findByUsername(OPERATOR)

        when:
        List<Project> resultList = SpringSecurityUtils.doWithAuth(user.username) {
            projectService.allProjects
        }

        then:
        resultList == [project]
    }

    void "hasPermission, aclPermissionEvaluator throws exceptions on unsupported permissions"() {
        given:
        setupData()

        when:
        permissionEvaluator.hasPermission(authentication, project, permission)

        then:
        thrown(IllegalArgumentException)

        where:
        permission          | _
        'unknownPermission' | _
        true                | _
    }

    void "hasPermission, string permission but unknown target domain results in false"() {
        given:
        setupData()

        when:
        boolean access = permissionEvaluator.hasPermission(authentication, DomainFactory.createSeqType(), 'MANAGE_USERS')

        then:
        !access
        0 * permissionEvaluator.aclPermissionEvaluator.hasPermission(_, _, _)
    }

    void "hasPermission, missing user for project role permissions returns false"() {
        given:
        setupData()

        authentication = new UsernamePasswordAuthenticationToken(new Principal(username: "unknownUsername"), null, [])

        when:
        boolean checkResult = permissionEvaluator.hasPermission(authentication, DomainFactory.createProject(), "OTP_READ_ACCESS")

        then:
        !checkResult
        0 * permissionEvaluator.aclPermissionEvaluator.hasPermission(_, _, _)
    }

    void "hasPermission, missing userProjectRole for project role permissions returns false"() {
        given:
        setupData()

        when:
        boolean checkResult = permissionEvaluator.hasPermission(authentication, DomainFactory.createProject(), "MANAGE_USERS")

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
}
