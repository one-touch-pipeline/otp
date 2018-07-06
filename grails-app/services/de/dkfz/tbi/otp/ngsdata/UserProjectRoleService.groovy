package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.security.access.prepost.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class UserProjectRoleService {

    MailHelperService mailHelperService
    UserService userService
    LdapService ldapService

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole createUserProjectRole(User user, Project project, ProjectRole projectRole, boolean enabled = true, boolean manageUsers) {
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

        Role role = Role.findByAuthority("ROLE_USER")
        if (!UserRole.findByUserAndRole(user, role)) {
            UserRole.create(user, role, true)
        }

        if (projectRole.accessToFiles) {
            requestToAddUserToUnixGroupIfRequired(userProjectRole.user, userProjectRole.project)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    void requestToAddUserToUnixGroupIfRequired(User user, Project project) {
        String[] groupNames = ldapService.getGroupsOfUserByUsername(user.username)
        if (!(project.unixGroup in groupNames)) {
            notifyAdministration(user, project, AdtoolAction.ADD)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    void requestToRemoveUserFromUnixGroupIfRequired(User user, Project project) {
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

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    void notifyAdministration(User user, Project project, AdtoolAction adtool) {
        String formattedAction = adtool.toString().toLowerCase()
        String subject = "Request to ${formattedAction} user '${user.username}' ${adtool == AdtoolAction.ADD ? 'to' : 'from'} project '${project.name}'"
        String body = "adtool group${formattedAction}user ${project.name} ${user.username}"
        String email = ProcessingOptionService.getValueOfProcessingOption(EMAIL_LINUX_GROUP_ADMINISTRATION)
        mailHelperService.sendEmail(subject, body, email)
    }

    private enum AdtoolAction {
        ADD, REMOVE
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'DELEGATE_USER_MANAGEMENT')")
    UserProjectRole updateManageUsers(UserProjectRole userProjectRole, boolean manageUsers) {
        assert userProjectRole: "the input userProjectRole must not be null"
        userProjectRole.manageUsers = manageUsers
        assert userProjectRole.save(flush: true, failOnError: true)
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
        return userProjectRole
    }
}
