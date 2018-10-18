package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import groovy.text.*
import org.codehaus.groovy.grails.context.support.*
import org.springframework.context.*
import org.springframework.context.i18n.*
import org.springframework.security.access.prepost.*

class UserProjectRoleService {

    final static String USER_PROJECT_ROLE_REQUIRED = "the input userProjectRole must not be null"
    final static String USERNAME_REQUIRED = "the input user needs a username"

    AuditLogService auditLogService
    LdapService ldapService
    MailHelperService mailHelperService
    PluginAwareResourceBundleMessageSource messageSource
    ProcessingOptionService processingOptionService
    UserService userService

    private UserProjectRole createUserProjectRole(User user, Project project, ProjectRole projectRole, Map flags = [:]) {
        assert user : "the user must not be null"
        assert project : "the project must not be null"

        UserProjectRole userProjectRole = new UserProjectRole([
                user       : user,
                project    : project,
                projectRole: projectRole,
            ] + flags
        )
        userProjectRole.save(flush: true, failOnError: true)
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addUserToProjectAndNotifyGroupManagementAuthority(Project project, ProjectRole projectRole, String username, Map flags = [:]) throws AssertionError {
        assert project : "project must not be null"

        LdapUserDetails ldapUserDetails = ldapService.getLdapUserDetailsByUsername(username)
        assert ldapUserDetails : "'${username}' can not be resolved to a user via LDAP"
        assert ldapUserDetails.mail : "Could not get a mail for this user via LDAP"

        User user = User.findByUsernameOrEmail(ldapUserDetails.cn, ldapUserDetails.mail)
        if (!user) {
            user = userService.createUser(ldapUserDetails.cn, ldapUserDetails.mail, ldapUserDetails.realName)
        }
        UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
        assert !userProjectRole : "User '${user.username}' is already part of project '${project.name}'"
        userProjectRole = createUserProjectRole(user, project, projectRole, flags)

        if (flags.accessToFiles ?: false) {
            requestToAddUserToUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
        }
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Enabled ${userProjectRole.user.username} for ${userProjectRole.project.name}")
        notifyProjectAuthoritiesAndUser(project, user)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addExternalUserToProject(Project project, String realName, String email, ProjectRole projectRole) throws AssertionError {
        assert project : "project must not be null"

        User user = User.findByEmail(email)
        if (!user) {
            user = userService.createUser(null, email, realName)
        }
        UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
        assert !userProjectRole : "User '${user.realName}' is already part of project '${project.name}'"
        userProjectRole = createUserProjectRole(user, project, projectRole)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Enabled ${userProjectRole.user.realName} for ${userProjectRole.project.name}")
        notifyProjectAuthoritiesAndUser(project, user)
    }

    private void requestToAddUserToUnixGroupIfRequired(User user, Project project) {
        String[] groupNames = ldapService.getGroupsOfUserByUsername(user.username)
        if (!(project.unixGroup in groupNames)) {
            notifyAdministration(user, project, AdtoolAction.ADD)
        }
    }

    private void requestToRemoveUserFromUnixGroupIfRequired(User user, Project project) {
        String[] groupNames = ldapService.getGroupsOfUserByUsername(user.username)
        if (project.unixGroup in groupNames &&
            !UserProjectRole.findAllByUserAndProjectInListAndAccessToFilesAndEnabled(
                    user,
                    Project.findAllByUnixGroupAndIdNotEqual(project.unixGroup, project.id),
                    true,
                    true)
        ) {
            notifyAdministration(user, project, AdtoolAction.REMOVE)
        }
    }

    private void notifyAdministration(User user, Project project, AdtoolAction adtool) {
        String formattedAction = adtool.toString().toLowerCase()
        String subject = createMessage("projectUser.notification.addToUnixGroup.subject", [
                action     : formattedAction,
                conjunction: adtool == AdtoolAction.ADD ? 'to' : 'from',
                username   : user.username,
                projectName: project.name,
        ])
        String body = createMessage("projectUser.notification.addToUnixGroup.body", [
                action          : formattedAction,
                projectUnixGroup: project.unixGroup,
                username        : user.username,
        ])
        String email = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION)
        mailHelperService.sendEmail(subject, body, email)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_SENT_MAIL, "Sent mail to ${email} to ${formattedAction} ${user.username} ${adtool == AdtoolAction.ADD ? 'to' : 'from'} ${project.name}")
    }

    private enum AdtoolAction {
        ADD, REMOVE
    }

    private void notifyProjectAuthoritiesAndUser(Project project, User user) {
        List<User> projectAuthorities = UserProjectRole.findAllByProjectAndProjectRoleAndEnabled(
                project,
                ProjectRole.findByName("PI"),
                true
        )*.user
        List<String> allMails = (projectAuthorities*.email + user.email).unique()
        String subject = createMessage("projectUser.notification.newProjectMember.subject", [projectName: project.name])
        String body = createMessage("projectUser.notification.newProjectMember.body", [
                projectName          : project.name,
                userIdentifier       : user.realName ?: user.username,
        ])
        mailHelperService.sendEmail(subject, body, allMails)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_SENT_MAIL, "Notified project authorities (${projectAuthorities*.realName.join(", ")}) and user (${user.username})")
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
            requestToAddUserToUnixGroupIfRequired(upr.user, upr.project)
        } else {
            requestToRemoveUserFromUnixGroupIfRequired(upr.user, upr.project)
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

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#upr.project, 'MANAGE_USERS')")
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
        assert userProjectRole.save(flush: true, failOnError: true)
        if (enabled) {
            notifyProjectAuthoritiesAndUser(userProjectRole.project, userProjectRole.user)
        }
        if (userProjectRole.accessToFiles) {
            if (enabled) {
                requestToAddUserToUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
            } else {
                requestToRemoveUserFromUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
            }
        }
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "${enabled ? "En" : "Dis"}abled ${userProjectRole.user.username} for ${userProjectRole.project.name}")
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole updateProjectRole(UserProjectRole userProjectRole, ProjectRole newProjectRole) {
        assert userProjectRole: USER_PROJECT_ROLE_REQUIRED
        assert newProjectRole: "the input projectRole must not be null"
        userProjectRole.projectRole = newProjectRole
        assert userProjectRole.save(flush: true, failOnError: true)
        return userProjectRole
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
}
