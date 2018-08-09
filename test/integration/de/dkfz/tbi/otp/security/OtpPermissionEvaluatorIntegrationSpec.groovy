package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import org.springframework.security.authentication.*
import org.springframework.security.core.*
import spock.lang.*

class OtpPermissionEvaluatorIntegrationSpec extends Specification implements UserAndRoles {

    OtpPermissionEvaluator permissionEvaluator
    Authentication authentication
    User user
    Project project
    UserProjectRole userProjectRole

    ProjectService projectService

    void setup() {
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
        userProjectRole.accessToOtp = otpAccess

        when:
        List<Project> resultList = SpringSecurityUtils.doWithAuth(user.username) {
            projectService.getAllProjects()
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
        user = User.findByUsername(OPERATOR)

        when:
        List<Project> resultList = SpringSecurityUtils.doWithAuth(user.username) {
            projectService.getAllProjects()
        }

        then:
        resultList == [project]
    }

    void "hasPermission, aclPermissionEvaluator throws exceptions on unsupported permissions"() {
        when:
        permissionEvaluator.hasPermission(authentication, project, permission)

        then:
        thrown(IllegalArgumentException)

        where:
        permission          |_
        'unknownPermission' |_
        true                |_
    }

    void "hasPermission, string permission but unknown target domain results in false"() {
        when:
        boolean access = permissionEvaluator.hasPermission(authentication, DomainFactory.createSeqType(), 'MANAGE_USERS')

        then:
        !access
        0 * permissionEvaluator.aclPermissionEvaluator.hasPermission(_,_,_)
    }

    void "hasPermission, missing user for project role permissions returns false"() {
        given:
        authentication = new UsernamePasswordAuthenticationToken(new Principal(username: "unknownUsername"), null, [])

        when:
        boolean checkResult = permissionEvaluator.hasPermission(authentication, DomainFactory.createProject(), "OTP_READ_ACCESS")

        then:
        !checkResult
        0 * permissionEvaluator.aclPermissionEvaluator.hasPermission(_,_,_)
    }

    void "hasPermission, missing userProjectRole for project role permissions returns false"() {
        when:
        boolean checkResult = permissionEvaluator.hasPermission(authentication, DomainFactory.createProject(), "MANAGE_USERS")

        then:
        !checkResult
    }

    void "hasPermission, disabled user for project role permissions returns false"() {
        given:
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

    void "hasPermission, test conditions for project role permissions"() {
        given:
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
        false       | false       | true                   | "MANAGE_USERS"             || true
        true        | false       | true                   | "MANAGE_USERS"             || true
        false       | false       | true                   | "DELEGATE_USER_MANAGEMENT" || true
    }

    @Unroll
    void "hasPermission, different combinations for ADD_USER permission (access = #access)"() {
        given:
        userProjectRole.manageUsersAndDelegate = manageUsersAndDelegate
        userProjectRole.manageUsers = manageUsers

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
}
