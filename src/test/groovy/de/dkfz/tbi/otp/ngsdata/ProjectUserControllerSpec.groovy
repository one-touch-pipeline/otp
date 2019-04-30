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

package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.acl.AclSid
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.LdapUserDetails
import de.dkfz.tbi.otp.security.*

import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY
import static javax.servlet.http.HttpServletResponse.SC_OK

class ProjectUserControllerSpec extends Specification implements ControllerUnitTest<ProjectUserController>, DataTest, UserAndRoles {

    Class[] getDomainClassesToMock() {[
            AclSid,
            Project,
            ProjectRole,
            Realm,
            Role,
            User,
            UserProjectRole,
            UserRole,
    ]}

    void "test index, sorting of users in different lists"() {
        given:
        Project project = DomainFactory.createProject()

        User enabledUser = DomainFactory.createUser()
        User disabledUser = DomainFactory.createUser()
        User unconnectedUser = DomainFactory.createUser()
        String unknownUsername = "unknownUser"

        addUserWithReadAccessToProject(enabledUser, project, true)
        addUserWithReadAccessToProject(disabledUser, project, false)

        controller.userProjectRoleService = Mock(UserProjectRoleService) {
            getEmailsForNotification(_) >> "emails"
        }
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
        def model = controller.index()

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

        controller.addUserToProject()

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

        controller.addUserToProject()

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
