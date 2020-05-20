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
import groovy.transform.TupleConstructor
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.impl.OtpDicomAuditFactory
import de.dkfz.odcf.audit.impl.OtpDicomAuditFactory.UniqueIdentifierType
import de.dkfz.odcf.audit.impl.enums.DicomCode.OtpPermissionCode
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*

import static de.dkfz.tbi.otp.security.DicomAuditUtils.getRealUserName

@Transactional
class UserProjectRoleService {

    SpringSecurityService springSecurityService
    AuditLogService auditLogService
    LdapService ldapService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    ProcessingOptionService processingOptionService
    UserService userService
    ConfigService configService

    private UserProjectRole createUserProjectRole(User user, Project project, ProjectRole projectRole, Map flags = [:]) {
        assert user: "the user must not be null"
        assert project: "the project must not be null"
        assert !UserProjectRole.findByUserAndProject(user, project): "User '${user.username ?: user.realName}' is already part of project '${project.name}'"

        String requester = springSecurityService?.principal?.hasProperty("username") ?
                springSecurityService.principal.username : springSecurityService?.principal
        UserProjectRole oldUPR = CollectionUtils.atMostOneElement(UserProjectRole.findAllByUserAndProject(user, project))
        List<OtpPermissionCode> grantedPermissions = getPermissionDiff(true, flags, oldUPR)
        List<OtpPermissionCode> revokedPermissions = getPermissionDiff(false, flags, oldUPR)

        UserProjectRole userProjectRole = new UserProjectRole([
                user                     : user,
                project                  : project,
                projectRole              : projectRole,
                fileAccessChangeRequested: flags.accessToFiles ?: false,
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

    void synchedBetweenRelatedProjects(String unixGroup, Closure action) {
        Project.withTransaction {
            Project.findAllByUnixGroup(unixGroup).each { Project project ->
                action(project)
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addUserToProjectAndNotifyGroupManagementAuthority(Project project, ProjectRole projectRole, String username, Map flags = [:]) throws AssertionError {
        assert project: "project must not be null"
        User user = createUserWithLdapData(username)

        synchedBetweenRelatedProjects(project.unixGroup) { Project p ->
            createUserProjectRole(user, p, projectRole, flags)
        }

        UserProjectRole userProjectRole = CollectionUtils.exactlyOneElement(UserProjectRole.findAllByUserAndProjectAndProjectRole(user, project, projectRole))
        if (userProjectRole.accessToFiles) {
            sendFileAccessNotifications(userProjectRole)
        }

        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Enabled ${userProjectRole.user.username} for ${userProjectRole.project.name}")
        notifyProjectAuthoritiesAndUser(userProjectRole)
    }

    User createUserWithLdapData(String username) {
        LdapUserDetails ldapUserDetails = ldapService.getLdapUserDetailsByUsername(username)
        if (!ldapUserDetails) {
            throw new LdapUserCreationException("'${username}' can not be resolved to a user via LDAP")
        }
        if (!ldapUserDetails.mail) {
            throw new LdapUserCreationException("Could not get a mail for '${username}' via LDAP")
        }

        User user = CollectionUtils.atMostOneElement(User.findAllByUsernameOrEmail(ldapUserDetails.username, ldapUserDetails.mail))

        if (user) {
            if (!user.username) {
                throw new LdapUserCreationException("There is already an external user with email '${user.email}'")
            }
            if (user.username != ldapUserDetails.username) {
                throw new LdapUserCreationException("The given email address '${user.email}' is already registered for LDAP user '${user.username}'")
            }
        } else {
            user = userService.createUser(ldapUserDetails.username, ldapUserDetails.mail, ldapUserDetails.realName)
        }
        return user
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addExternalUserToProject(Project project, String realName, String email, ProjectRole projectRole) throws AssertionError {
        assert project: "project must not be null"

        User user = CollectionUtils.atMostOneElement(User.findAllByEmail(email))
        if (user) {
            assert !user.username: "The given email address '${user.email}' is already registered for LDAP user '${user.username}'"
            assert user.realName == realName: "The given email address '${user.email}' is already registered for external user '${user.realName}'"
        } else {
            user = userService.createUser(null, email, realName)
        }

        synchedBetweenRelatedProjects(project.unixGroup) { Project p ->
            createUserProjectRole(user, p, projectRole)
        }

        UserProjectRole userProjectRole = CollectionUtils.exactlyOneElement(UserProjectRole.findAllByUserAndProjectAndProjectRole(user, project, projectRole))

        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, "Enabled ${userProjectRole.user.realName} for ${userProjectRole.project.name}")
        notifyProjectAuthoritiesAndUser(userProjectRole)
    }

    private void sendFileAccessNotifications(UserProjectRole userProjectRole) {
        notifyUsersAboutFileAccessChange(userProjectRole)
        notifyAdministration(userProjectRole, OperatorAction.ADD)
    }

    private void notifyUsersAboutFileAccessChange(UserProjectRole userProjectRole) {
        User requester = CollectionUtils.exactlyOneElement(User.findAllByUsername(springSecurityService.authentication.principal.username as String))
        Project project = userProjectRole.project
        User user = userProjectRole.user

        String subject = messageSourceService.createMessage("projectUser.notification.fileAccessChange.subject", [
                projectName: project.name,
        ])

        String clusterName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.CLUSTER_NAME)
        String clusterAdministrationEmail = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION)
        String supportTeamName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION)
        String body = messageSourceService.createMessage("projectUser.notification.fileAccessChange.body", [
                username                  : user.realName,
                requester                 : requester.realName,
                projectName               : project.name,
                dirAnalysis               : project.dirAnalysis,
                clusterName               : clusterName,
                clusterAdministrationEmail: clusterAdministrationEmail,
                supportTeamSalutation     : supportTeamName,
                linkProjectDirectory      : LsdfFilesService.getPath(configService.rootPath.path, project.dirName),
        ])

        List<String> ccs = getUniqueProjectAuthoritiesAndUserManagers(project)*.email.sort()

        mailHelperService.sendEmail(subject, body, user.email, ccs)
    }

    private void notifyAdministration(UserProjectRole userProjectRole, OperatorAction action) {
        User requester = CollectionUtils.exactlyOneElement(User.findAllByUsername(springSecurityService.authentication.principal.username as String))
        UserProjectRole requesterUserProjectRole = CollectionUtils.atMostOneElement(
                UserProjectRole.findAllByUserAndProject(requester, userProjectRole.project)
        )
        String switchedUserAnnotation = SpringSecurityUtils.switched ? " (switched from ${SpringSecurityUtils.switchedUserOriginalUsername})" : ""

        String formattedAction = action.toString().toLowerCase()
        String conjunction = action == OperatorAction.ADD ? 'to' : 'from'
        String scriptCommand = commandTemplate(userProjectRole, action)
        String subject = messageSourceService.createMessage("projectUser.notification.addToUnixGroup.subject", [
                requester  : requester.username,
                action     : formattedAction,
                conjunction: conjunction,
                username   : userProjectRole.user.username,
                unixGroup  : userProjectRole.project.unixGroup,
        ])

        String affectedUserUserDetail = messageSourceService.createMessage("projectUser.notification.addToUnixGroup.userDetail", [
                realName: userProjectRole.user.realName,
                username: userProjectRole.user.username,
                email   : userProjectRole.user.email,
                role    : userProjectRole.projectRole.name,
        ])
        String requesterUserDetail = messageSourceService.createMessage("projectUser.notification.addToUnixGroup.userDetail", [
                realName: requester.realName,
                username: requester.username + switchedUserAnnotation,
                email   : requester.email,
                role    : requesterUserProjectRole ? requesterUserProjectRole.projectRole.name : "Non-Project-User",
        ])

        String body = messageSourceService.createMessage("projectUser.notification.addToUnixGroup.body", [
                projectName           : userProjectRole.project,
                projectUnixGroup      : userProjectRole.project.unixGroup,
                projectList           : Project.findAllByUnixGroup(userProjectRole.project.unixGroup)*.name.sort().join(", "),
                requestedAction       : action,
                affectedUserUserDetail: affectedUserUserDetail,
                requesterUserDetail   : requesterUserDetail,
                scriptCommand         : scriptCommand,
        ])

        String email = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION)
        mailHelperService.sendEmail(subject, body, email)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_SENT_MAIL,
                "Sent mail to ${email} to ${formattedAction} ${userProjectRole.user.username} ${conjunction} ${userProjectRole.project.name} " +
                        "at the request of ${requester.username + switchedUserAnnotation}")
    }

    @TupleConstructor
    private enum OperatorAction {
        ADD(ProcessingOption.OptionName.AD_GROUP_ADD_USER_SNIPPET),
        REMOVE(ProcessingOption.OptionName.AD_GROUP_REMOVE_USER_SNIPPET),

        final ProcessingOption.OptionName commandTemplateOptionName
    }

    private void notifyProjectAuthoritiesAndUser(UserProjectRole userProjectRole) {
        User executingUser = CollectionUtils.exactlyOneElement(User.findAllByUsername(springSecurityService.authentication.principal.username as String))

        String projectRoleName = userProjectRole.projectRole.name
        String projectName = userProjectRole.project.name

        List<Role> administrativeRoles = Role.findAllByAuthorityInList(Role.ADMINISTRATIVE_ROLES)

        boolean userIsSubmitter = projectRoleName == ProjectRole.Basic.SUBMITTER.name()
        boolean executingUserIsAdministrativeUser = CollectionUtils.atMostOneElement(UserRole.findAllByUserAndRoleInList(executingUser, administrativeRoles))

        String subject = messageSourceService.createMessage("projectUser.notification.newProjectMember.subject", [projectName: projectName])

        String body
        if (userIsSubmitter && executingUserIsAdministrativeUser) {
            String supportTeamName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION)
            body = messageSourceService.createMessage("projectUser.notification.newProjectMember.body.administrativeUserAddedSubmitter", [
                    userIdentifier       : userProjectRole.user.realName ?: userProjectRole.user.username,
                    projectRole          : projectRoleName,
                    projectName          : projectName,
                    supportTeamName      : supportTeamName,
                    supportTeamSalutation: supportTeamName,
            ])
        } else {
            body = messageSourceService.createMessage("projectUser.notification.newProjectMember.body.userManagerAddedMember", [
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

    private static String getFlagChangeLogMessage(String flagName, boolean newStatus, String username, String projectName) {
        return "${flagName} ${newStatus ? "en" : "dis"}abled for ${username} in ${projectName}"
    }

    List<UserProjectRole> getRelatedUserProjectRoles(UserProjectRole userProjectRole) {
        List<Project> projectsWithSharedUnixGroup = Project.findAllByUnixGroup(userProjectRole.project.unixGroup)
        return UserProjectRole.findAllByUserAndProjectInList(userProjectRole.user, projectsWithSharedUnixGroup)
    }

    void synchedBetweenRelatedUserProjectRoles(UserProjectRole userProjectRole, Closure action) {
        UserProjectRole.withTransaction {
            getRelatedUserProjectRoles(userProjectRole).each { UserProjectRole relatedUpr ->
                action(relatedUpr)
            }
        }
    }

    /**
     * Helper function to check if there would be any changes to the flags
     */
    boolean nothingToChange(UserProjectRole userProjectRole, Closure consistencyCheck) {
        return getRelatedUserProjectRoles(userProjectRole).every(consistencyCheck)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole setAccessToOtp(UserProjectRole userProjectRole, boolean value) {
        if (nothingToChange(userProjectRole, { UserProjectRole upr -> upr.accessToOtp == value })) {
            return userProjectRole
        }
        synchedBetweenRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.accessToOtp = value
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Access to OTP", upr.accessToOtp, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ACCESS_TO_OTP, message)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole setAccessToFiles(UserProjectRole userProjectRole, boolean value) {
        if (nothingToChange(userProjectRole, { UserProjectRole upr -> upr.accessToFiles == value })) {
            return userProjectRole
        }
        synchedBetweenRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.accessToFiles = value
            upr.fileAccessChangeRequested = true
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Access to Files", upr.accessToFiles, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ACCESS_TO_FILES, message)
        }
        if (userProjectRole.accessToFiles) {
            sendFileAccessNotifications(userProjectRole)
        } else {
            notifyAdministration(userProjectRole, OperatorAction.REMOVE)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'DELEGATE_USER_MANAGEMENT')")
    UserProjectRole setManageUsers(UserProjectRole userProjectRole, boolean value) {
        if (nothingToChange(userProjectRole, { UserProjectRole upr -> upr.manageUsers == value })) {
            return userProjectRole
        }
        synchedBetweenRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.manageUsers = value
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Manage Users", upr.manageUsers, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_MANAGE_USER, message)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    UserProjectRole setManageUsersAndDelegate(UserProjectRole userProjectRole, boolean value) {
        if (nothingToChange(userProjectRole, { UserProjectRole upr -> upr.manageUsersAndDelegate == value })) {
            return userProjectRole
        }
        synchedBetweenRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.manageUsersAndDelegate = value
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Delegate Manage Users", upr.manageUsersAndDelegate, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_DELEGATE_MANAGE_USER, message)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS') or #userProjectRole.user.username == principal.username")
    UserProjectRole setReceivesNotifications(UserProjectRole userProjectRole, boolean value) {
        if (nothingToChange(userProjectRole, { UserProjectRole upr -> upr.receivesNotifications == value })) {
            return userProjectRole
        }
        synchedBetweenRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.receivesNotifications = value
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Receives Notification", upr.receivesNotifications, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_RECEIVES_NOTIFICATION, message)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole setEnabled(UserProjectRole userProjectRole, boolean value) {
        if (nothingToChange(userProjectRole, { UserProjectRole upr -> upr.enabled == value })) {
            return userProjectRole
        }
        synchedBetweenRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.enabled = value
            if (upr.accessToFiles) {
                upr.fileAccessChangeRequested = true
            }
            assert upr.save(flush: true)
        }
        if (value) {
            notifyProjectAuthoritiesAndUser(userProjectRole)
        }
        if (userProjectRole.accessToFiles) {
            if (value) {
                sendFileAccessNotifications(userProjectRole)
            } else {
                notifyAdministration(userProjectRole, OperatorAction.REMOVE)
            }
        }
        String message = "${value ? "En" : "Dis"}abled ${userProjectRole.user.username} for ${userProjectRole.project.name}"
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, message)
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole updateProjectRole(UserProjectRole userProjectRole, ProjectRole newProjectRole) {
        if (nothingToChange(userProjectRole, { UserProjectRole upr -> upr.projectRole == newProjectRole })) {
            return userProjectRole
        }
        synchedBetweenRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.projectRole = newProjectRole
            assert upr.save(flush: true)
        }
        return userProjectRole
    }

    void handleSharedUnixGroupOnProjectCreation(Project project, String unixGroup) {
        Project donorProject = (Project.findAllByUnixGroup(unixGroup) - [project]).find()
        if (!donorProject) {
            return
        }
        applyUserProjectRolesOntoProject(donorProject, project)
    }

    /**
     * Applies all given UserProjectRoles onto the targetProject.
     * In this context 'applies' means create them in the project if they do not exist, or
     * update an already existing UserProjectRole for the user with the properties of the
     * given one.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void applyUserProjectRolesOntoProject(List<UserProjectRole> sourceUserProjectRoles, Project targetProject) {
        List<UserProjectRole> sourceUserProjectRole = sourceUserProjectRoles
        List<UserProjectRole> targetUserProjectRoles = UserProjectRole.findAllByProject(targetProject)
        sourceUserProjectRole.each { UserProjectRole userProjectRole ->
            Map properties = [
                    project                  : targetProject,
                    user                     : userProjectRole.user,
                    projectRole              : userProjectRole.projectRole,
                    enabled                  : userProjectRole.enabled,
                    accessToOtp              : userProjectRole.accessToOtp,
                    accessToFiles            : userProjectRole.accessToFiles,
                    manageUsers              : userProjectRole.manageUsers,
                    manageUsersAndDelegate   : userProjectRole.manageUsersAndDelegate,
                    receivesNotifications    : userProjectRole.receivesNotifications,
                    fileAccessChangeRequested: userProjectRole.accessToFiles,
            ]
            UserProjectRole existingUserProjectRole = targetUserProjectRoles.find { it.user == userProjectRole.user }
            if (existingUserProjectRole) {
                properties.each { String property, Object value ->
                    existingUserProjectRole."$property" = value
                }
                existingUserProjectRole.save(flush: true)
            } else {
                new UserProjectRole(properties).save(flush: true)
            }
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void applyUserProjectRolesOntoProject(Project sourceProject, Project targetProject) {
        applyUserProjectRolesOntoProject(UserProjectRole.findAllByProject(sourceProject), targetProject)
    }

    String getEmailsForNotification(Project project) {
        assert project: 'No project given'
        return UserProjectRole.findAllByProjectAndReceivesNotificationsAndEnabled(project, true, true)*.user.findAll { it.enabled }*.email?.sort()?.join(',') ?: ''
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
                (added == flags.accessToOtp)            && (added == !oldUPR?.accessToOtp) ? [OtpPermissionCode.OTP_ACCESS] : [],
                (added == flags.accessToFiles)          && (added == !oldUPR?.accessToFiles) ? [OtpPermissionCode.FILE_ACCESS] : [],
                (added == flags.manageUsers)            && (added == !oldUPR?.manageUsers) ? [OtpPermissionCode.MANAGE_USERS] : [],
                (added == flags.manageUsersAndDelegate) && (added == !oldUPR?.manageUsersAndDelegate) ? [OtpPermissionCode.DELEGATE_MANAGE_USERS] : [],
        ].flatten()
    }

    private String commandTemplate(UserProjectRole userProjectRole, OperatorAction action) {
        return new SimpleTemplateEngine()
                .createTemplate(processingOptionService.findOptionAsString(action.commandTemplateOptionName))
                .make([unixGroup: userProjectRole.project.unixGroup, username: userProjectRole.user.username])
                .toString()
    }
}

class LdapUserCreationException extends RuntimeException {
    LdapUserCreationException(String message) {
        super(message)
    }
}
