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

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.text.SimpleTemplateEngine
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.impl.OtpDicomAuditFactory
import de.dkfz.odcf.audit.impl.OtpDicomAuditFactory.UniqueIdentifierType
import de.dkfz.odcf.audit.impl.enums.DicomCode.OtpPermissionCode
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.MailHelperService

import static de.dkfz.tbi.otp.security.DicomAuditUtils.getRealUserName

@Transactional
class UserProjectRoleService {

    final static String USER_PROJECT_ROLE_REQUIRED = "the input userProjectRole must not be null"
    final static String USERNAME_REQUIRED = "the input user needs a username"

    SpringSecurityService springSecurityService
    AuditLogService auditLogService
    LdapService ldapService
    MailHelperService mailHelperService
    PluginAwareResourceBundleMessageSource messageSource
    ProcessingOptionService processingOptionService
    UserService userService

    private UserProjectRole createUserProjectRole(User user, Project project, ProjectRole projectRole, Map flags = [:]) {
        assert user: "the user must not be null"
        assert project: "the project must not be null"
        assert !UserProjectRole.findByUserAndProject(user, project): "User '${user.username ?: user.realName}' is already part of project '${project.name}'"

        String requester = springSecurityService?.principal?.hasProperty("username") ?
                springSecurityService.principal.username : springSecurityService?.principal
        UserProjectRole oldUPR = UserProjectRole.findByUserAndProject(user, project)
        List<OtpPermissionCode> grantedPermissions = getPermissionDiff(true, flags, oldUPR)
        List<OtpPermissionCode> revokedPermissions = getPermissionDiff(false, flags, oldUPR)

        UserProjectRole userProjectRole = new UserProjectRole([
                user       : user,
                project    : project,
                projectRole: projectRole,
        ] + flags)
        userProjectRole.save(flush: true)

        String studyUID = OtpDicomAuditFactory.generateUID(UniqueIdentifierType.STUDY, String.valueOf(project.id))
        if (flags.enabled && !oldUPR?.enabled) {
            DicomAuditLogger.logUserActivated(EventOutcomeIndicator.SUCCESS, getRealUserName(requester), user.username, studyUID)
        } else if (!flags.enabled && oldUPR?.enabled) {
            DicomAuditLogger.logUserDeactivated(EventOutcomeIndicator.SUCCESS, getRealUserName(requester), user.username, studyUID)
        }

        if (grantedPermissions) {
            DicomAuditLogger.logPermissionGranted(EventOutcomeIndicator.SUCCESS, getRealUserName(requester), user.username, studyUID, *grantedPermissions)
        }
        if (revokedPermissions) {
            DicomAuditLogger.logPermissionRevoked(EventOutcomeIndicator.SUCCESS, getRealUserName(requester), user.username, studyUID, *revokedPermissions)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addUserToProjectAndNotifyGroupManagementAuthority(Project project, ProjectRole projectRole, String username, Map flags = [:]) throws AssertionError {
        assert project: "project must not be null"

        LdapUserDetails ldapUserDetails = ldapService.getLdapUserDetailsByUsername(username)
        assert ldapUserDetails: "'${username}' can not be resolved to a user via LDAP"
        assert ldapUserDetails.mail: "Could not get a mail for this user via LDAP"

        User user = User.findByUsernameOrEmail(ldapUserDetails.cn, ldapUserDetails.mail)

        if (user) {
            assert user.username: "There is already an external user with email '${user.email}'"
            assert user.username == ldapUserDetails.cn: "The given email address '${user.email}' is already registered for LDAP user '${user.username}'"
        } else {
            user = userService.createUser(ldapUserDetails.cn, ldapUserDetails.mail, ldapUserDetails.realName)
        }

        UserProjectRole userProjectRole = createUserProjectRole(user, project, projectRole, flags)

        if (userProjectRole.accessToFiles) {
            sendFileAccessNotifications(userProjectRole)
        }

        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Enabled ${userProjectRole.user.username} for ${userProjectRole.project.name}")
        notifyProjectAuthoritiesAndUser(userProjectRole)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addExternalUserToProject(Project project, String realName, String email, ProjectRole projectRole) throws AssertionError {
        assert project: "project must not be null"

        User user = User.findByEmail(email)
        if (user) {
            assert !user.username: "The given email address '${user.email}' is already registered for LDAP user '${user.username}'"
            assert user.realName == realName: "The given email address '${user.email}' is already registered for external user '${user.realName}'"
        } else {
            user = userService.createUser(null, email, realName)
        }

        UserProjectRole userProjectRole = createUserProjectRole(user, project, projectRole)

        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Enabled ${userProjectRole.user.realName} for ${userProjectRole.project.name}")
        notifyProjectAuthoritiesAndUser(userProjectRole)
    }

    private void sendFileAccessNotifications(UserProjectRole userProjectRole) {
        notifyUsersAboutFileAccessChange(userProjectRole)
        requestToAddUserToUnixGroupIfRequired(userProjectRole)
    }

    private void notifyUsersAboutFileAccessChange(UserProjectRole userProjectRole) {
        User requester = User.findByUsername(springSecurityService.authentication.principal.username as String)
        Project project = userProjectRole.project
        User user = userProjectRole.user

        String subject = createMessage("projectUser.notification.fileAccessChange.subject", [
            projectName: project.name,
        ])

        String clusterName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.CLUSTER_NAME)
        String clusterAdministrationEmail = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION)
        String supportTeamName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION)
        String body = createMessage("projectUser.notification.fileAccessChange.body", [
                username                  : user.realName,
                requester                 : requester.realName,
                projectName               : project.name,
                dirAnalysis               : project.dirAnalysis,
                clusterName               : clusterName,
                clusterAdministrationEmail: clusterAdministrationEmail,
                supportTeamSalutation     : supportTeamName,
        ])

        List<String> ccs = getUniqueProjectAuthoritiesAndUserManagers(project)*.email.sort()

        mailHelperService.sendEmail(subject, body, user.email, ccs)
    }

