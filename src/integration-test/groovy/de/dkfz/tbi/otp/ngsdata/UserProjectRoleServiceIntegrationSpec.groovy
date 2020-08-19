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

import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationTrustResolver
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*

@Rollback
@Integration
class UserProjectRoleServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    private final static String UNIX_GROUP = "UNIX_GROUP"
    private final static String OTHER_GROUP = "OTHER_GROUP"
    private final static String SEARCH = "search"

    private final static String EMAIL_SENDER_SALUTATION = "the supportTeam"
    private final static String CLUSTER_NAME = "OTP Cluster"

    private final static String AD_GROUP_TOOL_PATH = "path/to/script.sh"

    private final static String EMAIL_LINUX_GROUP_ADMINISTRATION = HelperUtils.randomEmail
    private final static String EMAIL_CLUSTER_ADMINISTRATION = HelperUtils.randomEmail

    @Autowired
    GrailsApplication grailsApplication

    UserProjectRoleService userProjectRoleService

    @Rule
    public TemporaryFolder temporaryFolder

    void setupData() {
        createUserAndRoles()
        createAllBasicProjectRoles()

        SpringSecurityService springSecurityService = new SpringSecurityService(
                grailsApplication          : grailsApplication,
                authenticationTrustResolver: Mock(AuthenticationTrustResolver) {
                    isAnonymous(_) >> false
                }
        )

        userProjectRoleService = new UserProjectRoleService()
        userProjectRoleService.messageSourceService = messageSourceServiceWithMockedMessageSource
        userProjectRoleService.springSecurityService = springSecurityService
        userProjectRoleService.auditLogService = new AuditLogService()
        userProjectRoleService.auditLogService.securityService = new SecurityService()
        userProjectRoleService.auditLogService.securityService.springSecurityService = springSecurityService
        userProjectRoleService.mailHelperService = Mock(MailHelperService)
        userProjectRoleService.processingOptionService = new ProcessingOptionService()
        userProjectRoleService.configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])

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

    /**
     * Sets up the ProcessingOption containing the templates for all OperatorActions with basic valid templates
     */
    @SuppressWarnings("GStringExpressionWithinString")
    void setupAdGroupToolSnippetProcessingOptions() {
        String prefix = AD_GROUP_TOOL_PATH
        String template = "\${unixGroup} \${username}"
        Arrays.asList(UserProjectRoleService.OperatorAction.values()).each { UserProjectRoleService.OperatorAction action ->
            DomainFactory.createProcessingOptionLazy(
                    name : action.commandTemplateOptionName,
                    value: "${prefix} ${action.name()} ${template}",
            )
        }
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
            userProjectRoleService.createUserProjectRole(upr.user, upr.project, upr.projectRoles)
        }

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("${upr.user.username ?: upr.user.realName}\' is already part of project")

        where:
        username   | _
        null       | _
        "username" | _
    }

    @Unroll
    void "notifyAdministration sends email with correct content (action=#operatorAction)"() {
        given:
        setupData()
        setupAdGroupToolSnippetProcessingOptions()

        Project project = createProject()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(project: project)
        UserProjectRole requesterUserProjectRole = DomainFactory.createUserProjectRole(project: project)
        String formattedAction = operatorAction.toString().toLowerCase()

        String scriptCommand = "${AD_GROUP_TOOL_PATH} ${operatorAction.name()} ${userProjectRole.project.unixGroup} ${userProjectRole.user.username}"

        String affectedUserUserDetail = """\
            |${userProjectRole.user.realName}
            |${userProjectRole.user.username}
            |${userProjectRole.user.email}
            |${userProjectRole.projectRoles*.name.join(",")}""".stripMargin()
        String requesterUserDetail = """\
            |${requesterUserProjectRole.user.realName}
            |${requesterUserProjectRole.user.username}
            |${requesterUserProjectRole.user.email}
            |${requesterUserProjectRole.projectRoles*.name.join(",")}""".stripMargin()

        when:
        SpringSecurityUtils.doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.notifyAdministration(userProjectRole, operatorAction)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                """addToUnixGroup
                |${requesterUserProjectRole.user.username}
                |${formattedAction}
                |${userProjectRole.user.username}
                |${conjunction}
                |${userProjectRole.project.unixGroup}""".stripMargin(),
                """addToUnixGroup
                |${userProjectRole.project.unixGroup}
                |${userProjectRole.project.name}
                |${operatorAction}
                |${scriptCommand}
                |${affectedUserUserDetail}
                |${requesterUserDetail}""".stripMargin(),
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

        createProject(unixGroup: OTHER_GROUP)

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
        setupAdGroupToolSnippetProcessingOptions()

        User executingUser = CollectionUtils.exactlyOneElement(User.findAllByUsername(ADMIN))
        User switchedUser = CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR))

        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole()
        String formattedAction = operatorAction.toString().toLowerCase()

        String scriptCommand = "${AD_GROUP_TOOL_PATH} ${operatorAction.name()} ${userProjectRole.project.unixGroup} ${userProjectRole.user.username}"

        String affectedUserUserDetail = """\
            |${userProjectRole.user.realName}
            |${userProjectRole.user.username}
            |${userProjectRole.user.email}
            |${userProjectRole.projectRoles*.name.join(",")}""".stripMargin()
        String requesterUserDetail = """\
            |${switchedUser.realName}
            |${switchedUser.username} (switched from ${executingUser.username})
            |${switchedUser.email}
            |Non-Project-User""".stripMargin()

        when:
        SpringSecurityUtils.doWithAuth(executingUser.username) {
            doAsSwitchedToUser(switchedUser.username) {
                userProjectRoleService.notifyAdministration(userProjectRole, operatorAction)
            }
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                """addToUnixGroup
                |${switchedUser.username}
                |${formattedAction}
                |${userProjectRole.user.username}
                |${conjunction}
                |${userProjectRole.project.unixGroup}""".stripMargin(),
                """addToUnixGroup
                |${userProjectRole.project.unixGroup}
                |${userProjectRole.project.name}
                |${operatorAction}
                |${scriptCommand}
                |${affectedUserUserDetail}
                |${requesterUserDetail}""".stripMargin(),
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
                projectRoles: [pi],
                manageUsers: true
        )
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: project,
                accessToFiles: fileAccess,
                enabled: enabledStatus,
        )

        when:
        SpringSecurityUtils.doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.setEnabled(userProjectRole, !enabledStatus)
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
                username: "unknownUser",
                realName: "Unknown User",
                mail    : "unknownUser@dummy.com",
        )
        userProjectRoleService.userService = new UserService()
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        expect:
        User.findByUsernameAndEmail(ldapUserDetails.username, ldapUserDetails.mail) == null

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole as Set<ProjectRole>, SEARCH, [:])
        }

        then:
        User.findAllByUsernameAndEmail(ldapUserDetails.username, ldapUserDetails.mail)
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, create UserProjectRole for new User"() {
        given:
        setupData()

        User user = DomainFactory.createUser()
        Project project = createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                username: user.username,
                realName: user.realName,
                mail    : user.email,
        )
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        expect:
        UserProjectRole.withCriteria {
            eq("user", user)
            eq("project", project)
            projectRoles {
                'in'("name", projectRole.name)
            }
        } == []

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole as Set<ProjectRole>, SEARCH, [:])
        }

        then:
        UserProjectRole.withCriteria {
            eq("user", user)
            eq("project", project)
            projectRoles {
                'in'("name", projectRole.name)
            }
        }
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
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole as Set<ProjectRole>, SEARCH, [:])
        }

        then:
        LdapUserCreationException e = thrown(LdapUserCreationException)
        e.message.contains('can not be resolved to a user via LDAP')
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, when there is already an external user with the email address, throws assertion exception"() {
        given:
        setupData()

        User user = DomainFactory.createUser(username: null)
        Project project = createProject()
        ProjectRole projectRole = DomainFactory.createProjectRole()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                username: 'username',
                realName: user.realName,
                mail    : user.email,
        )
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole as Set<ProjectRole>, SEARCH, [:])
        }

        then:
        LdapUserCreationException e = thrown(LdapUserCreationException)
        e.message.contains("There is already an external user with email '${user.email}'")
    }

    @Unroll
    void "addUserToProjectAndNotifyGroupManagementAuthority, send mail only for users with access to files (accessToFiles=#accessToFiles)"() {
        given:
        setupData()

        User user = DomainFactory.createUser()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                username: user.username,
                realName: user.realName,
                mail    : user.email,
        )
        Project project = createProject(unixGroup: UNIX_GROUP)
        UserProjectRole requesterUserProjectRole = DomainFactory.createUserProjectRole(
                project     : project,
                projectRoles: [pi],
                manageUsers : true
        )
        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
            getGroupsOfUser(_) >> []
        }

        int expectedInvocations = accessToFiles ? 1 : 0

        when:
        SpringSecurityUtils.doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(
                    project,
                    DomainFactory.createProjectRole() as Set<ProjectRole>,
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

        and:
        UserProjectRole.findAllByIdNotEqual(requesterUserProjectRole.id)*.fileAccessChangeRequested == [accessToFiles]

        where:
        accessToFiles << [true, false]
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, email is already in use by another internal user"() {
        given:
        setupData()

        User user = DomainFactory.createUser()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                username: "something_else",
                realName: user.realName,
                mail    : user.email,
        )

        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(createProject(), DomainFactory.createProjectRole() as Set<ProjectRole>, SEARCH)
        }

        then:
        LdapUserCreationException e = thrown(LdapUserCreationException)
        e.message.startsWith("The given email address '${user.email}' is already registered for LDAP user '${user.username}'")
        UserProjectRole.count() == 0
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, synchronizes action for projects with shared unix group"() {
        given:
        setupData()
        Project projectA = createProject()
        Project projectB = createProject(unixGroup: projectA.unixGroup)

        User user = DomainFactory.createUser()
        LdapUserDetails ldapUserDetails = new LdapUserDetails(
                username: user.username,
                realName: user.realName,
                mail    : user.email,
        )

        userProjectRoleService.ldapService = Mock(LdapService) {
            getLdapUserDetailsByUsername(_) >> ldapUserDetails
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(projectA, DomainFactory.createProjectRole() as Set<ProjectRole>, SEARCH)
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
            userProjectRoleService.addExternalUserToProject(project, realName, email, projectRole as Set<ProjectRole>)
        }

        then:
        User.findAllByEmail(email)
    }

    void "addExternalUserToProject, create UserProjectRole for new User"() {
        given:
        setupData()

        ProjectRole projectRole = DomainFactory.createProjectRole()
        User user = DomainFactory.createUser(username: null)
        Project project = createProject()

        expect:

        UserProjectRole.withCriteria {
            eq("user", user)
            eq("project", project)
            projectRoles {
                'in'("name", projectRole.name)
            }
        } == []

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, user.realName, user.email, projectRole as Set<ProjectRole>)
        }

        then:
        UserProjectRole.withCriteria {
            eq("user", user)
            eq("project", project)
            projectRoles {
                'in'("name", projectRole.name)
            }
        }
    }

    void "addExternalUserToProject, sends a mail when a user is added"() {
        given:
        setupData()

        ProjectRole projectRole = DomainFactory.createProjectRole()
        User user = DomainFactory.createUser(username: null)
        Project project = createProject()

        DomainFactory.createUserProjectRole(
                project    : project,
                user       : CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR)),
                manageUsers: true,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, user.realName, user.email, projectRole as Set<ProjectRole>)
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
                project    : project,
                user       : CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR)),
                manageUsers: true,
        )

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, 'wrong name', user.email, projectRole as Set<ProjectRole>)
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
            userProjectRoleService.addExternalUserToProject(projectA, user.realName, user.email, DomainFactory.createProjectRole() as Set<ProjectRole>)
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
                    project     : project,
                    projectRoles: [projectRole],
                    manageUsers : manageUsers,
                    enabled     : enabled,
            )
        }

        UserProjectRole newUserProjectRole = DomainFactory.createUserProjectRole(project: project)

        List<String> recipients = [
                addUserWithProjectRole(pi, true, true),
                addUserWithProjectRole(pi, false, true),
                addUserWithProjectRole(otherRole, true, true),
        ]*.user.email.sort()

        addUserWithProjectRole(otherRole, false, true)
        [pi, otherRole].each {
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

        Project project = createProject()
        UserProjectRole uprToBeNotified = DomainFactory.createUserProjectRole(
                user                 : DomainFactory.createUser(),
                project              : project,
                projectRoles         : [pi],
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

        ProjectRole projectRole = projectRoleName ? CollectionUtils.exactlyOneElement(ProjectRole.findAllByName(projectRoleName)) : DomainFactory.createProjectRole()
        UserProjectRole newUPR = DomainFactory.createUserProjectRole(
                projectRoles: [projectRole],
        )

        Map properties = [
                project    : newUPR.project,
                manageUsers: true,
        ]
        if (authoritative) {
            properties += [
                    user        : CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR)),
                    projectRoles: [pi],
            ]
        }
        UserProjectRole executingUPR = DomainFactory.createUserProjectRole(properties)

        String expectedSubject = "newProjectMember\n${newUPR.project.name}"
        String expectedContent = "newProjectMember\n${executingUPR.user.realName}\n${newUPR.user.realName}\n${newUPR.projectRoles.name.join(",")}\n${newUPR.project.name}"
        if (authoritative && projectRole == submitter) {
            String nameAndSalutation = EMAIL_SENDER_SALUTATION
            expectedContent = "administrativeUserAddedSubmitter\n${newUPR.user.realName}\n${newUPR.projectRoles.name.join(",")}\n${newUPR.project.name}\n${nameAndSalutation}\n${nameAndSalutation}"
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

        User executingUser = CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR))

        Project project = createProject(dirAnalysis: "/dev/null")
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: project,
        )

        List<UserProjectRole> toBeNotified = [
                DomainFactory.createUserProjectRole(project: project, manageUsers: true),
                DomainFactory.createUserProjectRole(project: project, projectRoles: [pi]),
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

    void "addProjectRolesToProjectUserRole, succeeds when ProjectRoles can be add"() {
        given:
        setupData()

        User executingUser = CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR))

        Set<ProjectRole> projectRoleList = 3.collect { DomainFactory.createProjectRole() }

        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
                projectRoles: [projectRoleList.first()]
        )

        when:
        SpringSecurityUtils.doWithAuth(executingUser.username) {
            userProjectRoleService.addProjectRolesToProjectUserRole(userProjectRole, projectRoleList.toList())
        }

        then:
        CollectionUtils.containSame(userProjectRole.projectRoles,  projectRoleList)
    }

    @Unroll
    void "test #flag set function"() {
        given:
        setupData()

        List<UserProjectRole> userProjectRoles = (1..2).collect {
            DomainFactory.createUserProjectRole(
                    project: createProject(unixGroup: UNIX_GROUP),
                    (flag): false,
            )
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService."set${flag.capitalize()}"(userProjectRoles[0], true)
        }

        then:
        userProjectRoles[0]."${flag}" == true
        fileAccessMail * userProjectRoleService.mailHelperService.sendEmail(_ as String, { it.contains("ADD") }, _ as String)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService."set${flag.capitalize()}"(userProjectRoles[0], false)
        }

        then:
        userProjectRoles[0]."${flag}" == false

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

    void "test setAccessToFilesWithUserNotification"() {
        given:
        setupData()

        List<UserProjectRole> userProjectRoles = (1..2).collect {
            DomainFactory.createUserProjectRole(
                    project: createProject(unixGroup: UNIX_GROUP),
                    accessToFiles: false,
            )
        }

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.setAccessToFilesWithUserNotification(userProjectRoles[0], true)
        }

        then:
        userProjectRoles.accessToFiles.each { it == true }
        1 * userProjectRoleService.mailHelperService.sendEmail(_ as String, { it.contains("ADD") }, _ as String)
        1 * userProjectRoleService.mailHelperService.sendEmail({ it.contains("fileAccessChange") }, _ as String, _ as String, _ as List<String>)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.setAccessToFilesWithUserNotification(userProjectRoles[0], false)
        }

        then:
        userProjectRoles.accessToFiles.each { it == false }
        1 * userProjectRoleService.mailHelperService.sendEmail(_ as String, { it.contains("REMOVE") }, _ as String)
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
            userProjectRoleService."set${flag.capitalize()}"(userProjectRole, true)
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

        // valid UserProjectRole of different Projects
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

        [bioinformatician, pi, submitter].each { ProjectRole projectRole ->
            expectedUsers << addUserProjectRole(projectRoles: [projectRole], manageUsers: true).user
            addUserProjectRole(projectRoles: [projectRole], manageUsers: false)
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
        DomainFactory.createUserProjectRole(projectRoles: [CollectionUtils.exactlyOneElement(ProjectRole.findAllByName(ProjectRole.AUTHORITY_PROJECT_ROLES.first()))])

        Project project = createProject()
        Closure<UserProjectRole> addUserProjectRole = { Map properties = [:] ->
            return DomainFactory.createUserProjectRole([
                    project: project,
            ] + properties)
        }

        List<User> expectedUsers = []

        // authoritative
        [pi].each { ProjectRole projectRole ->
            expectedUsers << addUserProjectRole(projectRoles: [projectRole], enabled: true).user
            addUserProjectRole(projectRoles: [projectRole], enabled: false)
        }

        // not authoritative
        [bioinformatician, submitter, DomainFactory.createProjectRole()].each { ProjectRole projectRole ->
            [true, false].each { boolean enabled ->
                addUserProjectRole(projectRoles: [projectRole], enabled: enabled)
            }
        }

        when:
        List<User> projectAuthorities = userProjectRoleService.getProjectAuthorities(project)

        then:
        projectAuthorities == expectedUsers
    }

    void "getBioinformaticianUsers, returns all enabled users of project with an Bioinformatician ProjectRole"() {
        given:
        setupData()

        List<ProjectRole> bioInfProjectRoles = [leadBioinformatician, bioinformatician]

        // valid UserProjectRole of different Projects
        bioInfProjectRoles.each { ProjectRole pr ->
            DomainFactory.createUserProjectRole(projectRoles: [pr])
        }

        Project project = createProject()
        Closure<UserProjectRole> addUserProjectRole = { Map properties = [:] ->
            return DomainFactory.createUserProjectRole([
                    project: project,
            ] + properties)
        }

        List<User> expectedUsers = []

        // bioinformaticians
        bioInfProjectRoles.each { ProjectRole projectRole ->
            expectedUsers << addUserProjectRole(projectRoles: [projectRole], enabled: true).user
            addUserProjectRole(projectRoles: [projectRole], enabled: false)
        }

        // not bioinformaticians
        [pi, submitter, DomainFactory.createProjectRole()].each { ProjectRole projectRole ->
            [true, false].each { boolean enabled ->
                addUserProjectRole(projectRoles: [projectRole], enabled: enabled)
            }
        }

        when:
        List<User> projectBioinformaticians = UserProjectRoleService.getBioinformaticianUsers(project)

        then:
        CollectionUtils.containSame(projectBioinformaticians, expectedUsers)
    }

    void "handleSharedUnixGroupOnProjectCreation, project with shared unix group"() {
        given:
        setupData()
        Project projectA = createProject()
        UserProjectRole userProjectRoleA = DomainFactory.createUserProjectRole(project: projectA)
        Project projectB = createProject(unixGroup: projectA.unixGroup)

        when:
        userProjectRoleService.handleSharedUnixGroupOnProjectCreation(projectB, projectB.unixGroup)

        then:
        CollectionUtils.exactlyOneElement(UserProjectRole.findAllByProject(projectB)).equalByAccessRelatedProperties(userProjectRoleA)
    }

    void "handleSharedUnixGroupOnProjectCreation, unix group is not shared"() {
        given:
        setupData()
        Project project = createProject()

        when:
        userProjectRoleService.handleSharedUnixGroupOnProjectCreation(project, project.unixGroup)

        then:
        UserProjectRole.findAllByProject(project) == []
    }

    void "applyUserProjectRolesOntoProject, properly applies, creates new, overwrites already existing"() {
        given:
        setupData()
        List<User> users = [
            DomainFactory.createUser(),
            DomainFactory.createUser(),
            DomainFactory.createUser(),
        ]

        Project projectA = createProject()
        DomainFactory.createUserProjectRole(project: projectA, user: users[0], enabled: false)
        DomainFactory.createUserProjectRole(project: projectA, user: users[1], manageUsers: true)
        DomainFactory.createUserProjectRole(project: projectA, user: users[2], accessToOtp: false)

        Project projectB = createProject()
        DomainFactory.createUserProjectRole(project: projectB, user: users[0], enabled: false)
        DomainFactory.createUserProjectRole(project: projectB, user: users[1], manageUsers: true)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            userProjectRoleService.applyUserProjectRolesOntoProject(projectA, projectB)
        }

        then:
        List<UserProjectRole> userProjectRolesA = UserProjectRole.findAllByProject(projectA)
        List<UserProjectRole> userProjectRolesB = UserProjectRole.findAllByProject(projectB)
        userProjectRolesB.size() == 3
        users.every { User user ->
            (userProjectRolesA.find { it.user == user }).equalByAccessRelatedProperties(userProjectRolesB.find { it.user == user })
        }
    }

    void "commandTemplate uses the correct processing option depending on the operator action"() {
        given:
        setupData()
        setupAdGroupToolSnippetProcessingOptions()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole()

        when:
        String scriptCommand = userProjectRoleService.commandTemplate(userProjectRole, operatorAction)

        then:
        switch (operatorAction) {
            case UserProjectRoleService.OperatorAction.ADD:
            case UserProjectRoleService.OperatorAction.REMOVE:
                scriptCommand == "${AD_GROUP_TOOL_PATH} ${operatorAction.name()} ${userProjectRole.project.unixGroup} ${userProjectRole.user.username}"
                break
            default:
                throw new UnsupportedOperationException("Unhandled ${UserProjectRoleService.OperatorAction} '${operatorAction}'")
        }

        where:
        operatorAction << Arrays.asList(UserProjectRoleService.OperatorAction.values())
    }

    @SuppressWarnings("GStringExpressionWithinString")
    void "commandTemplate throws exception when not all parameters are mapped"() {
        given:
        setupData()
        DomainFactory.createProcessingOptionLazy(
                name : ProcessingOption.OptionName.AD_GROUP_ADD_USER_SNIPPET,
                value: "script.sh add \${unixGroup} \${some_other_property}",
        )

        when:
        userProjectRoleService.commandTemplate(DomainFactory.createUserProjectRole(), UserProjectRoleService.OperatorAction.ADD)

        then:
        thrown(MissingPropertyException)
    }

    UserProjectRole createProjectUserForNotificationTest(Project project, boolean receivesNotifications, boolean userEnabled, boolean projectUserEnabled) {
        return DomainFactory.createUserProjectRole(
                project              : project ?: createProject(),
                user                 : DomainFactory.createUser(enabled: userEnabled),
                receivesNotifications: receivesNotifications,
                enabled              : projectUserEnabled,
        )
    }

    List<UserProjectRole> setupProjectUserForAllNotificationCombinations(Project project = null) {
        List<UserProjectRole> projectUsers = []
        [true, false].each { boolean receivesNotifications ->
            [true, false].each { boolean userEnabled ->
                [true, false].each { boolean projectUserEnabled ->
                    projectUsers << createProjectUserForNotificationTest(project, receivesNotifications, userEnabled, projectUserEnabled)
                }
            }
        }
        return projectUsers
    }

    void "getEmailsForNotification, mails of all to be notified users, sorted and concatenated with ','"() {
        given:
        setupData()

        Project project = DomainFactory.createProject()
        List<UserProjectRole> projectUsers = setupProjectUserForAllNotificationCombinations(project)

        String expected = projectUsers.findAll { UserProjectRole projectUser ->
            projectUser.enabled && projectUser.user.enabled && projectUser.receivesNotifications
        }*.user*.email.sort().join(",")

        expect:
        userProjectRoleService.getEmailsForNotification(project) == expected
    }

    void "getEmailsForNotification, returns empty string for project without users"() {
        given:
        setupData()

        expect:
        userProjectRoleService.getEmailsForNotification(DomainFactory.createProject()) == ""
    }

    void "getProjectUsersToBeNotified, only fully enabled project users with notification true of given project"() {
        given:
        setupData()

        setupProjectUserForAllNotificationCombinations()

        Project project = DomainFactory.createProject()
        List<UserProjectRole> projectUsers = setupProjectUserForAllNotificationCombinations(project)

        List<UserProjectRole> expected = projectUsers.findAll { UserProjectRole projectUser ->
            projectUser.enabled && projectUser.user.enabled && projectUser.receivesNotifications
        }

        when:
        List<UserProjectRole> projectUsersToBeNotified = userProjectRoleService.getProjectUsersToBeNotified(project)

        then:
        TestCase.assertContainSame(projectUsersToBeNotified, expected)
    }

    void "getProjectUsersToBeNotified, on project without users, returns empty list"() {
        given:
        setupData()

        expect:
        userProjectRoleService.getProjectUsersToBeNotified(DomainFactory.createProject()) == []
    }

    void "getMails, converts all objects, regardless of notification status"() {
        given:
        setupData()

        List<UserProjectRole> projectUsers = setupProjectUserForAllNotificationCombinations()
        List<String> expected = projectUsers*.user*.email

        when:
        List<String> mails = userProjectRoleService.getMails(projectUsers)

        then:
        TestCase.assertContainSame(mails, expected)
    }

    void "getMails, on empty list, returns empty string list"() {
        given:
        setupData()

        expect:
        userProjectRoleService.getMails([]) == []
    }

    MessageSourceService getMessageSourceServiceWithMockedMessageSource() {
        return new MessageSourceService(messageSource: messageSource)
    }

    @SuppressWarnings('GStringExpressionWithinString')
    PluginAwareResourceBundleMessageSource getMessageSource() {
        return Mock(PluginAwareResourceBundleMessageSource) {
            _ * getMessageInternal("projectUser.notification.addToUnixGroup.subject", [], _) >>
                    '''addToUnixGroup
                    |${requester}
                    |${action}
                    |${username}
                    |${conjunction}
                    |${unixGroup}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.addToUnixGroup.body", [], _) >>
                    '''addToUnixGroup
                    |${projectUnixGroup}
                    |${projectList}
                    |${requestedAction}
                    |${scriptCommand}
                    |${affectedUserUserDetail}
                    |${requesterUserDetail}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.addToUnixGroup.userDetail", [], _) >>
                    '''\
                    |${realName}
                    |${username}
                    |${email}
                    |${role}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.newProjectMember.subject", [], _) >>
                    '''newProjectMember
                    |${projectName}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.newProjectMember.body.userManagerAddedMember", [], _) >>
                    '''newProjectMember
                    |${executingUser}
                    |${userIdentifier}
                    |${projectRole}
                    |${projectName}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.newProjectMember.body.administrativeUserAddedSubmitter", [], _) >>
                    '''administrativeUserAddedSubmitter
                    |${userIdentifier}
                    |${projectRole}
                    |${projectName}
                    |${supportTeamName}
                    |${supportTeamSalutation}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.fileAccessChange.subject", [], _) >>
                    '''fileAccessChange
                    |${projectName}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.fileAccessChange.body", [], _) >>
                    '''fileAccessChange
                    |${username}
                    |${requester}
                    |${projectName}
                    |${dirAnalysis}
                    |${clusterName}
                    |${clusterAdministrationEmail}
                    |${supportTeamSalutation}'''.stripMargin()
        }
    }
}
