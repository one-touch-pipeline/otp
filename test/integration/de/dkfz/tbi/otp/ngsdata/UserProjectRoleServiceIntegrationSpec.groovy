package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import spock.lang.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class UserProjectRoleServiceIntegrationSpec extends Specification implements UserAndRoles {
    UserProjectRoleService userProjectRoleService = new UserProjectRoleService()

    def setup() {
        createUserAndRoles()
    }

    void "test manageUsers is removed and not granted when updating the project role"() {
        given:
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: User.findByUsername(USER),
                projectRole: DomainFactory.createProjectRole(accessToOtp: true),
                manageUsers: manageUsers,
        )
        ProjectRole newRole = DomainFactory.createProjectRole(
                accessToOtp: true,
                manageUsersAndDelegate: manageUsersAndDelegate,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.updateProjectRole(userProjectRole, newRole)
        }
        userProjectRole.projectRole.manageUsersAndDelegate = false

        then:
        !userProjectRole.manageUsers

        where:
        manageUsersAndDelegate | manageUsers
        false                  | false
        false                  | true
        true                   | false
        true                   | true
    }

    void "test updateProjectRole on valid input"() {
        given:
        ProjectRole newRole = DomainFactory.createProjectRole(accessToOtp: true)
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: User.findByUsername(USER),
                projectRole: DomainFactory.createProjectRole(accessToOtp: true),
                manageUsers: false,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.updateProjectRole(userProjectRole, newRole)
        }

        then:
        userProjectRole.projectRole == newRole
    }

    void "updateManageUsers updates properly"() {
        given:
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: User.findByUsername(USER),
                manageUsers: oldStatus,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.updateManageUsers(userProjectRole, newStatus)
        }

        then:
        userProjectRole.manageUsers == result

        where:
        oldStatus | newStatus || result
        false     | false     || false
        false     | true      || true
        true      | false     || false
        true      | true      || true
    }

    void "notifyAdministration sends email with correct content"() {
        given:
        DomainFactory.createProcessingOptionLazy(
                name: EMAIL_LINUX_GROUP_ADMINISTRATION,
                type: null,
                project: null,
                value: "administrationMail@dummy.com",
        )
        userProjectRoleService.mailHelperService = Mock(MailHelperService)
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole()

        when:
        userProjectRoleService.notifyAdministration(userProjectRole)

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                "Request to add user '${userProjectRole.user.username}' to project '${userProjectRole.project.name}'",
                "adtool groupadduser ${userProjectRole.project.name} ${userProjectRole.user.username}",
                ProcessingOptionService.getValueOfProcessingOption(EMAIL_LINUX_GROUP_ADMINISTRATION)
        )
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, create User if non is found for username or email"() {
        given:
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn:       "unknownUser",
                realName: "Unknown User",
                mail:     "unknownUser@dummy.com",
        )
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        userProjectRoleService.userService = Spy(UserService)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsernameOrMailOrRealName(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        1 * userProjectRoleService.userService.createUser(_)
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, throw exception when user is already connected to project"() {
        given:
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject()
        DomainFactory.createUserProjectRole(
                user:    user,
                project: project
        )
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn:       user.username,
                realName: user.realName,
                mail:     user.email,
        )
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsernameOrMailOrRealName(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, DomainFactory.createProjectRole(), "search")
        }

        then:
        def e = thrown(AssertionError)
        e.message.contains('is already part of project')
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, create UserProjectRole for new User"() {
        given:
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn:       user.username,
                realName: user.realName,
                mail:     user.email,
        )
        userProjectRoleService = Spy(UserProjectRoleService)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsernameOrMailOrRealName(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        1 * userProjectRoleService.createUserProjectRole(user, project, projectRole, true, false)
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, unsuccessful ldap search throws exception"() {
        given:
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsernameOrMailOrRealName(_) >> null
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        def e = thrown(AssertionError)
        e.message.contains('can not be resolved to a user via LDAP')
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, send mail for users with access to files"() {
        given:
        DomainFactory.createProcessingOptionLazy(
                name: EMAIL_LINUX_GROUP_ADMINISTRATION,
                type: null,
                project: null,
                value: "administrationMail@dummy.com",
        )
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole(accessToFiles: true)
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn:       user.username,
                realName: user.realName,
                mail:     user.email,
        )
        userProjectRoleService = Spy(UserProjectRoleService)
        userProjectRoleService.userService = Mock(UserService)
        userProjectRoleService.mailHelperService = Mock(MailHelperService)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsernameOrMailOrRealName(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        1 * userProjectRoleService.createUserProjectRole(user, project, projectRole, true, false)
        1 * userProjectRoleService.mailHelperService.sendEmail(
                "Request to add user '${user.username}' to project '${project.name}'",
                "adtool groupadduser ${project.name} ${user.username}",
                ProcessingOptionService.getValueOfProcessingOption(EMAIL_LINUX_GROUP_ADMINISTRATION)
        )
    }
}
