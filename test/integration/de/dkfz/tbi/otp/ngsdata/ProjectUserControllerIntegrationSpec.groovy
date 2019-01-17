package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.security.*
import grails.plugin.springsecurity.*
import spock.lang.*

import static javax.servlet.http.HttpServletResponse.*

class ProjectUserControllerIntegrationSpec extends Specification implements UserAndRoles {

    ProjectUserController controller = new ProjectUserController()

    void "test index, sorting of users in different lists"() {
        given:
        Project project = DomainFactory.createProject()

        User enabledUser = DomainFactory.createUser()
        User disabledUser = DomainFactory.createUser()
        User unconnectedUser = DomainFactory.createUser()
        String unknownUsername = "unknownUser"

        addUserWithReadAccessToProject(enabledUser, project, true)
        addUserWithReadAccessToProject(disabledUser, project, false)

        controller.projectService = Mock(ProjectService) {
            getAllProjects() >> [project]
        }
        controller.projectSelectionService = Mock(ProjectSelectionService) {
            getSelectedProject() >> new ProjectSelection(projects: [project])
            getProjectFromProjectSelectionOrAllProjects(_) >> project
        }
        controller.ldapService = Mock(LdapService) {
            getDistinguishedNameOfGroupByGroupName(_) >> project.unixGroup
            getGroupMembersByDistinguishedName(_) >> [enabledUser.username, disabledUser.username, unconnectedUser.username, unknownUsername]
            getLdapUserDetailsByUsername(_) >> new LdapUserDetails()
        }

        when:
        controller.request.method = 'GET'
        def model = SpringSecurityUtils.doWithAuth(enabledUser.username) {
            controller.index()
        }

        then:
        controller.response.status == SC_OK
        model.enabledProjectUsers.size() == 1
        model.enabledProjectUsers.first().user == enabledUser
        model.disabledProjectUsers.size() == 1
        model.disabledProjectUsers.first().user == disabledUser
        model.usersWithoutUserProjectRole == [unconnectedUser.username]
        model.unknownUsersWithFileAccess == [unknownUsername]
    }

    void "test addUserToProject, switch between respective functions to add users"() {
        given:
        createUserAndRoles()

        controller.userProjectRoleService = Mock(UserProjectRoleService) {
            addUserToProjectAndNotifyGroupManagementAuthority(_, _, _, _) >> null
            addExternalUserToProject(_, _, _, _) >> null
        }

        when:
        controller.request.method = 'POST'
        controller.params.project = DomainFactory.createProject()
        controller.params.addViaLdap = addViaLdap
        controller.params.searchString = "searchString"
        controller.params.projectRoleName = "projectRole"
        controller.params.realName = "realName"
        controller.params.email = "email@dummy.de"
        controller.params.accessToFiles = true
        controller.params.manageUsers = true
        controller.params.manageUsersAndDelegate = true

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.addUserToProject()
        }

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/projectUser/index"
        controller.flash.message.message == "Data stored successfully"
        intInvocations * controller.userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(_, _, _, _)
        extInvocations * controller.userProjectRoleService.addExternalUserToProject(_, _, _, _)

        where:
        addViaLdap | extInvocations | intInvocations
        true       | 0              | 1
        false      | 1              | 0
    }

    @Unroll
    void "test addUserToProject, catch exceptions caused in either method (#errorMessage)"() {
        given:
        createUserAndRoles()

        controller.userProjectRoleService = Mock(UserProjectRoleService) {
            addUserToProjectAndNotifyGroupManagementAuthority(_, _, _, _) >> { throw new AssertionError("internal") }
            addExternalUserToProject(_, _, _, _) >> { throw new AssertionError("external") }
        }

        when:
        controller.request.method = 'POST'
        controller.params.project = DomainFactory.createProject()
        controller.params.addViaLdap = addViaLdap
        controller.params.searchString = "searchString"
        controller.params.projectRoleName = "projectRole"
        controller.params.realName = "realName"
        controller.params.email = "email@dummy.de"
        controller.params.accessToFiles = true
        controller.params.manageUsers = true
        controller.params.manageUsersAndDelegate = true
        controller.params.receivesNotifications = true

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            controller.addUserToProject()
        }

        then:
        controller.response.status == SC_MOVED_TEMPORARILY
        controller.response.redirectedUrl == "/projectUser/index"
        controller.flash.message.message == "An error occurred"
        controller.flash.message.errorList == [errorMessage]

        where:
        addViaLdap | errorMessage
        true       | "internal"
        false      | "external"
    }
}
