/*
 * Copyright 2011-2024 The OTP authors
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

import grails.web.mapping.LinkGenerator
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.time.LocalDateTime

/**
 * Scheduled job to schedule users, which are disabled in the LDAP, to be disabled in OTP.
 *
 * It looks up all enabled internal users which are part of a project and sets their
 * plannedDeactivationDate if their password is invalid in LDAP.
 *
 * The plannedDeactivationDate is determined by today's date plus the period defined in
 * the ProcessingOption LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD.
 *
 * This job works in conjunction with {@link DeactivateUsersJob}, which is responsible
 * for actually disabling the users.
 */
@CompileDynamic
@Component
@Slf4j
class ScheduleUsersForDeactivationJob extends AbstractScheduledJob {

    @Autowired
    LinkGenerator linkGenerator

    @Autowired
    ConfigService configService

    @Autowired
    ProjectService projectService

    @Autowired
    IdentityProvider identityProvider

    @Autowired
    UserService userService

    @Autowired
    MessageSourceService messageSourceService

    @Autowired
    UserProjectRoleService userProjectRoleService

    List<User> getUsersToCheckForDeactivation() {
        List<User> enabledProjectUsers = UserProjectRole.withCriteria {
            eq("enabled", true)
            user {
                isNotNull("username")
            }
            projections {
                distinct("user")
            }
        } as List<User>

        List<User> otherScheduledUsers = User.withCriteria {
            isNotNull("plannedDeactivationDate")
        } as List<User>

        return (enabledProjectUsers + otherScheduledUsers).unique()
    }

    long getDeactivationGracePeriod() {
        return processingOptionService.findOptionAsLong(ProcessingOption.OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD)
    }

