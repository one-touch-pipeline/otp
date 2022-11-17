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
package de.dkfz.tbi.otp.cron

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.utils.MessageSourceService

/**
 * Scheduled job that checks for all users which are scheduled to be deactivated if their time has come.
 * If so, it subsequently deactivates them in all projects and notifies the group administration to
 * remove them from all groups.
 */
@Component
@Slf4j
class DeactivateUsersJob extends AbstractScheduledJob {

    @Autowired
    ProjectService projectService

    @Autowired
    UserProjectRoleService userProjectRoleService

    @Autowired
    IdentityProvider identityProvider

    @Autowired
    UserService userService

    @Autowired
    MessageSourceService messageSourceService

    static List<User> getUsersToCheck() {
        return User.createCriteria().list {
            isNotNull("username")
            isNotNull("plannedDeactivationDate")
            lt("plannedDeactivationDate", new Date())
        } as List<User>
    }

    String getUserRemovalCommandHelper(String unixGroup, User user) {
        return userProjectRoleService.commandTemplate(unixGroup, user.username, UserProjectRoleService.OperatorAction.REMOVE)
    }

    void notifyAdministration(User user, Set<String> allGroups) {
        String prefix = allGroups ? "TODO" : "DONE"
        String subject = "[${prefix}] Clean up LDAP groups of expired user ${user}"
        String body = "OTP has disabled ${user} because the LDAP account is expired.\n"

        if (allGroups) {
            body += """\
                |The user is still a member of some unix groups. Please remove this users access from the following groups:
                |
                |${allGroups.join(", ")}
                |
                |Removal helper command:
                |${allGroups.collect { String unixGroup -> getUserRemovalCommandHelper(unixGroup, user) }.join("\n")}
                |""".stripMargin()
        } else {
            body += "The user has no groups left to remove."
        }

        mailHelperService.sendEmailToTicketSystem(subject, body)
    }

    void notifyProjectAuthoritiesOfUsersProjects(User deactivatedUser) {
        userProjectRoleService.projectsAssociatedToProjectAuthority(deactivatedUser).each { User projectAuthority, List<Project> projects ->
            notifyProjectAuthority(deactivatedUser, projectAuthority, projects)
        }
    }

    private void notifyProjectAuthority(User deactivatedUser, User projectAuthority, List<Project> projects) {
        String recipient = projectAuthority.email
        List<String> ccs = [deactivatedUser.email]
        String subject = messageSourceService.createMessage("deactivateUsersJob.notification.userDeactivated.subject", [
                deactivatedUser: deactivatedUser.username,
        ])
        String body = messageSourceService.createMessage("deactivateUsersJob.notification.userDeactivated.body", [
                addressedUser          : "${projectAuthority.realName}",
                deactivatedUser        : "${deactivatedUser.realName} (${deactivatedUser.username})",
                deactivationGracePeriod: processingOptionService.findOptionAsString(ProcessingOption.OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD),
                supportTeamSalutation  : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
                projects               : projects*.name.sort().join('\n\t- '),
        ])
        mailHelperService.sendEmail(subject, body, recipient, ccs)
        log.info("Sent mail to ${recipient} concerning the deactivation of ${deactivatedUser.username}")
    }

    boolean isInGroup(User user, String unixGroup) {
        return unixGroup in identityProvider.getGroupsOfUser(user)
    }

    void disableUserAndNotify(User user) {
        log.info("Disable user ${user} with deactivation date ${user.plannedDeactivationDate}")
        Set<String> affectedUnixGroups = [] as Set<String>
        UserProjectRole.findAllByUserAndEnabled(user, true).each { UserProjectRole userProjectRole ->
            userProjectRoleService.doSetEnabled(userProjectRole, false)
            if (isInGroup(user, userProjectRole.project.unixGroup)) {
                affectedUnixGroups.add(userProjectRole.project.unixGroup)
            }
        }
        userService.setPlannedDeactivationDateOfUser(user, null)
        notifyProjectAuthoritiesOfUsersProjects(user)
        notifyAdministration(user, affectedUnixGroups)
    }

    @Override
    void wrappedExecute() {
        usersToCheck.each { User user ->
            disableUserAndNotify(user)
        }
    }
}
