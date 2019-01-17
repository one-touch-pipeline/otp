package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import org.codehaus.groovy.grails.context.support.*
import spock.lang.*

class UserProjectRoleServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {
    final static String UNIX_GROUP = "UNIX_GROUP"
    final static String OTHER_GROUP = "OTHER_GROUP"
    final static String SEARCH = "search"
    final static ProjectRole PI_PROJECT_ROLE = DomainFactory.createProjectRole(name: "PI")

    UserProjectRoleService userProjectRoleService
    String email = HelperUtils.randomEmail

    def setup() {
        SpringSecurityService springSecurityService = new SpringSecurityService()
        userProjectRoleService = new UserProjectRoleService()
        userProjectRoleService.messageSource = getMessageSource()
        userProjectRoleService.springSecurityService = springSecurityService
        userProjectRoleService.auditLogService = new AuditLogService()
        userProjectRoleService.auditLogService.springSecurityService = springSecurityService
        createUserAndRoles()
        userProjectRoleService.mailHelperService = Mock(MailHelperService)
        userProjectRoleService.processingOptionService = new ProcessingOptionService()
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION,
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

    @Unroll
    void "notifyAdministration sends email with correct content (action=#operatorAction)"() {
        given:
        Project project = DomainFactory.createProject()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(project: project)
        UserProjectRole requesterUserProjectRole = DomainFactory.createUserProjectRole(project: project)
        String formattedAction = operatorAction.toString().toLowerCase()

        String affectedUserUserDetail = "${userProjectRole.user.realName}\n${userProjectRole.user.username}\n${userProjectRole.user.email}\n${userProjectRole.projectRole.name}"
        String requesterUserDetail = "${requesterUserProjectRole.user.realName}\n${requesterUserProjectRole.user.username}\n${requesterUserProjectRole.user.email}\n${requesterUserProjectRole.projectRole.name}"

        when:
        SpringSecurityUtils.doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.notifyAdministration(userProjectRole, operatorAction)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                "${requesterUserProjectRole.user.username}\n${formattedAction}\n${userProjectRole.user.username}\n${conjunction}\n${userProjectRole.project.name}",
                "${userProjectRole.project.name}\n${userProjectRole.project.unixGroup}\n${operatorAction}\n${affectedUserUserDetail}\n${requesterUserDetail}",
                email,
                requesterUserProjectRole.user.email
        )

        where:
        conjunction | operatorAction
        "to"        | UserProjectRoleService.OperatorAction.ADD
        "from"      | UserProjectRoleService.OperatorAction.REMOVE
    }

    @Unroll
    void "notifyAdministration sends email with unswitched user as requester (action=#operatorAction)"() {
        given:
        User executingUser = User.findByUsername(ADMIN)
        User switchedUser = User.findByUsername(OPERATOR)

        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole()
        String formattedAction = operatorAction.toString().toLowerCase()

        String affectedUserUserDetail = "${userProjectRole.user.realName}\n${userProjectRole.user.username}\n${userProjectRole.user.email}\n${userProjectRole.projectRole.name}"
        String requesterUserDetail = "${switchedUser.realName}\n${switchedUser.username} (switched from ${executingUser.username})\n${switchedUser.email}\nNon-Project-User"

        when:
        SpringSecurityUtils.doWithAuth(executingUser.username) {
            doAsSwitchedToUser(switchedUser.username) {
                userProjectRoleService.notifyAdministration(userProjectRole, operatorAction)
            }
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                "${switchedUser.username}\n${formattedAction}\n${userProjectRole.user.username}\n${conjunction}\n${userProjectRole.project.name}",
                "${userProjectRole.project.name}\n${userProjectRole.project.unixGroup}\n${operatorAction}\n${affectedUserUserDetail}\n${requesterUserDetail}",
                email,
                switchedUser.email
        )

        where:
        conjunction | operatorAction
        "to"        | UserProjectRoleService.OperatorAction.ADD
        "from"      | UserProjectRoleService.OperatorAction.REMOVE
    }

