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
    void addUserToProjectAndNotifyGroupManagementAuthority(Project project, ProjectRole projectRole, String searchString) throws AssertionError {
        assert project : "project must not be null"

        LdapUserDetails ldapUserDetails = ldapService.getLdapUserDetailsByUsernameOrMailOrRealName(searchString)
        assert ldapUserDetails : "'${searchString}' can not be resolved to a user via LDAP"
        assert ldapUserDetails.mail : "Could not get a mail for this user via LDAP"

        User user = User.findByUsernameOrEmail(ldapUserDetails.cn, ldapUserDetails.mail)
        if (!user) {
            user = userService.createUser(new CreateUserCommand([
                    username: ldapUserDetails.cn,
                    email: ldapUserDetails.mail,
                    realName: ldapUserDetails.realName,
            ]))
            assert user : "user could not be created"
        }
        UserProjectRole userProjectRole = UserProjectRole.findByUserAndProject(user, project)
        assert !userProjectRole : "User '"+user.username+"' is already part of project '"+project.name+"'"

        userProjectRole = createUserProjectRole(user, project, projectRole, true, false)

        if (projectRole.accessToFiles) {
            notifyAdministration(userProjectRole)
        }
    }

    void notifyAdministration(UserProjectRole userProjectRole) {
        String subject = "Request to add user '"+userProjectRole.user.username+"' to project '"+userProjectRole.project.name+"'"
        String body = "adtool groupadduser ${userProjectRole.project.name} ${userProjectRole.user.username}"
        String email = ProcessingOptionService.getValueOfProcessingOption(EMAIL_LINUX_GROUP_ADMINISTRATION)
        mailHelperService.sendEmail(subject, body, email)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'DELEGATE_USER_MANAGEMENT')")
    UserProjectRole updateManageUsers(UserProjectRole userProjectRole, boolean manageUsers) {
        assert userProjectRole: "the input userProjectRole must not be null"
        userProjectRole.manageUsers = manageUsers
        assert userProjectRole.save(flush: true, failOnError: true)
        return userProjectRole
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#userProjectRole.project, 'MANAGE_USERS')")
    UserProjectRole updateProjectRole(UserProjectRole userProjectRole, ProjectRole newProjectRole) {
        assert userProjectRole: "the input userProjectRole must not be null"
        assert newProjectRole: "the input projectRole must not be null"
        updateManageUsers(userProjectRole, false)
        userProjectRole.projectRole = newProjectRole
        assert userProjectRole.save(flush: true, failOnError: true)
        return userProjectRole
    }
}
