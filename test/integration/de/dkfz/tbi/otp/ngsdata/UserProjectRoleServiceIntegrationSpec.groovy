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
    UserProjectRoleService userProjectRoleService

    def setup() {
        userProjectRoleService = new UserProjectRoleService()
        userProjectRoleService.auditLogService = new AuditLogService()
        userProjectRoleService.auditLogService.springSecurityService = new SpringSecurityService()
        createUserAndRoles()
        userProjectRoleService.mailHelperService = Mock(MailHelperService)
        DomainFactory.createProcessingOptionLazy(
                name: EMAIL_LINUX_GROUP_ADMINISTRATION,
                type: null,
                project: null,
                value: "administrationMail@dummy.com",
        )
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
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject()
        String formattedAction = adtoolaction.toString().toLowerCase()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.notifyAdministration(user, project, adtoolaction)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                "Request to ${formattedAction} user '${user.username}' ${conjunction} project '${project.name}'",
                "adtool group${formattedAction}user ${project.name} ${user.username}",
                ProcessingOptionService.getValueOfProcessingOption(EMAIL_LINUX_GROUP_ADMINISTRATION)
        )

        where:
        conjunction | adtoolaction
        "to"        | UserProjectRoleService.AdtoolAction.ADD
        "from"      | UserProjectRoleService.AdtoolAction.REMOVE
    }

    void "updateEnabledStatus, send mails when activating a role with file access"() {
        given:
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: DomainFactory.createUser(),
                project: DomainFactory.createProject(unixGroup: "unix_group"),
                projectRole: DomainFactory.createProjectRole(accessToFiles: accessToFiles),
        )

        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> ldapResult
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.updateEnabledStatus(userProjectRole, valueToUpdate)
        }

        then:
        userProjectRole.enabled == valueToUpdate
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _)

        where:
        ldapResult     | accessToFiles | valueToUpdate | invocations
        ["some_group"] | true          | false         | 0
        ["some_group"] | true          | true          | 1
        ["some_group"] | false         | false         | 0
        ["some_group"] | false         | true          | 0
        ["unix_group"] | true          | false         | 1
        ["unix_group"] | true          | true          | 0
        ["unix_group"] | false         | false         | 0
        ["unix_group"] | false         | true          | 0
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, create User if non is found for username or email"() {
        given:
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn:       "unknownUser",
                realName: "Unknown User",
                mail:     "unknownUser@dummy.com",
        )
        userProjectRoleService.userService = new UserService()
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        expect:
        User.findByUsernameAndEmail(ldapUserDetails.cn, ldapUserDetails.mail) == null

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        User.findByUsernameAndEmail(ldapUserDetails.cn, ldapUserDetails.mail)
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, throw exception when user is already connected to project"() {
        given:
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
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
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
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
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        expect:
        UserProjectRole.findByUserAndProjectAndProjectRole(user, project, projectRole) == null

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        UserProjectRole.findByUserAndProjectAndProjectRole(user, project, projectRole)
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, create UserRole with role ROLE_USER if it does not exist"() {
        given:
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn:       user.username,
                realName: user.realName,
                mail:     user.email,
        )
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        expect:
        UserRole.findByUserAndRole(user, Role.findByAuthority("ROLE_USER")) == null

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        UserRole.findByUserAndRole(user, Role.findByAuthority("ROLE_USER"))
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, unsuccessful ldap search throws exception"() {
        given:
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> null
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        def e = thrown(AssertionError)
        e.message.contains('can not be resolved to a user via LDAP')
    }

    void "addUserToProjectAndNotifyGroupManagementAuthorities, send mail only for users with access to files"() {
        given:
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject(unixGroup: "UNIX_GROUP")
        ProjectRole projectRole = DomainFactory.createProjectRole(accessToFiles: fileAccess)
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn:       user.username,
                realName: user.realName,
                mail:     user.email,
        )
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
            getGroupsOfUserByUsername(_) >> []
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, "search")
        }

        then:
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _)

        where:
        fileAccess | invocations
        true       | 1
        false      | 0
    }

    void "addExternalUserToProject, create User if non is found for realName or email"() {
        given:
        ProjectRole projectRole = DomainFactory.createProjectRole()
        String realName = "realName"
        String email = "email@dummy.de"
        Project project = DomainFactory.createProject()
        userProjectRoleService.userService = new UserService()

        expect:
        User.findByEmail(email) == null

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, realName, email, projectRole)
        }

        then:
        User.findByEmail(email)
    }

    void "addExternalUserToProject, throw exception when user is already connected to project"() {
        given:
        String email = "email@dummy.de"
        ProjectRole projectRole = DomainFactory.createProjectRole()
        Project project = DomainFactory.createProject()
        User user = DomainFactory.createUser(
                realName: "realName",
                email: email,
        )
        DomainFactory.createUserProjectRole(
                user:    user,
                project: project,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, "realName", email, projectRole)
        }

        then:
        def e = thrown(AssertionError)
        e.message.contains('is already part of project')
    }

    void "addExternalUserToProject, create UserProjectRole for new User"() {
        given:
        ProjectRole projectRole = DomainFactory.createProjectRole()
        User user = DomainFactory.createUser(username: null)
        Project project = DomainFactory.createProject()

        expect:
        UserProjectRole.findByUserAndProjectAndProjectRole(user, project, projectRole) == null

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, user.realName, user.email, projectRole)
        }

        then:
        UserProjectRole.findByUserAndProjectAndProjectRole(user, project, projectRole)
    }

    void "requestToAddUserToUnixGroupIfRequired, only send notification when user is not already in group"() {
        given:
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject(unixGroup: "UNIX_GROUP")
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> unixGroupsOfUser
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToAddUserToUnixGroupIfRequired(user, project)
        }
        then:
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _)

        where:
        invocations | unixGroupsOfUser
        0           | ["UNIX_GROUP"]
        0           | ["UNIX_GROUP", "OTHER_GROUP"]
        1           | ["OTHER_GROUP"]
        1           | []
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, user is not in projects with shared unix groups or no others exist"() {
        given:
        Project project = DomainFactory.createProject(unixGroup: unixGroup1)
        DomainFactory.createProject(unixGroup: unixGroup2)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [unixGroup1]
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(DomainFactory.createUser(), project)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _)

        where:
        unixGroup1 | unixGroup2
        "shared"   | "shared"
        "shared"   | "not_shared"
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, removed user has no remaining file access role in projects with shared unix group"() {
        given:
        String unixGroup = "unix_group"
        User user = DomainFactory.createUser()
        Project project1 = DomainFactory.createProject(unixGroup: unixGroup)
        Project project2 = DomainFactory.createProject(unixGroup: unixGroup)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [unixGroup]
        }
        DomainFactory.createUserProjectRole(
                user: user,
                project: project1,
                projectRole: DomainFactory.createProjectRole(accessToFiles: true),
        )
        DomainFactory.createUserProjectRole(
                user: user,
                project: project2,
                projectRole: DomainFactory.createProjectRole(accessToFiles: false),
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(user, project1)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _)
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, file access role in disabled UserProjectRole"() {
        given:
        String unixGroup = "unix_group"
        User user = DomainFactory.createUser()
        Project project1 = DomainFactory.createProject(unixGroup: unixGroup)
        Project project2 = DomainFactory.createProject(unixGroup: unixGroup)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [unixGroup]
        }
        ProjectRole fileAccessRole = DomainFactory.createProjectRole(accessToFiles: true)
        DomainFactory.createUserProjectRole(user: user, project: project1, projectRole: fileAccessRole)
        DomainFactory.createUserProjectRole(user: user, project: project2, projectRole: fileAccessRole, enabled: false)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(user, project1)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _)
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, removed user has file access role in project with same unix group"() {
        given:
        String unixGroup = "unix_group"
        User user = DomainFactory.createUser()
        Project project1 = DomainFactory.createProject(unixGroup: unixGroup)
        Project project2 = DomainFactory.createProject(unixGroup: unixGroup)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [unixGroup]
        }
        ProjectRole fileAccessRole = DomainFactory.createProjectRole(accessToFiles: true)
        DomainFactory.createUserProjectRole(user: user, project: project1, projectRole: fileAccessRole)
        DomainFactory.createUserProjectRole(user: user, project: project2, projectRole: fileAccessRole)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(user, project1)
        }

        then:
        0 * userProjectRoleService.mailHelperService.sendEmail(_, _, _)
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, only notify when user is in unix group"() {
        given:
        User user = DomainFactory.createUser()
        Project project = DomainFactory.createProject(unixGroup: "unix_group")
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> ldapResult
        }
        ProjectRole fileAccessRole = DomainFactory.createProjectRole(accessToFiles: true)
        DomainFactory.createUserProjectRole(user: user, project: project, projectRole: fileAccessRole)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(user, project)
        }

        then:
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _)

        where:
        ldapResult           | invocations
        ["unix_group"]       | 1
        ["some_other_group"] | 0
    }
}