    private void requestToAddUserToUnixGroupIfRequired(UserProjectRole userProjectRole) {
        String[] groupNames = ldapService.getGroupsOfUserByUsername(userProjectRole.user.username)
        if (!(userProjectRole.project.unixGroup in groupNames)) {
            notifyAdministration(userProjectRole, OperatorAction.ADD)
        }
    }

    private void requestToRemoveUserFromUnixGroupIfRequired(UserProjectRole userProjectRole) {
        String[] groupNames = ldapService.getGroupsOfUserByUsername(userProjectRole.user.username)
        List <Project> projects = Project.findAllByUnixGroupAndIdNotEqual(userProjectRole.project.unixGroup, userProjectRole.project.id)
        if (userProjectRole.project.unixGroup in groupNames && (
                !projects || !UserProjectRole.findAllByUserAndProjectInListAndAccessToFilesAndEnabled(
                                userProjectRole.user,
                                projects,
                                true,
                                true
                        )
        )) {
            notifyAdministration(userProjectRole, OperatorAction.REMOVE)
        }
    }

    private void notifyAdministration(UserProjectRole userProjectRole, OperatorAction action) {
        User requester = User.findByUsername(springSecurityService.authentication.principal.username as String)
        UserProjectRole requesterUserProjectRole = UserProjectRole.findByUserAndProject(requester, userProjectRole.project)
        String switchedUserAnnotation = SpringSecurityUtils.isSwitched() ? " (switched from ${SpringSecurityUtils.getSwitchedUserOriginalUsername()})" : ""

        String formattedAction = action.toString().toLowerCase()
        String conjunction = action == OperatorAction.ADD ? 'to' : 'from'
        String subject = createMessage("projectUser.notification.addToUnixGroup.subject", [
                requester  : requester.username,
                action     : formattedAction,
                conjunction: conjunction,
                username   : userProjectRole.user.username,
                projectName: userProjectRole.project.name,
        ])

        String affectedUserUserDetail = createMessage("projectUser.notification.addToUnixGroup.userDetail", [
                realName: userProjectRole.user.realName,
                username: userProjectRole.user.username,
                email   : userProjectRole.user.email,
                role    : userProjectRole.projectRole.name,
        ])
        String requesterUserDetail = createMessage("projectUser.notification.addToUnixGroup.userDetail", [
                realName: requester.realName,
                username: requester.username + switchedUserAnnotation,
                email   : requester.email,
                role    : requesterUserProjectRole ? requesterUserProjectRole.projectRole.name : "Non-Project-User",
        ])

        List<Project> otherProjectsOfUnixGroup = Project.findAllByUnixGroup(userProjectRole.project.unixGroup) - userProjectRole.project
        String body = createMessage("projectUser.notification.addToUnixGroup.body", [
                projectName           : userProjectRole.project,
                projectUnixGroup      : userProjectRole.project.unixGroup,
                projectList           : (otherProjectsOfUnixGroup*.name.sort() ?: ["None"]).join(", "),
                requestedAction       : action,
                affectedUserUserDetail: affectedUserUserDetail,
                requesterUserDetail   : requesterUserDetail,
        ])

        String email = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION)
        mailHelperService.sendEmail(subject, body, email)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_SENT_MAIL,
                "Sent mail to ${email} to ${formattedAction} ${userProjectRole.user.username} ${conjunction} ${userProjectRole.project.name} " +
                        "at the request of ${requester.username + switchedUserAnnotation}")
    }

    private enum OperatorAction {
        ADD, REMOVE
    }

    private void notifyProjectAuthoritiesAndUser(UserProjectRole userProjectRole) {
        User executingUser = User.findByUsername(springSecurityService.authentication.principal.username as String)

        String projectRoleName = userProjectRole.projectRole.name
        String projectName = userProjectRole.project.name

        List<Role> administrativeRoles = Role.findAllByAuthorityInList(Role.ADMINISTRATIVE_ROLES)

        boolean userIsSubmitter = projectRoleName == ProjectRole.Basic.SUBMITTER.name()
        boolean executingUserIsAdministrativeUser = UserRole.findByUserAndRoleInList(executingUser, administrativeRoles)

        String subject = createMessage("projectUser.notification.newProjectMember.subject", [projectName: projectName])

        String body
        if (userIsSubmitter && executingUserIsAdministrativeUser) {
            String supportTeamName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION)
            body = createMessage("projectUser.notification.newProjectMember.body.administrativeUserAddedSubmitter" , [
                userIdentifier       : userProjectRole.user.realName ?: userProjectRole.user.username,
                projectRole          : projectRoleName,
                projectName          : projectName,
                supportTeamName      : supportTeamName,
                supportTeamSalutation: supportTeamName,
            ])
        } else {
            body = createMessage("projectUser.notification.newProjectMember.body.userManagerAddedMember" , [
                userIdentifier: userProjectRole.user.realName ?: userProjectRole.user.username,
                projectRole   : projectRoleName,
                projectName   : projectName,
                executingUser : executingUser.realName ?: executingUser.username,
            ])
        }

        List<User> projectAuthoritiesAndUserManagers = getUniqueProjectAuthoritiesAndUserManagers(userProjectRole.project)
        List<String> recipients = projectAuthoritiesAndUserManagers*.email.unique().sort()
        if (recipients) {
            mailHelperService.sendEmail(subject, body, recipients, [userProjectRole.user.email])
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_SENT_MAIL,
                    "Notified project authorities (${projectAuthoritiesAndUserManagers*.realName.join(", ")}) and user (${userProjectRole.user.username})")
        }
    }

    private String getFlagChangeLogMessage(String flagName, boolean newStatus, String username, String projectName) {
        return "${flagName} ${newStatus ? "en" : "dis"}abled for ${username} in ${projectName}"
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#upr.project, 'MANAGE_USERS')")
    UserProjectRole toggleAccessToOtp(UserProjectRole upr) {
        assert upr: USER_PROJECT_ROLE_REQUIRED
        assert upr.user.username: USERNAME_REQUIRED
        upr.accessToOtp = !upr.accessToOtp
        assert upr.save(flush: true)
        String message = getFlagChangeLogMessage("Access to OTP", upr.accessToOtp, upr.user.username, upr.project.name)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ACCESS_TO_OTP, message)
        return upr
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#upr.project, 'MANAGE_USERS')")
    UserProjectRole toggleAccessToFiles(UserProjectRole upr) {
        assert upr: USER_PROJECT_ROLE_REQUIRED
        assert upr.user.username: USERNAME_REQUIRED
        upr.accessToFiles = !upr.accessToFiles
        assert upr.save(flush: true)
        if (upr.accessToFiles) {
            sendFileAccessNotifications(upr)
        } else {
            requestToRemoveUserFromUnixGroupIfRequired(upr)
        }
        String message = getFlagChangeLogMessage("Access to Files", upr.accessToFiles, upr.user.username, upr.project.name)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ACCESS_TO_FILES, message)
        return upr
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#upr.project, 'DELEGATE_USER_MANAGEMENT')")
    UserProjectRole toggleManageUsers(UserProjectRole upr) {
        assert upr: USER_PROJECT_ROLE_REQUIRED
        assert upr.user.username: USERNAME_REQUIRED
        upr.manageUsers = !upr.manageUsers
        assert upr.save(flush: true)
        String message = getFlagChangeLogMessage("Manage Users", upr.manageUsers, upr.user.username, upr.project.name)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_MANAGE_USER, message)
        return upr
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    UserProjectRole toggleManageUsersAndDelegate(UserProjectRole upr) {
        assert upr: USER_PROJECT_ROLE_REQUIRED
        assert upr.user.username: USERNAME_REQUIRED
        upr.manageUsersAndDelegate = !upr.manageUsersAndDelegate
        assert upr.save(flush: true)
        String message = getFlagChangeLogMessage("Delegate Manage Users", upr.manageUsersAndDelegate, upr.user.username, upr.project.name)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_DELEGATE_MANAGE_USER, message)
        return upr
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#upr.project, 'MANAGE_USERS') or #upr.user.username == principal.username")
    UserProjectRole toggleReceivesNotifications(UserProjectRole upr) {
        assert upr: USER_PROJECT_ROLE_REQUIRED
        upr.receivesNotifications = !upr.receivesNotifications
        assert upr.save(flush: true)
        String message = getFlagChangeLogMessage("Receives Notification", upr.receivesNotifications, upr.user.username, upr.project.name)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_RECEIVES_NOTIFICATION, message)
        return upr
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole updateEnabledStatus(UserProjectRole userProjectRole, boolean enabled) {
        assert userProjectRole: USER_PROJECT_ROLE_REQUIRED
        userProjectRole.enabled = enabled
        assert userProjectRole.save(flush: true)
        if (enabled) {
            notifyProjectAuthoritiesAndUser(userProjectRole)
        }
        if (userProjectRole.accessToFiles) {
            if (enabled) {
                sendFileAccessNotifications(userProjectRole)
            } else {
                requestToRemoveUserFromUnixGroupIfRequired(userProjectRole)
            }
        }
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED,
                "${enabled ? "En" : "Dis"}abled ${userProjectRole.user.username} for ${userProjectRole.project.name}")
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole updateProjectRole(UserProjectRole userProjectRole, ProjectRole newProjectRole) {
        assert userProjectRole: USER_PROJECT_ROLE_REQUIRED
        assert newProjectRole: "the input projectRole must not be null"
        userProjectRole.projectRole = newProjectRole
        assert userProjectRole.save(flush: true)
        return userProjectRole
    }

    String getEmailsForNotification(Project project) {
        assert project: 'No project given'

        Set<String> emails = UserProjectRole.findAllByProjectAndReceivesNotificationsAndEnabled(
                project,
                true,
                true
        )*.user.findAll { it.enabled }*.email

        if (emails) {
            return emails.unique().sort().join(',')
        }
        return ''
    }

    /**
     * returns the number of all users of specified projects.
     * If start and end date are set then only users which are created at given time period are returned for specified projects.
     * @return number of users
     */
    int getNumberOfValidUsersForProjects(List<Project> projects, Date startDate = null, Date endDate = null) {
        return UserProjectRole.createCriteria().get {
            'in'("project", projects)
            eq("enabled", true)
            projections {
                countDistinct("user")
            }
            user {
                if (startDate && endDate) {
                    between('dateCreated', startDate, endDate)
                }
            }
        } as int
    }

    private String createMessage(String templateName, Map properties = [:]) {
        assert templateName
        String template
        try {
            template = messageSource.getMessage(templateName, [].toArray(), LocaleContextHolder.getLocale())
        } catch (NoSuchMessageException e) {
            return ''
        }
        return new SimpleTemplateEngine().createTemplate(template).make(properties).toString()
    }

    List<String> getEmailsOfToBeNotifiedProjectUsers(Project project) {
        return getEmailsOfToBeNotifiedProjectUsers([project])
    }

    List<String> getEmailsOfToBeNotifiedProjectUsers(List<Project> projects) {
        return UserProjectRole.createCriteria().list {
            and {
                'in'('project', projects)
                eq('receivesNotifications', true)
                eq('enabled', true)
                user {
                    eq('enabled', true)
                }
            }
            projections {
                user {
                    distinct("email")
                }
            }
        } ?: []
    }

    private static List<User> getUniqueProjectAuthoritiesAndUserManagers(Project project) {
        return (getUserManagers(project) + getProjectAuthorities(project)).unique()
    }

    private static List<User> getUserManagers(Project project) {
        return UserProjectRole.findAllByProjectAndManageUsersAndEnabled(project, true, true)*.user
    }

    private static List<User> getProjectAuthorities(Project project) {
        return UserProjectRole.findAllByProjectAndProjectRoleInListAndEnabled(
                project,
                ProjectRole.findAllByNameInList(ProjectRole.AUTHORITY_PROJECT_ROLES),
                true
        )*.user
    }

    List<OtpPermissionCode> getPermissionDiff(boolean added, Map flags, UserProjectRole oldUPR) {
        return [
            (added == flags.accessToOtp)             && (added == !oldUPR?.accessToOtp)            ? [OtpPermissionCode.OTP_ACCESS] : [],
            (added == flags.accessToFiles)           && (added == !oldUPR?.accessToFiles)          ? [OtpPermissionCode.FILE_ACCESS] : [],
            (added == flags.manageUsers)             && (added == !oldUPR?.manageUsers)            ? [OtpPermissionCode.MANAGE_USERS] : [],
            (added == flags.manageUsersAndDelegate)  && (added == !oldUPR?.manageUsersAndDelegate) ? [OtpPermissionCode.DELEGATE_MANAGE_USERS] : [],
        ].flatten()
    }
}
