/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.project.projectRequest

import grails.gorm.transactions.Transactional
import org.codehaus.groovy.runtime.InvokerHelper

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectRequestUser
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.utils.exceptions.UserDisabledException

@Transactional
class ProjectRequestUserService {

    UserProjectRoleService userProjectRoleService
    UserService userService

    ProjectRequestUser saveProjectRequestUserFromCommand(ProjectRequestUserCommand cmd) {
        if (cmd.username) {
            User user = userService.findOrCreateUserWithLdapData(cmd.username)
            Map<String, Object> projectRequestUserParameters = [
                    user                  : user,
                    projectRoles          : cmd.projectRoles,
                    accessToOtp           : cmd.accessToOtp,
                    accessToFiles         : cmd.accessToFiles,
                    manageUsers           : forceManageUsers(cmd.projectRoles) ? true : cmd.manageUsers,
                    manageUsersAndDelegate: ableToDelegateManagement(cmd.projectRoles),
            ]
            ProjectRequestUser projectRequestUser
            if (cmd.projectRequestUser) {
                InvokerHelper.setProperties(cmd.projectRequestUser, projectRequestUserParameters)
                projectRequestUser = cmd.projectRequestUser
            } else {
                projectRequestUser = new ProjectRequestUser(projectRequestUserParameters)
            }
            saveProjectRequestUser(projectRequestUser)
            return projectRequestUser
        }
        return null
    }

    Set<ProjectRequestUser> saveProjectRequestUsersFromCommands(List<ProjectRequestUserCommand> users) {
        ProjectRequestUserCommand disabledUser = users.find { it && !userService.findOrCreateUserWithLdapData(it.username).enabled }
        if (disabledUser) {
            throw new UserDisabledException("Project request contains at least one disabled user.")
        }

        // When the user adds/removes user form elements dynamically it creates holes in the indexes, which are converted to null objects upon
        // command object binding. So we remove them with findAll.
        return users.findAll().collect { ProjectRequestUserCommand user ->
            saveProjectRequestUserFromCommand(user)
        }.findAll() as Set<ProjectRequestUser>
    }

    void deleteProjectRequestUser(ProjectRequestUser projectRequestUser) {
        projectRequestUser.delete(flush: true)
    }

    void saveProjectRequestUser(ProjectRequestUser projectRequestUser) {
        projectRequestUser.save(flush: true)
    }

    boolean ableToDelegateManagement(Set<ProjectRole> projectRoles) {
        return ProjectRoleService.projectRolesContainAuthoritativeRole(projectRoles)
    }

    boolean forceManageUsers(Set<ProjectRole> projectRoles) {
        return ProjectRoleService.projectRolesContainAuthoritativeRole(projectRoles) || ProjectRoleService.projectRolesContainCoordinator(projectRoles)
    }
}
