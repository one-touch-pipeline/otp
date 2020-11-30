/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.project

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*

import de.dkfz.tbi.otp.project.ProjectRequestUser.ApprovalState

@Transactional
class ProjectRequestUserService {

    SecurityService securityService
    UserProjectRoleService userProjectRoleService

    ProjectRequestUser createProjectRequestUser(User user, Set<ProjectRole> projectRoles, ApprovalState state, Map flags = [:]) {
        return new ProjectRequestUser([
                user         : user,
                projectRoles : projectRoles,
                approvalState: state,
        ] + flags)
    }

    ProjectRequestUser createProjectRequestUserFromCommand(ProjectRequestUserCommand cmd) {
        userProjectRoleService.createUserWithLdapData(cmd.username)
        User user = CollectionUtils.exactlyOneElement(User.findAllByUsername(cmd.username))
        return createProjectRequestUser(user, cmd.projectRoles, startingStateFromRoles(cmd.projectRoles), [
            accessToFiles         : cmd.accessToFiles,
            manageUsers           : ProjectRoleService.projectRolesContainAuthoritativeRole(cmd.projectRoles) ? true : cmd.manageUsers,
            manageUsersAndDelegate: ableToDelegateManagement(cmd.projectRoles),
        ])
    }

    Set<ProjectRequestUser> createProjectRequestUsersFromCommands(List<ProjectRequestUserCommand> users) {
        // When the user adds/removes user form elements dynamically it creates holes in the indexes, which are converted to null objects upon
        // command object binding. So we remove them with findAll.
        return users.findAll().collect { ProjectRequestUserCommand user ->
            createProjectRequestUserFromCommand(user)
        } as Set<ProjectRequestUser>
    }

    boolean ableToDelegateManagement(Set<ProjectRole> projectRoles) {
        return ProjectRoleService.projectRolesContainAuthoritativeRole(projectRoles)
    }

    ApprovalState startingStateFromRoles(Set<ProjectRole> projectRoles) {
        return ProjectRoleService.projectRolesContainAuthoritativeRole(projectRoles) ? ApprovalState.PENDING : ApprovalState.NOT_APPLICABLE
    }

    ProjectRequestUser getProjectRequestUserOfUser(ProjectRequest projectRequest, User user) {
        return CollectionUtils.exactlyOneElement(projectRequest.users.findAll { it.user == user })
    }

    ProjectRequestUser getProjectRequestUserOfCurrentUser(ProjectRequest projectRequest) {
        return getProjectRequestUserOfUser(projectRequest, securityService.currentUserAsUser)
    }

    ProjectRequestUser setApprovalStateAsCurrentUser(ProjectRequest request, ApprovalState state) {
        return getProjectRequestUserOfCurrentUser(request).with {
            approvalState = state
            save(flush: true)
        }
    }

    UserProjectRole toUserProjectRole(Project project, ProjectRequestUser projectRequestUser) {
        UserProjectRole upr = userProjectRoleService.createUserProjectRole(
                projectRequestUser.user,
                project,
                projectRequestUser.projectRoles,
        )
        userProjectRoleService.with {
            setAccessToOtp(upr)
            setAccessToFiles(upr, projectRequestUser.accessToFiles)
            setManageUsers(upr, projectRequestUser.manageUsers)
            setManageUsersAndDelegate(upr, projectRequestUser.manageUsersAndDelegate)
            setReceivesNotifications(upr, true)
        }
        return upr
    }
}
