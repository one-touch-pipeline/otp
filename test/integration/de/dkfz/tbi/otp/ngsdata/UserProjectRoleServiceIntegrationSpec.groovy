package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.testing.*
import grails.plugin.springsecurity.*
import spock.lang.*

class UserProjectRoleServiceIntegrationSpec extends Specification implements UserAndRoles {
    UserProjectRoleService userProjectRoleService = new UserProjectRoleService()

    def setup() {
        createUserAndRoles()
    }

    void "test manageUsers is removed and not granted when updating the project role"() {
        given:
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: User.findByUsername(USER),
                projectRole: DomainFactory.createProjectRole(accessToOtp: true),
                manageUsers: manageUsers,
        )
        ProjectRole newRole = DomainFactory.createProjectRole(
                accessToOtp: true,
                manageUsersAndDelegate: manageUsersAndDelegate,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.updateProjectRole(userProjectRole, newRole)
        }
        userProjectRole.projectRole.manageUsersAndDelegate = false

        then:
        !userProjectRole.manageUsers

        where:
        manageUsersAndDelegate | manageUsers
        false                  | false
        false                  | true
        true                   | false
        true                   | true
    }

    void "test updateProjectRole on valid input"() {
        given:
        ProjectRole newRole = DomainFactory.createProjectRole(accessToOtp: true)
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: User.findByUsername(USER),
                projectRole: DomainFactory.createProjectRole(accessToOtp: true),
                manageUsers: false,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.updateProjectRole(userProjectRole, newRole)
        }

        then:
        userProjectRole.projectRole == newRole
    }
}
