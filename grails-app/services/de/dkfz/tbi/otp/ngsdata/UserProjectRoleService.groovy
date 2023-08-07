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
import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException

@CompileDynamic
@Transactional
class UserProjectRoleService {

    AuditLogService auditLogService
    IdentityProvider identityProvider
    ConfigService configService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    ProcessingOptionService processingOptionService
    RemoteShellHelper remoteShellHelper
    SecurityService securityService
    UserProjectRoleService userProjectRoleService
    UserService userService

    UserProjectRole createUserProjectRole(User user, Project project, Set<ProjectRole> projectRoles, Map<String, Boolean> flags = [:]) {
        assert user: "the user must not be null"
        assert project: "the project must not be null"
        assert !UserProjectRole.findAllByUserAndProject(user, project): "User '${user.username ?: user.realName}' is already part of project '${project.name}'"

        UserProjectRole userProjectRole = new UserProjectRole([
                user   : user,
                project: project,
        ])

        projectRoles.each {
            userProjectRole.addToProjectRoles(it)
        }

        userProjectRole.save(flush: true)

        if (ProjectRoleService.projectRolesContainAuthoritativeRole(projectRoles)) {
            flags.manageUsersAndDelegate = true
            flags.manageUsers = true
        }
        if (ProjectRoleService.projectRolesContainCoordinator(projectRoles)) {
            flags.manageUsers = true
        }

        flags.accessToOtp ? userProjectRoleService.setAccessToOtp(userProjectRole, flags.accessToOtp) : null
        flags.accessToFiles ? userProjectRoleService.setAccessToFiles(userProjectRole, flags.accessToFiles) : null
        flags.manageUsers ? userProjectRoleService.setManageUsers(userProjectRole, flags.manageUsers) : null
        flags.manageUsersAndDelegate ? userProjectRoleService.setManageUsersAndDelegate(userProjectRole, flags.manageUsersAndDelegate) : null
        (flags.receivesNotifications || flags.receivesNotifications == null) ? null :
                userProjectRoleService.setReceivesNotifications(userProjectRole, flags.receivesNotifications)

        auditLogService.logAction(
                AuditLog.Action.PROJECT_USER_CREATED_PROJECT_USER,
                "Created Project User: ${userProjectRole.toStringWithAllProperties()}"
        )

        return userProjectRole
    }

    /**
     * Applies a function for each Project of related Projects.
     *
     * @param unixGroup a String, search target for the related projects
     * @param action closure to be called on each Project
     */
    void applyToRelatedProjects(String unixGroup, Closure action) {
        Project.withTransaction {
            Project.findAllByUnixGroup(unixGroup).each { Project project ->
                action(project)
            }
        }
    }

    Map<User, List<Project>> projectsAssociatedToProjectAuthority(User user) {
        List<Project> allProjects = UserProjectRole.findAllByUser(user)*.project.unique()
        Map<User, List<Project>> projectAuthoritiesWithProjects = [:]
        allProjects.each { project ->
            getProjectAuthorities(project).each { projectAuthority ->
                if (projectAuthoritiesWithProjects.containsKey(projectAuthority)) {
                    projectAuthoritiesWithProjects[projectAuthority] << project
                } else {
                    projectAuthoritiesWithProjects.put(projectAuthority, [project])
                }
            }
        }
        return projectAuthoritiesWithProjects
    }

