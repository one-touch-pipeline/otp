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
    final static String UNIX_GROUP = "UNIX_GROUP"
    final static String OTHER_GROUP = "OTHER_GROUP"
    final static String SEARCH = "search"

    UserProjectRoleService userProjectRoleService
    String email = HelperUtils.randomEmail

    def setup() {
        userProjectRoleService = new UserProjectRoleService()
        userProjectRoleService.auditLogService = new AuditLogService()
        userProjectRoleService.auditLogService.springSecurityService = new SpringSecurityService()
        createUserAndRoles()
        userProjectRoleService.mailHelperService = Mock(MailHelperService)
        userProjectRoleService.processingOptionService = new ProcessingOptionService()
        DomainFactory.createProcessingOptionLazy(
                name: EMAIL_LINUX_GROUP_ADMINISTRATION,
                type: null,
                project: null,
                value: email,
        )
    }

    void "test updateProjectRole on valid input"() {
        given:
        ProjectRole newRole = DomainFactory.createProjectRole()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.updateProjectRole(userProjectRole, newRole)
        }

        then:
        userProjectRole.projectRole == newRole
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
                "adtool group${formattedAction}user ${project.unixGroup} ${user.username}",
                email
        )

        where:
        conjunction | adtoolaction
        "to"        | UserProjectRoleService.AdtoolAction.ADD
        "from"      | UserProjectRoleService.AdtoolAction.REMOVE
    }

    void "updateEnabledStatus, send mails when activating a user with file access"() {
        given:
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: DomainFactory.createUser(),
                project: DomainFactory.createProject(unixGroup: UNIX_GROUP),
                accessToFiles: accessToFiles,
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
        ldapResult    | accessToFiles | valueToUpdate | invocations
        [OTHER_GROUP] | true          | false         | 0
        [OTHER_GROUP] | true          | true          | 1
        [OTHER_GROUP] | false         | false         | 0
        [OTHER_GROUP] | false         | true          | 0
        [UNIX_GROUP]  | true          | false         | 1
        [UNIX_GROUP]  | true          | true          | 0
        [UNIX_GROUP]  | false         | false         | 0
        [UNIX_GROUP]  | false         | true          | 0
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, create User if non is found for username or email"() {
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
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, SEARCH, [:])
        }

        then:
        User.findByUsernameAndEmail(ldapUserDetails.cn, ldapUserDetails.mail)
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, throw exception when user is already connected to project"() {
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
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, SEARCH, [:])
        }

        then:
        def e = thrown(AssertionError)
        e.message.contains('is already part of project')
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, create UserProjectRole for new User"() {
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
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, SEARCH, [:])
        }

        then:
        UserProjectRole.findByUserAndProjectAndProjectRole(user, project, projectRole)
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, unsuccessful ldap search throws exception"() {
        given:
        Project project = DomainFactory.createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> null
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole, SEARCH, [:])
        }

        then:
        def e = thrown(AssertionError)
        e.message.contains('can not be resolved to a user via LDAP')
    }

    @Unroll
    void "addUserToProjectAndNotifyGroupManagementAuthority, send mail only for users with access to files (accessToFiles = #accessToFiles)"() {
        given:
        User user = DomainFactory.createUser()
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
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(
                    DomainFactory.createProject(unixGroup: UNIX_GROUP),
                    DomainFactory.createProjectRole(),
                    SEARCH,
                    [accessToFiles: accessToFiles]
            )
        }

        then:
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _)

        where:
        accessToFiles | invocations
        true          | 1
        false         | 0
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
        Project project = DomainFactory.createProject(unixGroup: UNIX_GROUP)
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
        0           | [UNIX_GROUP]
        0           | [UNIX_GROUP, OTHER_GROUP]
        1           | [OTHER_GROUP]
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
        User user = DomainFactory.createUser()
        Project project1 = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        Project project2 = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [UNIX_GROUP]
        }
        DomainFactory.createUserProjectRole(
                user: user,
                project: project1,
                accessToFiles: true,
        )
        DomainFactory.createUserProjectRole(
                user: user,
                project: project2,
                accessToFiles: false,
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
        User user = DomainFactory.createUser()
        Project project1 = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        Project project2 = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [UNIX_GROUP]
        }
        DomainFactory.createUserProjectRole(user: user, project: project1, accessToFiles: true)
        DomainFactory.createUserProjectRole(user: user, project: project2, accessToFiles: true, enabled: false)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(user, project1)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _)
    }

    @Unroll
    void "test #flag toggle function"() {
        given:
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> []
        }
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: DomainFactory.createProject(unixGroup: UNIX_GROUP),
                user: User.findByUsername(USER),
                (flag): false,
        )

        expect:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService."toggle${flag.capitalize()}"(userProjectRole)."${flag}" == true
        }

        and:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService."toggle${flag.capitalize()}"(userProjectRole)."${flag}" == false
        }

        where:
        flag                     | _
        "accessToOtp"            | _
        "accessToFiles"          | _
        "manageUsers"            | _
        "manageUsersAndDelegate" | _
        "receivesNotifications"  | _
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, removed user has file access role in project with same unix group"() {
        given:
        User user = DomainFactory.createUser()
        Project project1 = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        Project project2 = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [UNIX_GROUP]
        }
        DomainFactory.createUserProjectRole(user: user, project: project1, accessToFiles: true)
        DomainFactory.createUserProjectRole(user: user, project: project2, accessToFiles: true)

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
        Project project = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> ldapResult
        }
        DomainFactory.createUserProjectRole(user: user, project: project, accessToFiles: true)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(user, project)
        }

        then:
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _)

        where:
        ldapResult    | invocations
        [UNIX_GROUP]  | 1
        [OTHER_GROUP] | 0
    }
}