    String getReasonForDeactivatedUsers() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_TEXT_REASON_DEACTIVATED_USERS)
    }

    String getOtpServiceSalutation() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME)
    }

    Date getPlannedDeactivationDate() {
        LocalDateTime plannedDeactivationDate = LocalDateTime.now(configService.clock).plusDays(deactivationGracePeriod)
        return Date.from(plannedDeactivationDate.atZone(configService.timeZoneId).toInstant())
    }

    private void setPlannedDeactivationDateOfUser(User user) {
        Date date = plannedDeactivationDate
        userService.setPlannedDeactivationDateOfUser(user, date)
        log.info("Scheduled ${user} for deactivation on ${date}")
    }

    private void resetPlannedDeactivationDateOfUser(User user) {
        userService.setPlannedDeactivationDateOfUser(user, null)
        sendReactivationMails(user)
        log.info("Reset deactivation date of ${user}")
    }

    /**
     * This function defines what is considered a deactivated user in the context of this job.
     *
     * Non-existing users are to be handled as deactivated.
     */
    boolean userIsDeactivated(User user) {
        return !identityProvider.exists(user) || identityProvider.isUserDeactivated(user)
    }

    String getMailBodyWithInvalidUsers(Set<UserProjectRole> invalidUsers) {
        return invalidUsers.groupBy { UserProjectRole it -> it.user }.collect { User user, List<UserProjectRole> userProjectRoles ->
            "  - ${user} in project(s): ${userProjectRoles*.project*.name.sort().join(", ")}"
        }.join("\n")
    }

    void sendDeactivationMailForAuthorityUser(User authority, Set<UserProjectRole> invalidUsers) {
        String subject = "Expired users in your OTP project"
        String body = """\
        |Dear ${authority?.realName ?: "user"},
        |
        |There are users with an expired AD account in your OTP project(s):
        |${getMailBodyWithInvalidUsers(invalidUsers)}
        |
        |${reasonForDeactivatedUsers}
        |
        |If a user's account stays expired for the next ${deactivationGracePeriod} days, it will automatically be removed. You will not receive a reminder.
        |If the user's account is extended or reactivated within these ${deactivationGracePeriod} days, the user will keep access to all projects.
        |
        |Please be aware that the user will not get a copy of this mail and can therefore not take action.
        |
        |Best regards,
        |OTP on behalf of the ${otpServiceSalutation}
        |""".stripMargin()

        mailHelperService.sendEmail(subject, body, authority.email)

        log.info("Sent deactivation mail to ${authority} concerning the following users: ${invalidUsers}")
    }

    void sendDeactivationMailForService(Set<UserProjectRole> invalidUsers) {
        String subject = "Expired users in OTP projects without authority"
        String body = """\
        |The following users have been scheduled for deactivation, but at least one of their projects does not have an authorative user:
        |${getMailBodyWithInvalidUsers(invalidUsers)}
        |""".stripMargin()

        mailHelperService.sendEmailToTicketSystem(subject, body)

        log.info("Sent deactivation mail to service concerning the following users: ${invalidUsers}")
    }

    void sendDeactivationMails(Map<User, Set<UserProjectRole>> map) {
        map.each { User authority, Set<UserProjectRole> invalidUsers ->
            if (authority) {
                sendDeactivationMailForAuthorityUser(authority, invalidUsers)
            } else {
                sendDeactivationMailForService(invalidUsers)
            }
        }
    }

    void sendReactivationMails(User reactivatedUser) {
        userProjectRoleService.projectsAssociatedToProjectAuthority(reactivatedUser).each { User projectAuthority, List<Project> projects ->
            sendReactivationMailForAuthority(reactivatedUser, projectAuthority, projects)
        }
    }

    private void sendReactivationMailForAuthority(User reactivatedUser, User projectAuthority, List<Project> projects) {
        String recipient = projectAuthority.email
        List<String> ccs = [reactivatedUser.email]
        String subject = messageSourceService.createMessage("scheduledUsersForDeactivationJob.notification.userReactivated.subject", [
                reactivatedUser: reactivatedUser.username,
        ])
        String body = messageSourceService.createMessage("scheduledUsersForDeactivationJob.notification.userReactivated.body", [
                addressedUser        : "${projectAuthority.realName}",
                reactivatedUser      : "${reactivatedUser.realName} (${reactivatedUser.username})",
                userAuthority        : processingOptionService.findOptionAsString(ProcessingOption.OptionName.USER_AUTHORITY_TEAM_NAME),
                supportTeamSalutation: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
                projects             : projects*.name.sort().join('\n\t- '),
                projectUserLinks     : projects.collect {
                    linkGenerator.link(
                            controller: 'projectUser',
                            action: 'index',
                            absolute: true,
                            params: [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): it],
                    )
                }.sort().join('\n\t- '),
        ])
        mailHelperService.sendEmail(subject, body, recipient, ccs)
    }

    ActionPlan buildActionPlan() {
        ActionPlan plan = new ActionPlan()
        usersToCheckForDeactivation.sort { it.username }.each { User user ->
            if (userIsDeactivated(user)) {
                if (!user.plannedDeactivationDate) {
                    plan.usersToSetDate.add(user)
                    plan.addUserToNotificationMap(user)
                }
            } else {
                if (user.plannedDeactivationDate) {
                    plan.usersToResetDate.add(user)
                }
            }
        }
        return plan
    }

    @SuppressWarnings("UnnecessarySetter")
    void executeActionPlan(ActionPlan plan) {
        plan.usersToSetDate.each { User user ->
            setPlannedDeactivationDateOfUser(user)
        }
        plan.usersToResetDate.each { User user ->
            resetPlannedDeactivationDateOfUser(user)
        }
        sendDeactivationMails(plan.notificationMap)
    }

    @Override
    void wrappedExecute() {
        executeActionPlan(buildActionPlan())
    }
}

@CompileDynamic
@SuppressWarnings("AnnotationsForJobs")
class ActionPlan {

    List<User> usersToSetDate = []
    List<User> usersToResetDate = []
    Map<User, Set<UserProjectRole>> notificationMap = [:].withDefault { [] as Set<UserProjectRole> }

    void addUserToNotificationMap(User user) {
        UserProjectRole.findAllByUserAndEnabled(user, true).each { UserProjectRole upr ->
            List<User> authorityUsers = getValidAuthorityUsers(upr)
            if (authorityUsers) {
                authorityUsers.each { User authority ->
                    notificationMap[authority].add(upr)
                }
            } else {
                notificationMap[null].add(upr)
            }
        }
    }

    /**
     * Valid authority users need to have a role defined in ProjectRole.AUTHORITY_PROJECT_ROLES
     * and be an internal user, so they can log in to manage the users.
     */
    private List<User> getValidAuthorityUsers(UserProjectRole userProjectRole) {
        return UserProjectRole.createCriteria().list {
            ne("id", userProjectRole.id)
            eq("project", userProjectRole.project)
            user {
                isNotNull("username")
            }
            projectRoles {
                'in'("name", ProjectRole.AUTHORITY_PROJECT_ROLES)
            }
            eq("enabled", true)
            projections {
                distinct("user")
            }
        } as List<User>
    }
}