    void "updateEnabledStatus, send mails when activating a user with file access"() {
        given:
        Project project = createProject(unixGroup: UNIX_GROUP)
        UserProjectRole requesterUserProjectRole = DomainFactory.createUserProjectRole(
                project: project,
                projectRole: PI_PROJECT_ROLE,
                manageUsers: true
        )
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: project,
                accessToFiles: accessToFiles,
        )

        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> ldapResult
        }

        when:
        SpringSecurityUtils.doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.updateEnabledStatus(userProjectRole, valueToUpdate)
        }

        then:
        userProjectRole.enabled == valueToUpdate
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _ as String, _)
        (valueToUpdate ? 1 : 0) * userProjectRoleService.mailHelperService.sendEmail(_, _, _ as List<String>)

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
        Project project = createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn: "unknownUser",
                realName: "Unknown User",
                mail: "unknownUser@dummy.com",
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
        Project project = createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        DomainFactory.createUserProjectRole(
                user: user,
                project: project
        )
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn: user.username,
                realName: user.realName,
                mail: user.email,
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
        Project project = createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn: user.username,
                realName: user.realName,
                mail: user.email,
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
        Project project = createProject()
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
                cn: user.username,
                realName: user.realName,
                mail: user.email,
        )
        Project project = createProject(unixGroup: UNIX_GROUP)
        UserProjectRole requesterUserProjectRole = DomainFactory.createUserProjectRole(
                project: project,
                projectRole: PI_PROJECT_ROLE,
                manageUsers: true
        )
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
            getGroupsOfUserByUsername(_) >> []
        }

        when:
        SpringSecurityUtils.doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(
                    project,
                    DomainFactory.createProjectRole(),
                    SEARCH,
                    [accessToFiles: accessToFiles]
            )
        }

        then:
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _ as String, _)
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _ as List<String>)

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
        Project project = createProject()
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
        Project project = createProject()
        User user = DomainFactory.createUser(
                realName: "realName",
                email: email,
        )
        DomainFactory.createUserProjectRole(
                user: user,
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
        Project project = createProject()

        expect:
        UserProjectRole.findByUserAndProjectAndProjectRole(user, project, projectRole) == null

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, user.realName, user.email, projectRole)
        }

        then:
        UserProjectRole.findByUserAndProjectAndProjectRole(user, project, projectRole)
    }

    void "addExternalUserToProject, sends a mail when a user is added"() {
        given:
        ProjectRole projectRole = DomainFactory.createProjectRole()
        User user = DomainFactory.createUser()
        Project project = createProject()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, user.realName, user.email, projectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _ as List<String>)
    }

    void "notifyProjectAuthoritiesAndUser, uses the mails of each project authority and the affected user"() {
        given:
        int numberOfRoles = 5
        int disabledRoles = 2
        ProjectRole pi = PI_PROJECT_ROLE
        Project project = createProject()

        User userToAdd = DomainFactory.createUser()
        List<String> authorityMails = (1..numberOfRoles).findResults {
            UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                    user: DomainFactory.createUser(),
                    project: project,
                    projectRole: pi
            )
            if (it < numberOfRoles - disabledRoles) {
                userProjectRole.enabled = false
                return null
            }
            return userProjectRole.user.email
        }

        List<String> allMails = authorityMails + userToAdd.email

        assert numberOfRoles == UserProjectRole.findAllByProjectAndProjectRole(project, pi).size()
        assert authorityMails.size() == numberOfRoles - disabledRoles

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(project, userToAdd)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, allMails)
    }

    void "notifyProjectAuthoritiesAndUser, is unaffected by receivesNotification flag"() {
        given:
        Project project = createProject()

        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: DomainFactory.createUser(),
                project: project,
                projectRole: PI_PROJECT_ROLE,
                receivesNotifications: false,
        )
        User user = DomainFactory.createUser()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(project, user)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, [userProjectRole.user.email, user.email])
    }

    void "notifyProjectAuthoritiesAndUser sends email with correct content"() {
        given:
        User user = DomainFactory.createUser()
        Project project = createProject()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(project, user)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                "${project.name}",
                "${project.name}\n${user.realName}",
                _ as List<String>
        )
    }

    void "requestToAddUserToUnixGroupIfRequired, only send notification when user is not already in group"() {
        given:
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> unixGroupsOfUser
        }
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP)
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToAddUserToUnixGroupIfRequired(userProjectRole)
        }

        then:
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _, _)

        where:
        invocations | unixGroupsOfUser
        0           | [UNIX_GROUP]
        0           | [UNIX_GROUP, OTHER_GROUP]
        1           | [OTHER_GROUP]
        1           | []
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, user is not in projects with shared unix groups or no others exist"() {
        given:
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [unixGroup1]
        }
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: createProject(unixGroup: unixGroup1)
        )
        createProject(unixGroup: unixGroup2)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(userProjectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _, _)

        where:
        unixGroup1 | unixGroup2
        "shared"   | "shared"
        "shared"   | "not_shared"
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, removed user has no remaining file access role in projects with shared unix group"() {
        given:
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [UNIX_GROUP]
        }
        User user = DomainFactory.createUser()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: user,
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: true,
        )
        DomainFactory.createUserProjectRole(
                user: user,
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: false,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(userProjectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _, _)
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, file access role in disabled UserProjectRole"() {
        given:
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [UNIX_GROUP]
        }
        User user = DomainFactory.createUser()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: user,
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: true
        )
        DomainFactory.createUserProjectRole(
                user: user,
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: true,
                enabled: false
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(userProjectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, _, _)
    }

    @Unroll
    void "test #flag toggle function"() {
        given:
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> []
        }
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
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
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> [UNIX_GROUP]
        }
        User user = DomainFactory.createUser()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                user: user,
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: true
        )
        DomainFactory.createUserProjectRole(
                user: user,
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: true
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(userProjectRole)
        }

        then:
        0 * userProjectRoleService.mailHelperService.sendEmail(_, _, _)
    }

    void "requestToRemoveUserFromUnixGroupIfRequired, only notify when user is in unix group"() {
        given:
        userProjectRoleService.ldapService = Mock(LdapService) {
            getGroupsOfUserByUsername(_) >> ldapResult
        }
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: true
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.requestToRemoveUserFromUnixGroupIfRequired(userProjectRole)
        }

        then:
        invocations * userProjectRoleService.mailHelperService.sendEmail(_, _, _, _)

        where:
        ldapResult    | invocations
        [UNIX_GROUP]  | 1
        [OTHER_GROUP] | 0
    }

    void "getEmailsOfToBeNotifiedProjectUsers, only return emails of users that receive notification and are enabled"() {
        given:
        Project project = createProject()
        List<String> expectedEmails = []
        int numberOfUsers = 12
        numberOfUsers.times { int i ->
            boolean notification = (i <= numberOfUsers / 2)
            boolean enabled = (i % 2 == 0)
            UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                    user: DomainFactory.createUser(),
                    project: project,
                    receivesNotifications: notification,
                    enabled: enabled,
            )
            if (notification && enabled) {
                expectedEmails << userProjectRole.user.email
            }
        }

        when:
        List<String> emails = userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(project)

        then:
        emails.sort() == expectedEmails.sort()
    }

    void "getEmailsOfToBeNotifiedProjectUsers, do not return email of disabled user"() {
        given:
        Project project = createProject()
        DomainFactory.createUserProjectRole(
                user: DomainFactory.createUser([
                        enabled: false
                ]),
                project: project,
                receivesNotifications: true,
                enabled: true,
        )

        when:
        List<String> emails = userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(project)

        then:
        emails.empty
    }


    PluginAwareResourceBundleMessageSource getMessageSource() {
        return Mock(PluginAwareResourceBundleMessageSource) {
            _ * getMessageInternal("projectUser.notification.addToUnixGroup.subject", [], _) >>
                    '''${requester}\n${action}\n${username}\n${conjunction}\n${projectName}'''

            _ * getMessageInternal("projectUser.notification.addToUnixGroup.body", [], _) >>
                    '''${projectName}\n${projectUnixGroup}\n${requestedAction}\n${affectedUserUserDetail}\n${requesterUserDetail}'''

            _ * getMessageInternal("projectUser.notification.addToUnixGroup.userDetail", [], _) >>
                    '''${realName}\n${username}\n${email}\n${role}'''

            _ * getMessageInternal("projectUser.notification.newProjectMember.subject", [], _) >>
                    '''${projectName}'''

            _ * getMessageInternal("projectUser.notification.newProjectMember.body", [], _) >>
                    '''${projectName}\n${userIdentifier}'''
        }
    }
}