    List<UserProjectRole> findAllByUser(User user) {
        return UserProjectRole.findAllByUser(user)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addUserToProjectAndNotifyGroupManagementAuthority(Project project, Set<ProjectRole> projectRolesSet, String username, Map<String, Boolean> flags = [:])
            throws AssertionError {
        assert project: "project must not be null"
        User user = userService.findOrCreateUserWithLdapData(username)

        if (!user.enabled) {
            throw new OtpRuntimeException("User is disabled.")
        }

        applyToRelatedProjects(project.unixGroup) { Project p ->
            createUserProjectRole(user, p, projectRolesSet, flags)
        }

        UserProjectRole userProjectRole = CollectionUtils.exactlyOneElement(UserProjectRole.findAllByProjectAndUser(project, user))
        if (userProjectRole.accessToFiles) {
            notifyUsersAboutFileAccessChange(userProjectRole)
        }

        notifyProjectAuthoritiesAndUser(userProjectRole)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addExternalUserToProject(Project project, String realName, String email, Set<ProjectRole> projectRoles) throws AssertionError {
        assert project: "project must not be null"

        User user = CollectionUtils.atMostOneElement(User.findAllByEmailAndUsernameIsNull(email))
        if (user) {
            assert user.realName == realName: "The given email address '${user.email}' is already registered for external user '${user.realName}'"
        } else {
            user = userService.createUser(null, email, realName)
        }

        applyToRelatedProjects(project.unixGroup) { Project p ->
            createUserProjectRole(user, p, projectRoles)
        }

        UserProjectRole userProjectRole = CollectionUtils.exactlyOneElement(UserProjectRole.findAllByProjectAndUser(project, user))

        notifyProjectAuthoritiesAndUser(userProjectRole)
    }

    private void notifyUsersAboutFileAccessChange(UserProjectRole userProjectRole) {
        Project project = userProjectRole.project
        User user = userProjectRole.user

        String subject = messageSourceService.createMessage("projectUser.notification.fileAccessChange.subject", [
                projectName: project.name,
        ])

        String clusterAdministrationEmail = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION)
        String clusterName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.CLUSTER_NAME)
        String supportTeamName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME)
        String body = messageSourceService.createMessage("projectUser.notification.fileAccessChange.body", [
                username                  : user.realName,
                requester                 : mailHelperService.senderName,
                projectName               : project.name,
                dirAnalysis               : project.dirAnalysis ?: "-",
                clusterName               : clusterName,
                clusterAdministrationEmail: clusterAdministrationEmail,
                supportTeamSalutation     : supportTeamName,
                linkProjectDirectory      : LsdfFilesService.getPath(configService.rootPath.path, project.dirName),
        ])

        List<String> ccs = getUniqueProjectAuthoritiesAndUserManagers(project)*.email.sort()

