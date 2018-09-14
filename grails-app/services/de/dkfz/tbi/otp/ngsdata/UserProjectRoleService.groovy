package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.security.access.prepost.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.security.AuditLog.Action.*

class UserProjectRoleService {

    MailHelperService mailHelperService
    UserService userService
    LdapService ldapService
    AuditLogService auditLogService
    ProcessingOptionService processingOptionService

    private UserProjectRole createUserProjectRole(User user, Project project, ProjectRole projectRole, boolean enabled = true, boolean manageUsers) {
        assert user : "the user must not be null"
        assert project : "the project must not be null"
        UserProjectRole userProjectRole = new UserProjectRole([
                user        : user,
                project     : project,
                projectRole : projectRole,
                enabled     : enabled,
                manageUsers : manageUsers,
        ])
        userProjectRole.save(flush: true, failOnError: true)
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addUserToProjectAndNotifyGroupManagementAuthority(Project project, ProjectRole projectRole, String username) throws AssertionError {
        assert project : "project must not be null"

        LdapUserDetails ldapUserDetails = ldapService.getLdapUserDetailsByUsername(username)
        assert ldapUserDetails : "'${username}' can not be resolved to a user via LDAP"
        assert ldapUserDetails.mail : "Could not get a mail for this user via LDAP"

        User user = User.findByUsernameOrEmail(ldapUserDetails.cn, ldapUserDetails.mail)
        if (!user) {
            user = userService.createUser(new CreateUserCommand([
                    username: ldapUserDetails.cn,
                    email: ldapUserDetails.mail,
                    realName: ldapUserDetails.realName,
            ]))
        }
        UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
        assert !userProjectRole : "User '${user.username}' is already part of project '${project.name}'"
        userProjectRole = createUserProjectRole(user, project, projectRole, true, false)

        if (projectRole.accessToFiles) {
            requestToAddUserToUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
        }
        auditLogService.logAction(PROJECT_USER_CHANGED_ENABLED, "Enabled ${userProjectRole.user.username} for ${userProjectRole.project.name}")
        auditLogService.logAction(PROJECT_USER_CHANGED_PROJECT_ROLE, "Project Role ${projectRole.name} granted to ${userProjectRole.user.username} in ${userProjectRole.project.name}")
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'MANAGE_USERS')")
    void addExternalUserToProject(Project project, String realName, String email, ProjectRole projectRole) throws AssertionError {
        assert project : "project must not be null"

        User user = User.findByEmail(email)
        if (!user) {
            user = userService.createUser(new CreateUserCommand([
                    email: email,
                    realName: realName,
            ]))
        }
        UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
        assert !userProjectRole : "User '${user.realName}' is already part of project '${project.name}'"
        userProjectRole = createUserProjectRole(user, project, projectRole, true, false)
        auditLogService.logAction(PROJECT_USER_CHANGED_ENABLED, "Enabled ${userProjectRole.user.realName} for ${userProjectRole.project.name}")
        auditLogService.logAction(PROJECT_USER_CHANGED_PROJECT_ROLE, "Project Role ${projectRole.name} granted to ${userProjectRole.user.username} in ${userProjectRole.project.name}")
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
            !UserProjectRole.findAllByUserAndProjectInListAndProjectRoleInListAndEnabled(
                    user,
                    Project.findAllByUnixGroupAndIdNotEqual(project.unixGroup, project.id),
                    ProjectRole.findAllByAccessToFiles(true),
                    true)
        ) {
            notifyAdministration(user, project, AdtoolAction.REMOVE)
        }
    }

    private void notifyAdministration(User user, Project project, AdtoolAction adtool) {
        String formattedAction = adtool.toString().toLowerCase()
        String subject = "Request to ${formattedAction} user '${user.username}' ${adtool == AdtoolAction.ADD ? 'to' : 'from'} project '${project.name}'"
        String body = "adtool group${formattedAction}user ${project.name} ${user.username}"
        String email = processingOptionService.findOptionAsString(EMAIL_LINUX_GROUP_ADMINISTRATION)
        mailHelperService.sendEmail(subject, body, email)
        auditLogService.logAction(PROJECT_USER_SENT_MAIL, "Sent mail to ${email} to ${formattedAction} ${user.username} ${adtool == AdtoolAction.ADD ? 'to' : 'from'} ${project.name}")
    }

    private enum AdtoolAction {
        ADD, REMOVE
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'DELEGATE_USER_MANAGEMENT')")
    UserProjectRole updateManageUsers(UserProjectRole userProjectRole, boolean manageUsers) {
        assert userProjectRole: "the input userProjectRole must not be null"
        userProjectRole.manageUsers = manageUsers
        assert userProjectRole.save(flush: true, failOnError: true)
        auditLogService.logAction(PROJECT_USER_CHANGED_MANAGE_USER, "Manage User ${manageUsers ? "en" : "dis"}abled for ${userProjectRole.user.username} in ${userProjectRole.project.name} with role ${userProjectRole.projectRole.name}")
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole updateEnabledStatus(UserProjectRole userProjectRole, boolean enabled) {
        assert userProjectRole: "the input userProjectRole must not be null"
        userProjectRole.enabled = enabled
        assert userProjectRole.save(flush: true, failOnError: true)
        if (userProjectRole.projectRole.accessToFiles) {
            if (enabled) {
                requestToAddUserToUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
            } else {
                requestToRemoveUserFromUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
            }
        }
        auditLogService.logAction(PROJECT_USER_CHANGED_ENABLED, "${enabled ? "En" : "Dis"}abled ${userProjectRole.user.username} for ${userProjectRole.project.name}")
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole updateProjectRole(UserProjectRole userProjectRole, ProjectRole newProjectRole) {
        assert userProjectRole: "the input userProjectRole must not be null"
        assert newProjectRole: "the input projectRole must not be null"
        ProjectRole oldProjectRole = userProjectRole.projectRole
        updateManageUsers(userProjectRole, false)
        userProjectRole.projectRole = newProjectRole
        assert userProjectRole.save(flush: true, failOnError: true)
        if (oldProjectRole.accessToFiles != newProjectRole.accessToFiles) {
            if (newProjectRole.accessToFiles) {
                requestToAddUserToUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
            } else {
                requestToRemoveUserFromUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
            }
        }
        auditLogService.logAction(PROJECT_USER_CHANGED_PROJECT_ROLE, "Project Role updated from ${oldProjectRole.name} to ${newProjectRole.name} for ${userProjectRole.user.username} in ${userProjectRole.project.name}")
        return userProjectRole
    }
}
