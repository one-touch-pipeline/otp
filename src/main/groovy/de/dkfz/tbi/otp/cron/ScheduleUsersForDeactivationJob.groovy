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

import grails.web.mapping.LinkGenerator
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.security.User

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
@SuppressWarnings("AnnotationsForJobs")
@Scope("singleton")
@Component
@Slf4j
class ScheduleUsersForDeactivationJob extends ScheduledJob {

    @Autowired
    LinkGenerator linkGenerator

    @Autowired
    ConfigService configService

    @Autowired
    ProjectService projectService

    @Autowired
    LdapService ldapService

    @Autowired
    UserService userService

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

    String getLinuxGroupAdministrationMail() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION)
    }

    String getOtpServiceSalutation() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION)
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
        log.info("Reset deactivation date of ${user}")
    }

    /**
     * This function defines what is considered a deactivated user in the context of this job.
     *
     * Non-existing users are to be handled as deactivated.
     */
    boolean userIsDeactivated(User user) {
        return !ldapService.existsInLdap(user) || ldapService.isUserDeactivated(user)
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
        |OTP has detected users with expired accounts in your projects, please deactivate them from all projects.
        |You can manage the users of a project here: ${linkGenerator.link(controller: 'projectUser', action: 'index', absolute: true)},
        |
        |If a user's account stays expired for the next ${deactivationGracePeriod} days, they will be removed automatically. You will not receive a reminder.
        |
        |The expired users are:
        |${getMailBodyWithInvalidUsers(invalidUsers)}
        |
        |If this is wrong please contact us at: ${linuxGroupAdministrationMail}
        |
        |Best regards,
        |OTP on behalf of ${otpServiceSalutation}
        |""".stripMargin()

        mailHelperService.sendEmail(subject, body, authority.email, [linuxGroupAdministrationMail])

        log.info("Sent deactivation mail to ${authority} concerning the following users: ${invalidUsers}")
    }

    void sendDeactivationMailForService(Set<UserProjectRole> invalidUsers) {
        String subject = "Expired users in OTP projects without authority"
        String body = """\
        |The following users have been scheduled for deactivation, but at least one of their projects does not have an authorative user:
        |${getMailBodyWithInvalidUsers(invalidUsers)}
        |""".stripMargin()

        mailHelperService.sendEmail(subject, body, linuxGroupAdministrationMail)

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
            projectRole {
                'in'("name", ProjectRole.AUTHORITY_PROJECT_ROLES)
            }
            eq("enabled", true)
            projections {
                distinct("user")
            }
        } as List<User>
    }
}
