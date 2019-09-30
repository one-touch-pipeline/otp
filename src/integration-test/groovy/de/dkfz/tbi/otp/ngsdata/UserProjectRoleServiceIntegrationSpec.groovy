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

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.security.authentication.AuthenticationTrustResolver
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

@Rollback
@Integration
class UserProjectRoleServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    private final static String UNIX_GROUP = "UNIX_GROUP"
    private final static String OTHER_GROUP = "OTHER_GROUP"
    private final static String SEARCH = "search"

    private final static String EMAIL_SENDER_SALUTATION = "the supportTeam"
    private final static String CLUSTER_NAME = "OTP Cluster"

    private final static String EMAIL_LINUX_GROUP_ADMINISTRATION = HelperUtils.randomEmail
    private final static String EMAIL_CLUSTER_ADMINISTRATION = HelperUtils.randomEmail

    @SuppressWarnings('JUnitPublicProperty')
    UserProjectRoleService userProjectRoleService

    @SuppressWarnings('JUnitPublicProperty')
    ProcessingOptionService processingOptionService

    void setupData() {
        SpringSecurityService springSecurityService = new SpringSecurityService()
        springSecurityService.authenticationTrustResolver = Mock(AuthenticationTrustResolver) {
            isAnonymous(_) >> false
        }
        createUserAndRoles()
        createAllBasicProjectRoles()

        processingOptionService = new ProcessingOptionService()
        userProjectRoleService = new UserProjectRoleService()
        userProjectRoleService.messageSourceService = getMessageSourceServiceWithMockedMessageSource()
        userProjectRoleService.springSecurityService = springSecurityService
        userProjectRoleService.auditLogService = new AuditLogService()
        userProjectRoleService.auditLogService.springSecurityService = springSecurityService
        userProjectRoleService.mailHelperService = Mock(MailHelperService)
        userProjectRoleService.processingOptionService = new ProcessingOptionService()

        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.EMAIL_LINUX_GROUP_ADMINISTRATION,
                type: null,
                project: null,
                value: EMAIL_LINUX_GROUP_ADMINISTRATION,
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION,
                type: null,
                project: null,
                value: EMAIL_SENDER_SALUTATION,
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.CLUSTER_NAME,
                type: null,
                project: null,
                value: CLUSTER_NAME,
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION,
                type: null,
                project: null,
                value: EMAIL_CLUSTER_ADMINISTRATION,
        )
    }

    @Unroll
    void "createUserProjectRole, asserts that the user is not already connected to the project (username=#username)"() {
        given:
        setupData()

        UserProjectRole upr = DomainFactory.createUserProjectRole(
                user: DomainFactory.createUser(username: username)
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.createUserProjectRole(upr.user, upr.project, upr.projectRole)
        }

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("${upr.user.username ?: upr.user.realName}\' is already part of project")

        where:
        username   | _
        null       | _
        "username" | _
    }

    void "test updateProjectRole on valid input"() {
        given:
        setupData()

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
        setupData()

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
                "addToUnixGroup\n${requesterUserProjectRole.user.username}\n${formattedAction}\n${userProjectRole.user.username}\n${conjunction}\n${userProjectRole.project.unixGroup}",
                "addToUnixGroup\n${userProjectRole.project.unixGroup}\n${userProjectRole.project.name}\n${operatorAction}\n${affectedUserUserDetail}\n${requesterUserDetail}",
                EMAIL_LINUX_GROUP_ADMINISTRATION,
        )

        where:
        conjunction | operatorAction
        "to"        | UserProjectRoleService.OperatorAction.ADD
        "from"      | UserProjectRoleService.OperatorAction.REMOVE
    }

    void "notifyAdministration lists other projects with same unix group as requested project"() {
        given:
        setupData()

        Project usedProject = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        Project otherProject1 = DomainFactory.createProject(unixGroup: UNIX_GROUP)
        Project otherProject2 = DomainFactory.createProject(unixGroup: UNIX_GROUP)

        DomainFactory.createProject(unixGroup: OTHER_GROUP)

        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(project: usedProject)

        String projectList = [otherProject1, otherProject2]*.name.sort().join(", ")

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.notifyAdministration(userProjectRole, UserProjectRoleService.OperatorAction.ADD)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, { it.contains(projectList) }, EMAIL_LINUX_GROUP_ADMINISTRATION)
    }

    @Unroll
    void "notifyAdministration sends email with unswitched user as requester (action=#operatorAction)"() {
        given:
        setupData()

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
                "addToUnixGroup\n${switchedUser.username}\n${formattedAction}\n${userProjectRole.user.username}\n${conjunction}\n${userProjectRole.project.unixGroup}",
                "addToUnixGroup\n${userProjectRole.project.unixGroup}\n${userProjectRole.project.name}\n${operatorAction}\n${affectedUserUserDetail}\n${requesterUserDetail}",
                EMAIL_LINUX_GROUP_ADMINISTRATION,
        )

        where:
        conjunction | operatorAction
        "to"        | UserProjectRoleService.OperatorAction.ADD
        "from"      | UserProjectRoleService.OperatorAction.REMOVE
    }

    @Unroll
    void "toggleEnabled, send mails when activating a user with file access (fileAccess=#fileAccess, newEnabledStatus=#newEnabledStatus)"() {
        given:
        setupData()

        Project project = createProject(unixGroup: UNIX_GROUP)
        UserProjectRole requesterUserProjectRole = DomainFactory.createUserProjectRole(
                project: project,
                projectRole: PI,
                manageUsers: true
        )
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: project,
                accessToFiles: fileAccess,
                enabled: enabledStatus,
        )

        when:
        SpringSecurityUtils.doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.toggleEnabled(userProjectRole)
        }

        then: "new enabled status was set"
        userProjectRole.enabled == !enabledStatus

        and: "notification for unix group administration was sent"
        removeMailCount * userProjectRoleService.mailHelperService.sendEmail(_ as String, { it.contains("REMOVE") }, EMAIL_LINUX_GROUP_ADMINISTRATION)
        addMailCount * userProjectRoleService.mailHelperService.sendEmail(_ as String, { it.contains("ADD") }, EMAIL_LINUX_GROUP_ADMINISTRATION)

        and: "notification for user managers regarding file access was sent"
        notifyFileAccess * userProjectRoleService.mailHelperService.sendEmail({
            it.contains("fileAccessChange")
        }, _ as String, userProjectRole.user.email, _ as List<String>)

        and: "notification for user managers that a user has been enabled was sent"
        notifyEnableUser * userProjectRoleService.mailHelperService.sendEmail({
            it.contains("newProjectMember")
        }, _ as String, _ as List<String>, [userProjectRole.user.email])

        0 * userProjectRoleService.mailHelperService.sendEmail(*_)

        where:
        fileAccess | enabledStatus || removeMailCount | addMailCount | notifyEnableUser | notifyFileAccess
        true       | true          || 1               | 0            | 0                | 0
        true       | false         || 0               | 1            | 1                | 1
        false      | true          || 0               | 0            | 0                | 0
        false      | false         || 0               | 0            | 1                | 0
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, create User if non is found for username or email"() {
        given:
        setupData()

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

    void "addUserToProjectAndNotifyGroupManagementAuthority, create UserProjectRole for new User"() {
        given:
        setupData()

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
        setupData()

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
        AssertionError e = thrown(AssertionError)
        e.message.contains('can not be resolved to a user via LDAP')
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, when there is already an external user with the email address, throws assertion exception"() {
        given:
        setupData()

        User user = DomainFactory.createUser(username: null)
        Project project = createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn: 'username',
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
        AssertionError e = thrown(AssertionError)
        e.message.contains("There is already an external user with email '${user.email}'")
    }

    @Unroll
    void "addUserToProjectAndNotifyGroupManagementAuthority, send mail only for users with access to files (accessToFiles=#accessToFiles)"() {
        given:
        setupData()

        User user = DomainFactory.createUser()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn: user.username,
                realName: user.realName,
                mail: user.email,
        )
        Project project = createProject(unixGroup: UNIX_GROUP)
        UserProjectRole requesterUserProjectRole = DomainFactory.createUserProjectRole(
                project: project,
                projectRole: PI,
                manageUsers: true
        )
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
            getGroupsOfUserByUsername(_) >> []
        }

        int expectedInvocations = accessToFiles ? 1 : 0

        when:
        SpringSecurityUtils.doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(
                    project,
                    DomainFactory.createProjectRole(),
                    SEARCH,
                    [accessToFiles: accessToFiles]
            )
        }

        then: "notification for unix group administration was sent"
        expectedInvocations * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String, _ as String)

        and: "notification for user managers regarding file access was sent"
        expectedInvocations * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String, user.email, _ as List<String>)

        and: "notification for user managers regarding new user was sent"
        1 * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String, _ as List<String>, [user.email])

        where:
        accessToFiles << [true, false]
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, email is already in use by another internal user"() {
        given:
        setupData()

        User user = DomainFactory.createUser()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                cn: "something_else",
                realName: user.realName,
                mail: user.email,
        )

        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(createProject(), DomainFactory.createProjectRole(), SEARCH)
        }

        then:
        AssertionError e = thrown()
        e.message.startsWith("The given email address '${user.email}' is already registered for LDAP user '${user.username}'")
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, synchronizes action for projects with shared unix group"() {
        given:
        setupData()
        Project projectA = createProject()
        Project projectB = createProject(unixGroup: projectA.unixGroup)

        User user = DomainFactory.createUser()
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
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(projectA, DomainFactory.createProjectRole(), SEARCH)
        }

        then:
        UserProjectRole.findAllByUserAndProjectInList(user, [projectA, projectB]).size() == 2
    }

    void "addExternalUserToProject, create User if non is found for realName or email"() {
        given:
        setupData()

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

    void "addExternalUserToProject, create UserProjectRole for new User"() {
        given:
        setupData()

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
        setupData()

        ProjectRole projectRole = DomainFactory.createProjectRole()
        User user = DomainFactory.createUser(username: null)
        Project project = createProject()

        DomainFactory.createUserProjectRole(
                project: project,
                user: User.findByUsername(OPERATOR),
                manageUsers: true,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, user.realName, user.email, projectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String, _ as List<String>, _ as List<String>)
    }

    void "addExternalUserToProject, when user with different attributes already exists, throws error"() {
        given:
        setupData()

        ProjectRole projectRole = DomainFactory.createProjectRole()
        User user = DomainFactory.createUser(username: ldap ? 'right name' : null)
        Project project = createProject()

        DomainFactory.createUserProjectRole(
                project: project,
                user: User.findByUsername(OPERATOR),
                manageUsers: true,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, 'wrong name', user.email, projectRole)
        }

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("The given email address '${user.email}' is already registered for ${ldap ? "LDAP user '${user.username}'" : "external user '${user.realName}'"}")

        where:
        ldap  | _
        true  | _
        false | _
    }

    void "addExternalUserToProject, synchronizes action for projects with shared unix group"() {
        given:
        setupData()
        Project projectA = createProject()
        Project projectB = createProject(unixGroup: projectA.unixGroup)

        User user = DomainFactory.createUser(username: null)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(projectA, user.realName, user.email, DomainFactory.createProjectRole())
        }

        then:
        UserProjectRole.findAllByUserAndProjectInList(user, [projectA, projectB]).size() == 2
    }

    void "notifyProjectAuthoritiesAndUser, sends mail, direct recipients: authorities and user managers, CC: affected user"() {
        given:
        setupData()

        ProjectRole otherRole = DomainFactory.createProjectRole()

        Project project = createProject()
        Closure<UserProjectRole> addUserWithProjectRole = { ProjectRole projectRole, boolean manageUsers, boolean enabled ->
            return DomainFactory.createUserProjectRole(
                    project    : project,
                    projectRole: projectRole,
                    manageUsers: manageUsers,
                    enabled    : enabled,
            )
        }

        UserProjectRole newUserProjectRole = DomainFactory.createUserProjectRole(project: project)

        List<String> recipients = [
                addUserWithProjectRole(PI, true, true),
                addUserWithProjectRole(PI, false, true),
                addUserWithProjectRole(otherRole, true, true),
        ]*.user.email.sort()

        addUserWithProjectRole(otherRole, false, true)
        [PI, otherRole].each {
            addUserWithProjectRole(it, true, false)
            addUserWithProjectRole(it, false, false)
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(newUserProjectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                _ as String,
                _ as String,
                recipients,
                [newUserProjectRole.user.email],
        )
    }

    void "notifyProjectAuthoritiesAndUser, is unaffected by receivesNotification flag"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        UserProjectRole uprToBeNotified = DomainFactory.createUserProjectRole(
                user: DomainFactory.createUser(),
                project: project,
                projectRole: PI,
                receivesNotifications: false,
        )
        UserProjectRole newUPR = DomainFactory.createUserProjectRole(project: project)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(newUPR)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String, [uprToBeNotified.user.email], [newUPR.user.email])
    }

    @Unroll
    void "notifyProjectAuthoritiesAndUser sends email with correct content (authoritative=#authoritative, role=#projectRoleName)"() {
        given:
        setupData()

        ProjectRole projectRole = projectRoleName ? ProjectRole.findByName(projectRoleName) : DomainFactory.createProjectRole()
        UserProjectRole newUPR = DomainFactory.createUserProjectRole(
                projectRole: projectRole,
        )

        Map properties = [
                project    : newUPR.project,
                manageUsers: true,
        ]
        if (authoritative) {
            properties += [
                    user       : User.findByUsername(OPERATOR),
                    projectRole: PI,
            ]
        }
        UserProjectRole executingUPR = DomainFactory.createUserProjectRole(properties)

        String expectedSubject = "newProjectMember\n${newUPR.project.name}"
        String expectedContent = "newProjectMember\n${executingUPR.user.realName}\n${newUPR.user.realName}\n${newUPR.projectRole.name}\n${newUPR.project.name}"
        if (authoritative && projectRole == SUBMITTER) {
            String nameAndSalutation = EMAIL_SENDER_SALUTATION
            expectedContent = "administrativeUserAddedSubmitter\n${newUPR.user.realName}\n${newUPR.projectRole.name}\n${newUPR.project.name}\n${nameAndSalutation}\n${nameAndSalutation}"
        }

        when:
        SpringSecurityUtils.doWithAuth(executingUPR.user.username) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(newUPR)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                expectedSubject,
                expectedContent,
                [executingUPR.user.email],
                [newUPR.user.email],
        )

        where:
        authoritative | projectRoleName
        true          | ProjectRole.Basic.SUBMITTER.name()
        false         | ProjectRole.Basic.SUBMITTER.name()
        true          | null
        false         | null
    }

    void "notifyUsersAboutFileAccessChange, builds correct content"() {
        given:
        setupData()

        User executingUser = User.findByUsername(OPERATOR)

        Project project = DomainFactory.createProject(dirAnalysis: "/dev/null")
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: project,
        )

        List<UserProjectRole> toBeNotified = [
            DomainFactory.createUserProjectRole(project: project, manageUsers: true),
            DomainFactory.createUserProjectRole(project: project, projectRole: PI),
        ]

        String projectName = project.name
        String expectedBody = """\
            fileAccessChange
            ${userProjectRole.user.realName}
            ${executingUser.realName}
            ${projectName}
            ${project.dirAnalysis}
            ${CLUSTER_NAME}
            ${EMAIL_CLUSTER_ADMINISTRATION}
            ${EMAIL_SENDER_SALUTATION}""".stripIndent()

        when:
        SpringSecurityUtils.doWithAuth(executingUser.username) {
            userProjectRoleService.notifyUsersAboutFileAccessChange(userProjectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                'fileAccessChange\n' + projectName,
                expectedBody,
                userProjectRole.user.email,
                toBeNotified*.user.email,
        )
    }

    @Unroll
    void "test #flag toggle function"() {
        given:
        setupData()

        List<UserProjectRole> userProjectRoles = 2.collect {
            DomainFactory.createUserProjectRole(
                    project: createProject(unixGroup: UNIX_GROUP),
                    (flag): false,
            )
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService."toggle${flag.capitalize()}"(userProjectRoles[0])
        }

        then:
        userProjectRoles.every {
            it."${flag}" == true
        }
        fileAccessMail * userProjectRoleService.mailHelperService.sendEmail(_ as String, { it.contains("ADD") }, _ as String)
        fileAccessMail * userProjectRoleService.mailHelperService.sendEmail({ it.contains("fileAccessChange") }, _ as String, _ as String, _ as List<String>)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService."toggle${flag.capitalize()}"(userProjectRoles[0])
        }

        then:
        userProjectRoles.every {
            it."${flag}" == false
        }
        fileAccessMail * userProjectRoleService.mailHelperService.sendEmail(_ as String, { it.contains("REMOVE") }, _ as String)
        _ * userProjectRoleService.mailHelperService.sendEmail(*_)

        where:
        flag                     | fileAccessMail
        "accessToOtp"            | 0
        "accessToFiles"          | 1
        "manageUsers"            | 0
        "manageUsersAndDelegate" | 0
        "receivesNotifications"  | 0
        "enabled"                | 0
    }

    @Unroll
    void "test #flag access as project user"() {
        given:
        setupData()

        User user = DomainFactory.createUser()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
                (flag): false,
        )

        when:
        SpringSecurityUtils.doWithAuth(user.username) {
            userProjectRoleService."toggle${flag.capitalize()}"(userProjectRole)
        }

        then:
        userProjectRole."${flag}" == true

        where:
        flag << ["accessToOtp", "accessToFiles", "manageUsers", "manageUsersAndDelegate", "receivesNotifications", "enabled"]
    }

    void "getEmailsOfToBeNotifiedProjectUsers, only return emails of users that receive notification and are enabled"() {
        given:
        setupData()

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
        setupData()

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

    @Unroll
    void "test getEmailsForNotification with receivesNotifications #receivesNotifications roleEnabled #roleEnabled userEnabled #userEnabled result #result"() {
        given:
        setupData()

        String output
        Project project = createProject()
        DomainFactory.createUserProjectRole(
                user: DomainFactory.createUser([
                        enabled: userEnabled,
                        email: EMAIL_LINUX_GROUP_ADMINISTRATION
                ]),
                project: project,
                receivesNotifications: receivesNotifications,
                enabled: roleEnabled,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            output = userProjectRoleService.getEmailsForNotification(project)
        }

        then:
        output == result

        where:
        receivesNotifications | roleEnabled | userEnabled || result
        true                  | true        | true        || EMAIL_LINUX_GROUP_ADMINISTRATION
        true                  | true        | false       || ''
        true                  | false       | true        || ''
        true                  | false       | false       || ''
        false                 | true        | true        || ''
        false                 | true        | false       || ''
        false                 | false       | true        || ''
        false                 | false       | false       || ''
    }

    @Unroll
    void "test getNumberOfValidUsersForProjects for given date"() {
        given:
        setupData()

        Date baseDate = new Date(0, 0, 10)
        Date startDate = startDateOffset  == null ? null : baseDate - startDateOffset
        Date endDate = endDateOffset == null ? null : baseDate - endDateOffset

        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole()
        userProjectRole.user.dateCreated = baseDate - 1
        userProjectRole.user.save(flush: true)

        when:
        int users = userProjectRoleService.getNumberOfValidUsersForProjects([userProjectRole.project], startDate, endDate)

        then:
        users == expectedUsers

        where:
        startDateOffset | endDateOffset || expectedUsers
        2               | 0             || 1
        8               | 2             || 0
        null            | null          || 1
    }

    void "getUserManagers, returns all enabled users of project with manageUsers regardless of ProjectRole or other flags"() {
        given:
        setupData()

        // valid UserProjectRole of different Project
        DomainFactory.createUserProjectRole(manageUsers: true)

        Project project = createProject()
        Closure<UserProjectRole> addUserProjectRole = { Map properties = [:] ->
            return DomainFactory.createUserProjectRole([
                    project              : project,
                    accessToOtp          : false,
                    receivesNotifications: false,
            ] + properties)
        }

        List<User> expectedUsers = []

        [BIOINFORMATICIAN, PI, SUBMITTER].each { ProjectRole projectRole ->
            expectedUsers << addUserProjectRole(projectRole: projectRole, manageUsers: true).user
            addUserProjectRole(projectRole: projectRole, manageUsers: false)
        }

        ["accessToOtp", "accessToFiles", "manageUsersAndDelegate", "receivesNotifications"].each { String flag ->
            [true, false].each { boolean flagStatus ->
                expectedUsers << addUserProjectRole((flag): flagStatus, manageUsers: true).user
                addUserProjectRole((flag): flagStatus, manageUsers: false)
            }
        }

        when:
        List<User> userManagers = userProjectRoleService.getUserManagers(project)

        then:
        userManagers == expectedUsers
    }

    void "getProjectAuthorities, returns all enabled users of project with an authoritative ProjectRole"() {
        given:
        setupData()

        // valid UserProjectRole of different Project
        DomainFactory.createUserProjectRole(projectRole: ProjectRole.findByName(ProjectRole.AUTHORITY_PROJECT_ROLES.first()))

        Project project = createProject()
        Closure<UserProjectRole> addUserProjectRole = { Map properties = [:] ->
            return DomainFactory.createUserProjectRole([
                    project: project,
            ] + properties)
        }

        List<User> expectedUsers = []

        // authoritative
        [PI].each { ProjectRole projectRole ->
            [true, false].each { boolean enabled ->
                expectedUsers << addUserProjectRole(projectRole: projectRole, enabled: true).user
                addUserProjectRole(projectRole: projectRole, enabled: false)
            }
        }

        // not authoritative
        [BIOINFORMATICIAN, SUBMITTER, DomainFactory.createProjectRole()].each { ProjectRole projectRole ->
            [true, false].each { boolean enabled ->
                addUserProjectRole(projectRole: projectRole, enabled: enabled)
            }
        }

        when:
        List<User> projectAuthorities = userProjectRoleService.getProjectAuthorities(project)

        then:
        projectAuthorities == expectedUsers
    }

    MessageSourceService getMessageSourceServiceWithMockedMessageSource() {
        return new MessageSourceService(
                messageSource: getMessageSource()
        )
    }

    @SuppressWarnings('GStringExpressionWithinString')
    PluginAwareResourceBundleMessageSource getMessageSource() {
        return Mock(PluginAwareResourceBundleMessageSource) {
            _ * getMessageInternal("projectUser.notification.addToUnixGroup.subject", [], _) >>
                    '''addToUnixGroup\n${requester}\n${action}\n${username}\n${conjunction}\n${unixGroup}'''

            _ * getMessageInternal("projectUser.notification.addToUnixGroup.body", [], _) >>
                    '''addToUnixGroup\n${projectUnixGroup}\n${projectList}\n${requestedAction}\n${affectedUserUserDetail}\n${requesterUserDetail}'''

            _ * getMessageInternal("projectUser.notification.addToUnixGroup.userDetail", [], _) >>
                    '''${realName}\n${username}\n${email}\n${role}'''

            _ * getMessageInternal("projectUser.notification.newProjectMember.subject", [], _) >>
                    '''newProjectMember\n${projectName}'''

            _ * getMessageInternal("projectUser.notification.newProjectMember.body.userManagerAddedMember", [], _) >>
                    '''newProjectMember\n${executingUser}\n${userIdentifier}\n${projectRole}\n${projectName}'''

            _ * getMessageInternal("projectUser.notification.newProjectMember.body.administrativeUserAddedSubmitter", [], _) >>
                    '''administrativeUserAddedSubmitter\n${userIdentifier}\n${projectRole}\n${projectName}\n${supportTeamName}\n${supportTeamSalutation}'''

            _ * getMessageInternal("projectUser.notification.fileAccessChange.subject", [], _) >>
                    '''fileAccessChange\n${projectName}'''

            _ * getMessageInternal("projectUser.notification.fileAccessChange.body", [], _) >>
                    '''fileAccessChange\n${username}\n${requester}\n${projectName}\n${dirAnalysis}\n${clusterName}\n${clusterAdministrationEmail}\n${supportTeamSalutation}'''
        }
    }
}