        mailHelperService.sendEmail(subject, body, user.email, ccs)
    }

    private void notifyAdministration(UserProjectRole userProjectRole, OperatorAction action) {
        User requester = securityService.currentUser
        UserProjectRole requesterUserProjectRole = CollectionUtils.atMostOneElement(
                UserProjectRole.findAllByUserAndProject(requester, userProjectRole.project)
        )
        String switchedUserAnnotation = securityService.switched ? " (switched from ${securityService.userSwitchInitiator.username})" : ""

        String formattedAction = action.toString().toLowerCase()
        String conjunction = action == OperatorAction.ADD ? 'to' : 'from'

        CommandAndResult command = executeOrNotify(userProjectRole, action)
        String scriptCommand
        String subjectPrefix = ""
        if (command.output) {
            String success
            if (command.output.exitCode == 0) {
                success = "successfully"
                subjectPrefix = 'DONE: '
            } else {
                success = "unsuccessfully"
                subjectPrefix = 'ERROR: '
            }
            scriptCommand = "Command '${command.command}' was executed ${success}.\n" +
                    "Output:\n${command.output.stdout ?: '-'}\nError:\n${command.output.stderr ?: '-'}"
        } else {
            scriptCommand = "Command to execute:\n${command.command}"
        }

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
                role    : userProjectRole.projectRoles*.name.join(", "),
        ])
        String requesterUserDetail = messageSourceService.createMessage("projectUser.notification.addToUnixGroup.userDetail", [
                realName: requester.realName,
                username: requester.username + switchedUserAnnotation,
                email   : requester.email,
                role    : requesterUserProjectRole ? requesterUserProjectRole.projectRoles*.name.join(", ") : "Non-Project-User",
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

        mailHelperService.sendEmailToTicketSystem("${subjectPrefix}${subject}", body)
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_SENT_MAIL,
                "Sent mail to ${mailHelperService.ticketSystemEmailAddress} to ${formattedAction} ${userProjectRole.user.username} ${conjunction} " +
                        "${userProjectRole.project.name} at the request of ${requester.username + switchedUserAnnotation}")
    }

    private void notifyProjectAuthoritiesAndUser(UserProjectRole userProjectRole) {
        List<String> projectRoleNames = userProjectRole.projectRoles*.name
        String projectName = userProjectRole.project.name

        String subject = messageSourceService.createMessage("projectUser.notification.newProjectMember.subject", [projectName: projectName])
        String body = messageSourceService.createMessage("projectUser.notification.newProjectMember.body.userManagerAddedMember", [
                userIdentifier: userProjectRole.user.realName ?: userProjectRole.user.username,
                projectRole   : projectRoleNames.join(", "),
                projectName   : projectName,
                executingUser : mailHelperService.senderName,
        ])

        List<User> projectAuthoritiesAndUserManagers = getUniqueProjectAuthoritiesAndUserManagers(userProjectRole.project)
        List<String> recipients = projectAuthoritiesAndUserManagers*.email.unique().sort() + [userProjectRole.user.email]

        if (recipients) {
            mailHelperService.sendEmail(subject, body, recipients)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_SENT_MAIL,
                    "Notified project authorities (${projectAuthoritiesAndUserManagers*.realName.join(", ")}) and user (${userProjectRole.user.username})")
        }
    }

    private void notifyProjectAuthoritiesAndDisabledUser(UserProjectRole userProjectRole) {
        List<User> projectAuthoritiesAndUserManagers = getUniqueProjectAuthoritiesAndUserManagers(userProjectRole.project)
        List<String> recipient = [userProjectRole.user]*.email
        List<String> ccs = projectAuthoritiesAndUserManagers*.email.unique().sort()

        String subject = messageSourceService.createMessage("projectUser.notification.userDeactivated.subject", [
                project: userProjectRole.project.name,
        ])
        String body = messageSourceService.createMessage("projectUser.notification.userDeactivated.body", [
                user                 : "${userProjectRole.user.realName} (${userProjectRole.user.username})",
                project              : userProjectRole.project.name,
                supportTeamSalutation: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient, ccs)
    }

    private static String getFlagChangeLogMessage(String flagName, boolean newStatus, String username, String projectName) {
        return "${flagName} ${newStatus ? "en" : "dis"}abled for ${username} in ${projectName}"
    }

    /**
     * Get all UserProjectRoles by given UserProjectRole where the same unix group was used.
     *
     * @param userProjectRole an UserProjectRole with target unix group
     * @return all related UserProjectRole with same unix group
     */
    List<UserProjectRole> getRelatedUserProjectRoles(UserProjectRole userProjectRole) {
        List<Project> projectsWithSharedUnixGroup = Project.findAllByUnixGroup(userProjectRole.project.unixGroup)
        return UserProjectRole.findAllByUserAndProjectInList(userProjectRole.user, projectsWithSharedUnixGroup)
    }

    /**
     * Applies a function for each UserProjectRole of related UserProjectRoles.
     *
     * @param userProjectRole an UserProjectRole for that related UserProjectRoles are searched
     * @param action Closure to be called on each UserProjectRole
     */
    void applyToRelatedUserProjectRoles(UserProjectRole userProjectRole, Closure action) {
        UserProjectRole.withTransaction {
            getRelatedUserProjectRoles(userProjectRole).each { UserProjectRole relatedUpr ->
                action(relatedUpr)
            }
        }
    }

    /**
     * Checks if all UserProjectRole match the applied predicate closure.
     *
     * @param consistencyCheck Closure to be called on each UserProjectRole
     */
    boolean checkRelatedUserProjectRolesFor(UserProjectRole userProjectRole, Closure consistencyCheck) {
        return getRelatedUserProjectRoles(userProjectRole).every(consistencyCheck)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole setAccessToOtp(UserProjectRole userProjectRole, boolean accessToOtp) {
        if (checkRelatedUserProjectRolesFor(userProjectRole) { UserProjectRole upr -> upr.accessToOtp == accessToOtp }) {
            return userProjectRole
        }
        applyToRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.accessToOtp = accessToOtp
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Access to OTP", upr.accessToOtp, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ACCESS_TO_OTP, message)
        }
        return userProjectRole
    }

    /**
     * Set accessToFiles for the given UserProjectRole to the given value.
     * Also set the fileAccessChangeRequested and trigger the notification for the administration.
     *
     * if force is set neither fileAccessChangeRequested is set nor the administration gets notified.
     *
     * @param userProjectRole UserProjectRole to change
     * @param force The flag to determined whether the change should requested or set immediately
     * @return The changed UserProjectRole
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole setAccessToFiles(UserProjectRole userProjectRole, boolean accessToFiles, boolean force = false) {
        if (checkRelatedUserProjectRolesFor(userProjectRole) { UserProjectRole upr -> upr.accessToFiles == accessToFiles }) {
            return userProjectRole
        }
        applyToRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.accessToFiles = accessToFiles
            upr.fileAccessChangeRequested = !force
            assert upr.save(flush: true)
            if (force) {
                String message = getFlagChangeLogMessage("Take the state over of file access over from the ldap",
                        upr.accessToFiles, upr.user.username, upr.project.name)
                auditLogService.logActionWithSystemUser(AuditLog.Action.LDAP_BASED_CHANGED_ACCESS_TO_FILES, message)
            } else {
                String message = getFlagChangeLogMessage("Access to Files", upr.accessToFiles, upr.user.username, upr.project.name)
                auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ACCESS_TO_FILES, message)
            }
        }
        if (!force) {
            if (userProjectRole.accessToFiles) {
                notifyAdministration(userProjectRole, OperatorAction.ADD)
            } else {
                notifyAdministration(userProjectRole, OperatorAction.REMOVE)
            }
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole setAccessToFilesWithUserNotification(UserProjectRole userProjectRole, boolean accessToFiles) {
        setAccessToFiles(userProjectRole, accessToFiles)
        if (userProjectRole.accessToFiles) {
            notifyUsersAboutFileAccessChange(userProjectRole)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'DELEGATE_USER_MANAGEMENT')")
    UserProjectRole setManageUsers(UserProjectRole userProjectRole, boolean value) {
        if (checkRelatedUserProjectRolesFor(userProjectRole) { UserProjectRole upr -> upr.manageUsers == value }) {
            return userProjectRole
        }
        applyToRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.manageUsers = value
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Manage Users", upr.manageUsers, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_MANAGE_USER, message)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    UserProjectRole setManageUsersAndDelegate(UserProjectRole userProjectRole, boolean value) {
        if (checkRelatedUserProjectRolesFor(userProjectRole) { UserProjectRole upr -> upr.manageUsersAndDelegate == value }) {
            return userProjectRole
        }
        applyToRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.manageUsersAndDelegate = value
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Delegate Manage Users", upr.manageUsersAndDelegate, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_DELEGATE_MANAGE_USER, message)
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS') or #userProjectRole.user.username == principal.username")
    UserProjectRole setReceivesNotifications(UserProjectRole userProjectRole, boolean value) {
        if (checkRelatedUserProjectRolesFor(userProjectRole) { UserProjectRole upr -> upr.receivesNotifications == value }) {
            return userProjectRole
        }
        applyToRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.receivesNotifications = value
            assert upr.save(flush: true)
            String message = getFlagChangeLogMessage("Receives Notification", upr.receivesNotifications, upr.user.username, upr.project.name)
            auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_RECEIVES_NOTIFICATION, message)
        }
        return userProjectRole
    }

    /**
     * Sets the activated flag of UserProjectRole and informs responsible persons about the change. If a User gets disabled all
     * permissions are set to false. If a User gets enabled the permissions are not granted automatically.
     *
     * @param userProjectRole the UserProjectRole to change
     * @param value which will be set
     * @return updated UserProjectRole
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS') or hasPermission(#userProjectRole, 'IS_USER')")
    UserProjectRole setEnabled(UserProjectRole userProjectRole, boolean value) {
        User currentUser = securityService.currentUser
        if (value && userProjectRole.user == currentUser && !securityService.hasCurrentUserAdministrativeRoles()) {
            throw new InsufficientRightsException("You are not allowed to reactivate your own user! Please ask your administrator.")
        }
        if (userProjectRole.user != currentUser && !securityService.hasCurrentUserAdministrativeRoles() && userProjectRole.isPi()) {
            throw new InsufficientRightsException("You don't have enough rights to execute this operation! Please ask your administrator.")
        }
        if (!userProjectRole.user.enabled && value) {
            throw new OtpRuntimeException("User is disabled.")
        }
        boolean hadFileAccess = userProjectRole.accessToFiles
        if (checkRelatedUserProjectRolesFor(userProjectRole) { UserProjectRole upr -> upr.enabled == value }) {
            return userProjectRole
        }
        doSetEnabled(userProjectRole, value)
        if (value) {
            notifyProjectAuthoritiesAndUser(userProjectRole)
        } else {
            notifyProjectAuthoritiesAndDisabledUser(userProjectRole)
            if (hadFileAccess) {
                notifyAdministration(userProjectRole, OperatorAction.REMOVE)
            }
        }
        String message = "${value ? "En" : "Dis"}abled ${userProjectRole.user.username} for ${userProjectRole.project.name}"
        auditLogService.logAction(AuditLog.Action.PROJECT_USER_CHANGED_ENABLED, message)
        return userProjectRole
    }

    void doSetEnabled(UserProjectRole userProjectRole, boolean value) {
        applyToRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.enabled = value
            // users should be reactivated without any further permissions
            upr.accessToOtp = false
            upr.accessToFiles = false
            upr.manageUsers = false
            upr.manageUsersAndDelegate = false
            upr.receivesNotifications = false
            assert upr.save(flush: true)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole addProjectRolesToProjectUserRole(UserProjectRole userProjectRole, List<ProjectRole> newProjectRoles) {
        newProjectRoles.each { ProjectRole newProjectRole ->
            if (checkRelatedUserProjectRolesFor(userProjectRole) { UserProjectRole upr -> newProjectRole in upr.projectRoles }) {
                return
            }
            applyToRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
                upr.projectRoles.add(newProjectRole)
                assert upr.save(flush: true)
            }
        }
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole deleteProjectUserRole(UserProjectRole userProjectRole, ProjectRole currentProjectRole) {
        assert currentProjectRole in userProjectRole.projectRoles
        if (currentProjectRole.name == ProjectRole.Basic.PI.name() && !securityService.hasCurrentUserAdministrativeRoles()) {
            throw new OtpRuntimeException("Cannot remove role ${ProjectRole.Basic.PI.name()}. Please ask an administrator!")
        }
        if (userProjectRole.projectRoles.size() <= 1) {
            throw new OtpRuntimeException("A user must have at least one role!")
        }
        applyToRelatedUserProjectRoles(userProjectRole) { UserProjectRole upr ->
            upr.projectRoles.remove(currentProjectRole)
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
                    projectRoles             : new HashSet<ProjectRole>(userProjectRole.projectRoles),
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

    List<UserProjectRole> getProjectUsersToBeNotified(List<Project> projects) {
        return UserProjectRole.withCriteria {
            'in'('project', projects)
            eq("receivesNotifications", true)
            eq("enabled", true)
            user {
                eq("enabled", true)
            }
        } as List<UserProjectRole>
    }

    List<String> getEmailsOfToBeNotifiedProjectUsers(List<Project> projects) {
        return getProjectUsersToBeNotified(projects)*.user*.email
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

    private static List<User> getUniqueProjectAuthoritiesAndUserManagers(Project project) {
        return (getUserManagers(project) + getProjectAuthorities(project)).unique()
    }

    private static List<User> getUserManagers(Project project) {
        return UserProjectRole.findAllByProjectAndManageUsersAndEnabled(project, true, true)*.user
    }

    static List<User> getProjectAuthorities(Project project) {
        return UserProjectRole.createCriteria().list {
            eq("project", project)
            eq("enabled", true)
            user {
                eq("enabled", true)
            }
            projectRoles {
                'in'("name", ProjectRole.AUTHORITY_PROJECT_ROLES)
            }
        }*.user
    }

    static List<User> getBioinformaticianUsers(Project project) {
        return UserProjectRole.createCriteria().list {
            eq("project", project)
            eq("enabled", true)
            user {
                eq("enabled", true)
            }
            projectRoles {
                'in'("name", ProjectRole.BIOINFORMATICIAN_PROJECT_ROLES)
            }
        }*.user
    }

    CommandAndResult executeOrNotify(UserProjectRole userProjectRole, OperatorAction action) {
        String command = userProjectRoleService.commandTemplate(userProjectRole, action)
        if (processingOptionService.findOptionAsBoolean(ProcessingOption.OptionName.AD_GROUP_USER_SNIPPET_EXECUTE)) {
            ProcessOutput processOutput = remoteShellHelper.executeCommandReturnProcessOutput(userProjectRole.project.realm, command)
            return new CommandAndResult(command, processOutput)
        }
        return new CommandAndResult(command, null)
    }

    protected String commandTemplate(UserProjectRole userProjectRole, OperatorAction action) {
        return commandTemplate(userProjectRole.project.unixGroup, userProjectRole.user.username, action)
    }

    protected String commandTemplate(String unixGroup, String username, OperatorAction action) {
        return new SimpleTemplateEngine()
                .createTemplate(processingOptionService.findOptionAsString(action.commandTemplateOptionName))
                .make([unixGroup: unixGroup, username: username])
                .toString()
    }

    @TupleConstructor
    enum OperatorAction {
        ADD(ProcessingOption.OptionName.AD_GROUP_ADD_USER_SNIPPET),
        REMOVE(ProcessingOption.OptionName.AD_GROUP_REMOVE_USER_SNIPPET),

        final ProcessingOption.OptionName commandTemplateOptionName
    }

    @TupleConstructor
    static class CommandAndResult {
        String command
        ProcessOutput output
    }
}

class LdapUserCreationException extends RuntimeException {
    LdapUserCreationException(String message) {
        super(message)
    }
}
