/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.cron

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService.OperatorAction
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails

@CompileDynamic
@Component
@Slf4j
class CheckFileAccessInconsistenciesForUsersOnlyInLdapOrWithoutProjectRoleJob extends AbstractScheduledJob {

    static final String SUBJECT = "User Management Inconsistencies status"

    @Autowired
    IdentityProvider identityProvider

    @Autowired
    UserProjectRoleService userProjectRoleService

    @Override
    void wrappedExecute() {
        String userInconsistencies = generateReportForUsersOnlyInLdapOrInOtpWithoutProjectRole()
        if (userInconsistencies) {
            mailHelperService.saveMail(SUBJECT, userInconsistencies)
        }
    }

    String generateReportForUsersOnlyInLdapOrInOtpWithoutProjectRole() {
        List<String> output = []
        Map<String, IdpUserDetails> cache = [:]

        Project.findAll().each { Project project ->
            List<User> projectUsers = UserProjectRole.findAllByProject(project)*.user
            List<String> nonDatabaseUsers = []
            List<String> ldapGroupMembers = identityProvider.getGroupMembersByGroupName(project.unixGroup) - processingOptionService.findOptionAsList(
                    ProcessingOption.OptionName.GUI_IGNORE_UNREGISTERED_OTP_USERS_FOUND
            )

            ldapGroupMembers.each { String username ->
                User user = User.findAllByUsername(username).find { it }
                if (user) {
                    projectUsers << user
                } else {
                    nonDatabaseUsers << username
                }
            }

            projectUsers = projectUsers.unique().sort { it.username }

            List<User> usersWithoutUserProjectRole = projectUsers.findAll { User user ->
                !UserProjectRole.findAllByUserAndProject(user, project).find { it }
            }

            if (usersWithoutUserProjectRole || nonDatabaseUsers) {
                output << ("Project: ${project.name} (${project.unixGroup})" as String)
                if (usersWithoutUserProjectRole) {
                    output << "Following users have not been added to the project:"
                    usersWithoutUserProjectRole.each { User user ->
                        String removeCommand = "Command to remove user from group: " +
                                userProjectRoleService.getCommand(project.unixGroup, user.username, OperatorAction.REMOVE)
                        output << sprintf("%-15s | %-20s | %-40s | %s", [user.username, user.realName, user.email, removeCommand])
                    }
                    output << "\n"
                }
                if (nonDatabaseUsers) {
                    output << "Following Users could not be resolved to a user in the OTP database:"
                    nonDatabaseUsers.each { String username ->
                        IdpUserDetails details = cache.computeIfAbsent(username) {
                            identityProvider.getIdpUserDetailsByUsername(username)
                        }
                        String removeCommand = "Command to remove user from group: " +
                                userProjectRoleService.getCommand(project.unixGroup, username, OperatorAction.REMOVE)
                        output << sprintf("%-15s | %-20s | %-40s | %s", [details.username, details.realName, details.mail, removeCommand])
                    }
                    output << "\n"
                }
            }
        }
        if (output) {
            output.add(
                    0,
                    "The following table lists users, which are in an OTP project group according LDAP, but in OTP are not connected to that project.\n"
            )
        }
        return output.size() ? output.join('\n') : ""
    }
}
