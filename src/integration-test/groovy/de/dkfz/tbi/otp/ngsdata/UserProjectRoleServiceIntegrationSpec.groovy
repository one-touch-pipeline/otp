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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.authentication.AuthenticationTrustResolver
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.security.user.identityProvider.data.IdpUserDetails
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path
import java.time.*
import java.time.temporal.ChronoUnit

@Rollback
@Integration
class UserProjectRoleServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore, UserDomainFactory {

    private final static String UNIX_GROUP = "UNIX_GROUP"
    private final static String OTHER_GROUP = "OTHER_GROUP"
    private final static String SEARCH = "search"

    private final static String EMAIL_SENDER_SALUTATION = "the supportTeam"
    private final static String CLUSTER_NAME = "OTP Cluster"

    private final static String AD_GROUP_TOOL_PATH = "path/to/script.sh"

    private final static String EMAIL_CLUSTER_ADMINISTRATION = HelperUtils.randomEmail
    private static final String EMAIL_INTERN = 'intern@de.de'
    private static final String EMAIL_EXTERN = 'extern@de.de'
    private static final String EMAIL_TICKET_SYSTEM = HelperUtils.randomEmail
    private static final String EMAIL_SENDER_NAME = "Tina Test"

    AutoTimestampEventListener autoTimestampEventListener
    RoleHierarchy roleHierarchy
    TestConfigService configService
    UserProjectRoleService userProjectRoleService

    @TempDir
    Path tempDir

    void setupData() {
        createUserAndRoles()
        createAllBasicProjectRoles()

        SecurityService securityService = new SecurityService(
                authenticationTrustResolver: Mock(AuthenticationTrustResolver) {
                    isAnonymous(_) >> false
                },
                roleHierarchy: roleHierarchy,
                configService: configService,
        )

        configService.addOtpProperties(tempDir)

        userProjectRoleService = new UserProjectRoleService()
        userProjectRoleService.messageSourceService = messageSourceServiceWithMockedMessageSource
        userProjectRoleService.securityService = securityService
        userProjectRoleService.auditLogService = new AuditLogService()
        userProjectRoleService.auditLogService.securityService = securityService
        userProjectRoleService.auditLogService.processingOptionService = new ProcessingOptionService()
        userProjectRoleService.processingOptionService = new ProcessingOptionService()
        userProjectRoleService.configService = configService
        userProjectRoleService.userProjectRoleService = userProjectRoleService
        userProjectRoleService.dicomAuditUtils = new DicomAuditUtils()
        userProjectRoleService.dicomAuditUtils.securityService = securityService

        userProjectRoleService.mailHelperService = Mock(MailHelperService) {
            getTicketSystemEmailAddress() >> EMAIL_TICKET_SYSTEM
            getSenderName() >> EMAIL_SENDER_NAME
        }

        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.HELP_DESK_TEAM_NAME,
                type: null,
                project: null,
                value: EMAIL_SENDER_SALUTATION,
        )
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.CLUSTER_NAME,
                type: null,
                project: null,
                value: CLUSTER_NAME,
        )
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION,
                type: null,
                project: null,
                value: EMAIL_CLUSTER_ADMINISTRATION,
        )
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.OTP_SYSTEM_USER,
                type: null,
                project: null,
                value: SYSTEM_USER,
        )
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM,
                value: EMAIL_TICKET_SYSTEM,
        )
    }

    void cleanup() {
        configService.clean()
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
                    name: action.commandTemplateOptionName,
                    value: "${prefix} ${action.name()} ${template}",
            )
        }
    }

    @Unroll
    void "createUserProjectRole, asserts that the user is not already connected to the project (username=#username)"() {
        given:
        setupData()

        UserProjectRole upr = createUserProjectRole(
                user: createUser(username: username)
        )

        when:
        doWithAuth(OPERATOR) {
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
    void "createUserProjectRole, sets manageUsers and delegate users true for #projectRoleNames when manageUsers #manageUsers and delegateUser #delegateUsers"() {
        given:
        setupData()

        Project project = createProject()
        User user = createUser()
        Map<String, Boolean> flags = [
                accessToOtp           : true,
                accessToFiles         : true,
                manageUsers           : manageUsers,
                manageUsersAndDelegate: delegateUsers,
                receivesNotifications : true,
        ]
        Set<ProjectRole> projectRoles = projectRoleNames.collect({ roleName -> ProjectRole.findByName(roleName.name()) })

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.createUserProjectRole(user, project, projectRoles as Set, flags)
        }

        then:
        UserProjectRole.count == 1
        UserProjectRole createUserProjectRole = UserProjectRole.findAll()[0]
        createUserProjectRole.accessToOtp
        createUserProjectRole.manageUsers
        createUserProjectRole.accessToFiles
        createUserProjectRole.manageUsersAndDelegate
        createUserProjectRole.receivesNotifications
        createUserProjectRole.projectRoles == projectRoles

        where:
        manageUsers | delegateUsers | projectRoleNames
        true        | true          | [ProjectRole.Basic.BIOINFORMATICIAN]
        false       | false         | [ProjectRole.Basic.PI]
        false       | true          | [ProjectRole.Basic.COORDINATOR]
    }

    @Unroll
    void "notifyAdministration sends email with correct content (manual execution) (action=#operatorAction)"() {
        given:
        setupData()
        setupAdGroupToolSnippetProcessingOptions()

        Project project = createProject()
        UserProjectRole userProjectRole = createUserProjectRole(project: project)
        UserProjectRole requesterUserProjectRole = createUserProjectRole(project: project)
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
        doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.notifyAdministration(userProjectRole, operatorAction)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(
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
                |Command to execute:
                |${scriptCommand}
                |${affectedUserUserDetail}
                |${requesterUserDetail}""".stripMargin()
        )

        where:
        conjunction | operatorAction
        "to"        | UserProjectRoleService.OperatorAction.ADD
        "from"      | UserProjectRoleService.OperatorAction.REMOVE
    }

    @Unroll
    void "notifyAdministration sends email with correct content (automatic execution)"() {
        given:
        setupData()
        setupAdGroupToolSnippetProcessingOptions()
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.AD_GROUP_USER_SNIPPET_EXECUTE,
                value: "true",
        )

        userProjectRoleService.remoteShellHelper = Mock(RemoteShellHelper)

        Project project = createProject()
        UserProjectRole userProjectRole = createUserProjectRole(project: project)
        UserProjectRole requesterUserProjectRole = createUserProjectRole(project: project)
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
        doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.notifyAdministration(userProjectRole, operatorAction)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(
                """${subject}addToUnixGroup
                |${requesterUserProjectRole.user.username}
                |${formattedAction}
                |${userProjectRole.user.username}
                |${conjunction}
                |${userProjectRole.project.unixGroup}""".stripMargin(),
                """addToUnixGroup
                |${userProjectRole.project.unixGroup}
                |${userProjectRole.project.name}
                |${operatorAction}
                |Command '${scriptCommand}' was executed ${result}.
                |Output:
                |test-output
                |Error:
                |test-error
                |${affectedUserUserDetail}
                |${requesterUserDetail}""".stripMargin()
        )
        1 * userProjectRoleService.remoteShellHelper.executeCommandReturnProcessOutput(
                _, scriptCommand
        ) >> new ProcessOutput("test-output", "test-error", exitCode)

        where:
        exitCode | operatorAction                               || result           | conjunction | subject
        0        | UserProjectRoleService.OperatorAction.ADD    || "successfully"   | "to"        | "DONE: "
        1        | UserProjectRoleService.OperatorAction.REMOVE || "unsuccessfully" | "from"      | "ERROR: "
        1        | UserProjectRoleService.OperatorAction.ADD    || "unsuccessfully" | "to"        | "ERROR: "
    }

    void "notifyAdministration lists other projects with same unix group as requested project"() {
        given:
        setupData()

        Project usedProject = createProject(unixGroup: UNIX_GROUP)
        Project otherProject1 = createProject(unixGroup: UNIX_GROUP)
        Project otherProject2 = createProject(unixGroup: UNIX_GROUP)

        createProject(unixGroup: OTHER_GROUP)

        UserProjectRole userProjectRole = createUserProjectRole(project: usedProject)

        String projectList = [otherProject1, otherProject2]*.name.sort().join(", ")

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.notifyAdministration(userProjectRole, UserProjectRoleService.OperatorAction.ADD)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(_, { it.contains(projectList) })
    }

    @Unroll
    void "notifyAdministration sends email with unswitched user as requester (action=#operatorAction)"() {
        given:
        setupData()
        setupAdGroupToolSnippetProcessingOptions()

        User executingUser = CollectionUtils.exactlyOneElement(User.findAllByUsername(ADMIN))
        User switchedUser = CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR))

        UserProjectRole userProjectRole = createUserProjectRole()
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
        doWithAuth(executingUser.username) {
            doAsSwitchedToUser(switchedUser.username) {
                userProjectRoleService.notifyAdministration(userProjectRole, operatorAction)
            }
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(
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
                |Command to execute:
                |${scriptCommand}
                |${affectedUserUserDetail}
                |${requesterUserDetail}""".stripMargin()
        )

        where:
        conjunction | operatorAction
        "to"        | UserProjectRoleService.OperatorAction.ADD
        "from"      | UserProjectRoleService.OperatorAction.REMOVE
    }

    @Unroll
    void "toggleEnabled, send mails and remove permissions but not grant them (enabledStatus=#enabledStatus)"() {
        given:
        setupData()

        Project project = createProject(unixGroup: UNIX_GROUP)
        UserProjectRole requesterUserProjectRole = createUserProjectRole(
                project: project,
                projectRoles: [pi],
                manageUsers: true,
                user: getUser(USER)
        )
        UserProjectRole userProjectRole = createUserProjectRole(
                project: project,
                accessToOtp: !accountInLdap,
                accessToFiles: accessToFiles,
                manageUsers: manageUsers,
                manageUsersAndDelegate: manageUsersAndDelegate,
                receivesNotifications: receivesNotifications,
                enabled: enabledStatus,
        )

        // mock user currently logged in
        userProjectRoleService.securityService = Mock(SecurityService) {
            getCurrentUser() >> requesterUserProjectRole.user
        }

        // mock user existence in ldap
        userProjectRoleService.identityProvider = Mock(IdentityProvider) {
            isUserInIdpAndActivated(_) >> accountInLdap
        }

        when:
        doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.setEnabled(userProjectRole, !enabledStatus)
        }

        then: "new enabled status was set"
        userProjectRole.enabled == !enabledStatus

        and: "all permissions are taken and non are granted"
        !userProjectRole.accessToOtp
        !userProjectRole.accessToFiles
        !userProjectRole.manageUsers
        !userProjectRole.manageUsersAndDelegate
        !userProjectRole.receivesNotifications

        and: "notification for unix group administration was sent"
        removeMailCount * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(_ as String, { it.contains("REMOVE") })

        and: "notification for user managers that a user has been enabled was sent"
        notifyEnableUser * userProjectRoleService.mailHelperService.sendEmail({
            it.contains("newProjectMember")
        }, _ as String, [requesterUserProjectRole.user.email, userProjectRole.user.email])

        and: "notification for user that he was disabled"
        (enabledStatus ? 1 : 0) * userProjectRoleService.mailHelperService.sendEmail({
            it.contains("deactivateUserIn")
        }, _ as String, [userProjectRole.user.email], [requesterUserProjectRole.user.email])

        0 * userProjectRoleService.mailHelperService.sendEmail(*_)

        where:
        accountInLdap | accessToFiles | manageUsers | manageUsersAndDelegate | receivesNotifications | enabledStatus || removeMailCount | notifyEnableUser
        true          | true          | true        | true                   | true                  | true          || 1               | 0
        true          | false         | true        | true                   | true                  | true          || 0               | 0
        true          | false         | false       | false                  | false                 | false         || 0               | 1
        false         | false         | false       | false                  | false                 | false         || 0               | 1
    }

    @Unroll
    void "setEnabled, setting enabled to #enabled should be restricted, when not admins try to remove pi userProjectRoles"() {
        given:
        setupData()

        User user = createUser()
        UserProjectRole userProjectRolePi = createUserProjectRole(user: user, projectRoles: [pi])

        User currentUser = getUser(USER)
        createUserProjectRole(user: currentUser, projectRoles: [pi])

        when:
        doWithAuth(USER) {
            userProjectRoleService.setEnabled(userProjectRolePi, enabled)
        }

        then:
        thrown(InsufficientRightsException)

        where:
        enabled << [true, false]
    }

    void "setEnabled, setting disabled should always be possible, if userProjectRole is the current users userProjectRole"() {
        given:
        setupData()

        User currentUser = getUser(USER)
        UserProjectRole userProjectRole = createUserProjectRole(user: currentUser, projectRoles: [pi], enabled: true)

        when:
        doWithAuth(USER) {
            userProjectRoleService.setEnabled(userProjectRole, false)
        }

        then:
        !userProjectRole.enabled
    }

    void "setEnabled, setting enabled, if user is userProjectRoles user should not be possible"() {
        given:
        setupData()

        User currentUser = getUser(USER)
        UserProjectRole userProjectRole = createUserProjectRole(user: currentUser, projectRoles: [pi], enabled: false)

        // mock user currently logged in
        userProjectRoleService.securityService = Mock(SecurityService)

        when:
        doWithAuth(USER) {
            userProjectRoleService.setEnabled(userProjectRole, true)
        }

        then:
        1 * userProjectRoleService.securityService.currentUser >> currentUser

        and:
        thrown(InsufficientRightsException)
    }

    @Unroll
    void "addUserToProjectAndNotifyGroupManagementAuthority, create User if non is found for username for #name"() {
        given:
        setupData()

        createUser([username: 'intern', email: EMAIL_INTERN])
        createUser([username: 'extern', email: EMAIL_EXTERN])

        Project project = createProject()
        ProjectRole projectRole = createProjectRole()
        IdpUserDetails idpUserDetails = new IdpUserDetails(
                username: "unknown",
                realName: "Unknown User",
                mail: mailOfUser,
        )
        userProjectRoleService.userService = new UserService()
        userProjectRoleService.identityProvider = Mock(IdentityProvider) {
            getIdpUserDetailsByUsername(_) >> idpUserDetails
        }

        expect:
        User.findAllByUsernameAndEmail(idpUserDetails.username, idpUserDetails.mail).empty

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole as Set<ProjectRole>, SEARCH, [:])
        }

        then:
        User.findAllByUsernameAndEmail(idpUserDetails.username, idpUserDetails.mail)

        where:
        name                       | mailOfUser
        'new mail'                 | "unknownUser@dummy.com"
        'mail used by intern user' | EMAIL_INTERN
        'mail used by extern user' | EMAIL_EXTERN
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, create UserProjectRole for existing User"() {
        given:
        setupData()

        User user = createUser()
        Project project = createProject()
        ProjectRole projectRole = createProjectRole()
        IdpUserDetails idpUserDetails = new IdpUserDetails(
                username: user.username,
                realName: user.realName,
                mail: user.email,
        )
        userProjectRoleService.identityProvider = Mock(IdentityProvider) {
            getIdpUserDetailsByUsername(_) >> idpUserDetails
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
        doWithAuth(OPERATOR) {
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
        ProjectRole projectRole = createProjectRole()
        userProjectRoleService.identityProvider = Mock(IdentityProvider) {
            getIdpUserDetailsByUsername(_) >> null
        }

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(project, projectRole as Set<ProjectRole>, SEARCH, [:])
        }

        then:
        LdapUserCreationException e = thrown(LdapUserCreationException)
        e.message.contains('can not be resolved to a user via LDAP')
    }

    @Unroll
    void "addUserToProjectAndNotifyGroupManagementAuthority, send mail only for users with access to files (accessToFiles=#accessToFiles)"() {
        given:
        setupData()
        createUser(username: SYSTEM_USER)

        User user = createUser()
        IdpUserDetails idpUserDetails = new IdpUserDetails(
                username: user.username,
                realName: user.realName,
                mail: user.email,
        )
        Project project = createProject(unixGroup: UNIX_GROUP)
        UserProjectRole requesterUserProjectRole = createUserProjectRole(
                project: project,
                projectRoles: [pi],
                manageUsers: true
        )
        userProjectRoleService.identityProvider = Mock(IdentityProvider) {
            getIdpUserDetailsByUsername(_) >> idpUserDetails
            getGroupsOfUser(_) >> []
        }

        int expectedInvocations = accessToFiles ? 1 : 0

        when:
        doWithAuth(requesterUserProjectRole.user.username) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(
                    project,
                    createProjectRole() as Set<ProjectRole>,
                    user.username,
                    [accessToFiles: accessToFiles]
            )
        }

        then: "notification for unix group administration was sent"
        expectedInvocations * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(_ as String, _ as String)

        and: "notification for user managers regarding file access was sent"
        expectedInvocations * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String, user.email, _ as List<String>)

        and: "notification for user managers regarding new user was sent"
        1 * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String,
                [requesterUserProjectRole.user.email, user.email])

        and:
        UserProjectRole.findAllByIdNotEqual(requesterUserProjectRole.id)*.fileAccessChangeRequested == [accessToFiles]

        where:
        accessToFiles << [true, false]
    }

    void "addUserToProjectAndNotifyGroupManagementAuthority, synchronizes action for projects with shared unix group"() {
        given:
        setupData()
        Project projectA = createProject()
        Project projectB = createProject(unixGroup: projectA.unixGroup)

        User user = createUser()
        IdpUserDetails idpUserDetails = new IdpUserDetails(
                username: user.username,
                realName: user.realName,
                mail: user.email,
        )

        userProjectRoleService.identityProvider = Mock(IdentityProvider) {
            getIdpUserDetailsByUsername(_) >> idpUserDetails
        }

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.addUserToProjectAndNotifyGroupManagementAuthority(projectA, createProjectRole() as Set<ProjectRole>, SEARCH)
        }

        then:
        UserProjectRole.findAllByUserAndProjectInList(user, [projectA, projectB]).size() == 2
    }

    void "addExternalUserToProject, create User if non is found for realName or email"() {
        given:
        setupData()

        ProjectRole projectRole = createProjectRole()
        String realName = "realName"
        String email = "email@dummy.de"
        Project project = createProject()
        userProjectRoleService.userService = new UserService()

        expect:
        User.findAllByEmail(email).empty

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, realName, email, projectRole as Set<ProjectRole>)
        }

        then:
        User.findAllByEmail(email)
    }

    void "addExternalUserToProject, create UserProjectRole for new User"() {
        given:
        setupData()

        ProjectRole projectRole = createProjectRole()
        User user = createUser(username: null)
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
        doWithAuth(OPERATOR) {
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

        ProjectRole projectRole = createProjectRole()
        User user = createUser(username: null)
        Project project = createProject()

        createUserProjectRole(
                project: project,
                user: CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR)),
                manageUsers: true,
        )

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, user.realName, user.email, projectRole as Set<ProjectRole>)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String, _ as List<String>)
    }

    void "addExternalUserToProject, when user with same external email already exists, throws error"() {
        given:
        setupData()

        ProjectRole projectRole = createProjectRole()
        User user = createUser(username: null)
        Project project = createProject()

        createUserProjectRole(
                project: project,
                user: CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR)),
                manageUsers: true,
        )

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(project, 'wrong name', user.email, projectRole as Set<ProjectRole>)
        }

        then:
        AssertionError e = thrown(AssertionError)
        e.message.contains("The given email address '${user.email}' is already registered for external user '${user.realName}'")
    }

    void "addExternalUserToProject, synchronizes action for projects with shared unix group"() {
        given:
        setupData()
        Project projectA = createProject()
        Project projectB = createProject(unixGroup: projectA.unixGroup)

        User user = createUser(username: null)

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.addExternalUserToProject(projectA, user.realName, user.email, createProjectRole() as Set<ProjectRole>)
        }

        then:
        UserProjectRole.findAllByUserAndProjectInList(user, [projectA, projectB]).size() == 2
    }

    void "notifyProjectAuthoritiesAndUser, sends mail, direct recipients: authorities and user managers, CC: affected user"() {
        given:
        setupData()

        ProjectRole otherRole = createProjectRole()

        Project project = createProject()
        Closure<UserProjectRole> addUserWithProjectRole = { ProjectRole projectRole, boolean manageUsers, boolean enabled ->
            return createUserProjectRole(
                    project: project,
                    projectRoles: [projectRole],
                    manageUsers: manageUsers,
                    enabled: enabled,
            )
        }

        UserProjectRole newUserProjectRole = createUserProjectRole(project: project)

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
        doWithAuth(OPERATOR) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(newUserProjectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                _ as String,
                _ as String,
                recipients + [newUserProjectRole.user.email],
        )
    }

    void "notifyProjectAuthoritiesAndUser, is unaffected by receivesNotification flag"() {
        given:
        setupData()

        Project project = createProject()
        UserProjectRole uprToBeNotified = createUserProjectRole(
                user: createUser(),
                project: project,
                projectRoles: [pi],
                receivesNotifications: false,
        )
        UserProjectRole newUPR = createUserProjectRole(project: project)

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(newUPR)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_ as String, _ as String,
                [uprToBeNotified.user.email, newUPR.user.email])
    }

    @Unroll
    void "notifyProjectAuthoritiesAndUser sends email with correct content (authoritative=#authoritative, role=#projectRoleName)"() {
        given:
        setupData()

        ProjectRole projectRole = projectRoleName ? CollectionUtils.exactlyOneElement(ProjectRole.findAllByName(projectRoleName)) : createProjectRole()
        UserProjectRole newUPR = createUserProjectRole(
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
        UserProjectRole executingUPR = createUserProjectRole(properties)

        String expectedSubject = "newProjectMember\n${newUPR.project.name}"
        String expectedContent = "newProjectMember\n${EMAIL_SENDER_NAME}\n${newUPR.user.realName}\n${newUPR.projectRoles.name.join(",")}\n${newUPR.project.name}"

        when:
        doWithAuth(executingUPR.user.username) {
            userProjectRoleService.notifyProjectAuthoritiesAndUser(newUPR)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                expectedSubject,
                expectedContent,
                [executingUPR.user.email, newUPR.user.email],
        )

        where:
        authoritative | projectRoleName
        true          | ProjectRole.Basic.SUBMITTER.name()
        false         | ProjectRole.Basic.SUBMITTER.name()
        true          | null
        false         | null
    }

    void "notifyProjectAuthoritiesAndDisabledUser, sends mail, direct recipients: affected, CC: authorities and user managers"() {
        given:
        setupData()

        Project project = createProject()

        createUserProjectRole(
                project: project,
                projectRoles: [bioinformatician],
                manageUsersAndDelegate: false,
                manageUsers: false,
                enabled: true,
        )
        UserProjectRole piUserRole = createUserProjectRole(
                project: project,
                projectRoles: [pi],
                manageUsersAndDelegate: true,
                manageUsers: true,
                enabled: true,
        )
        UserProjectRole manageUserRole = createUserProjectRole(
                project: project,
                projectRoles: [coordinator],
                manageUsersAndDelegate: false,
                manageUsers: true,
                enabled: true,
        )

        UserProjectRole userProjectRoleToDeactivate = createUserProjectRole(
                project: project,
                enabled: true,
        )

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.notifyProjectAuthoritiesAndDisabledUser(userProjectRoleToDeactivate)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(_, _, [userProjectRoleToDeactivate.user.email], [piUserRole, manageUserRole]*.user*.email)
    }

    void "notifyUsersAboutFileAccessChange, builds correct content"() {
        given:
        setupData()

        Project project = createProject(dirAnalysis: "/dev/null")
        UserProjectRole userProjectRole = createUserProjectRole(
                project: project,
        )

        List<UserProjectRole> toBeNotified = [
                createUserProjectRole(project: project, manageUsers: true),
                createUserProjectRole(project: project, projectRoles: [pi]),
        ]
        String linkProjectDirectory = LsdfFilesService.getPath(configService.rootPath.path, project.dirName)

        String projectName = project.name
        String expectedBody = """\
            fileAccessChange
            ${userProjectRole.user.realName}
            ${EMAIL_SENDER_NAME}
            ${projectName}
            ${project.dirAnalysis}
            ${CLUSTER_NAME}
            ${EMAIL_CLUSTER_ADMINISTRATION}
            ${linkProjectDirectory}
            ${EMAIL_SENDER_SALUTATION}""".stripIndent()

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.notifyUsersAboutFileAccessChange(userProjectRole)
        }

        then:
        1 * userProjectRoleService.mailHelperService.sendEmail(
                'fileAccessChange\n' + projectName,
                expectedBody,
                userProjectRole.user.email,
                toBeNotified*.user*.email.sort(),
        )
    }

    void "addProjectRolesToProjectUserRole, succeeds when ProjectRoles can be add"() {
        given:
        setupData()

        User executingUser = CollectionUtils.exactlyOneElement(User.findAllByUsername(OPERATOR))

        Set<ProjectRole> projectRoleList = 3.collect { createProjectRole() }

        UserProjectRole userProjectRole = createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
                projectRoles: [projectRoleList.first()]
        )

        when:
        doWithAuth(executingUser.username) {
            userProjectRoleService.addProjectRolesToProjectUserRole(userProjectRole, projectRoleList.toList())
        }

        then:
        CollectionUtils.containSame(userProjectRole.projectRoles, projectRoleList)
    }

    @Unroll
    void "test #flag set function"() {
        given:
        setupData()

        List<UserProjectRole> userProjectRoles = (1..2).collect {
            createUserProjectRole(
                    project: createProject(unixGroup: UNIX_GROUP),
                    (flag): false,
            )
        }

        // mock user currently logged in
        userProjectRoleService.securityService = Mock(SecurityService) {
            getCurrentUser() >> getUser(OPERATOR)
        }

        // mock user existence in ldap
        userProjectRoleService.identityProvider = Mock(IdentityProvider) {
            isUserInIdpAndActivated(_) >> true
        }

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService."set${flag.capitalize()}"(userProjectRoles[0], true)
        }

        then:
        userProjectRoles[0]."${flag}" == true
        fileAccessMail * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(_ as String, { it.contains("ADD") })

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService."set${flag.capitalize()}"(userProjectRoles[0], false)
        }

        then:
        userProjectRoles[0]."${flag}" == false

        fileAccessMail * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(_ as String, { it.contains("REMOVE") })
        _ * userProjectRoleService.mailHelperService.sendEmail(*_)

        where:
        flag                     | fileAccessMail
        "accessToFiles"          | 1
        "manageUsers"            | 0
        "manageUsersAndDelegate" | 0
        "receivesNotifications"  | 0
        "enabled"                | 0
    }

    @Unroll
    void "setAccessToOtp, when called with UserProjectRole where accessToOtp is #accessToOtpVar with #accessToOtpCall, then database entry should be set to #accessToOtpCall"() {
        given:
        setupData()
        createUser([username: SYSTEM_USER])
        UserProjectRole userProjectRole = createUserProjectRole(
                accessToOtp: accessToOtpVar,
        )

        when:
        doWithAuth(USER) {
            userProjectRoleService.setAccessToOtp(userProjectRole, accessToOtpCall)
        }

        then:
        userProjectRole.accessToOtp == accessToOtpCall

        where:
        accessToOtpVar | accessToOtpCall
        true           | true
        true           | false
        false          | true
        false          | false
    }

    @Unroll
    void "setAccessToFiles, when oldFile Access is #oldFileAccess and new is #newFileAccess and force is #force, then create expected AuditLog"() {
        given:
        setupData()
        createUser([username: SYSTEM_USER])

        UserProjectRole userProjectRole = createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: oldFileAccess,
        )

        when:
        doWithAuth(USER) {
            userProjectRoleService.setAccessToFiles(userProjectRole, newFileAccess, force)
        }

        then:
        AuditLog.count() == auditLogCount
        CollectionUtils.exactlyOneElement(AuditLog.findAllByAction(action)).user.username == user

        where:
        oldFileAccess | newFileAccess | force || auditLogCount | action                                               | user
        false         | true          | false || 2             | AuditLog.Action.PROJECT_USER_CHANGED_ACCESS_TO_FILES | USER
        true          | false         | false || 2             | AuditLog.Action.PROJECT_USER_CHANGED_ACCESS_TO_FILES | USER
        false         | true          | true  || 1             | AuditLog.Action.LDAP_BASED_CHANGED_ACCESS_TO_FILES   | SYSTEM_USER
        true          | false         | true  || 1             | AuditLog.Action.LDAP_BASED_CHANGED_ACCESS_TO_FILES   | SYSTEM_USER
    }

    @Unroll
    void "setAccessToFiles, when old and new File Access is the same (#fileAccess) and force is #force, then create no AuditLog"() {
        given:
        setupData()
        createUser([username: SYSTEM_USER])

        UserProjectRole userProjectRole = createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
                accessToFiles: fileAccess,
        )

        when:
        doWithAuth(USER) {
            userProjectRoleService.setAccessToFiles(userProjectRole, fileAccess, force)
        }

        then:
        AuditLog.count == 0

        where:
        fileAccess | force
        false      | false
        true       | false
        false      | true
        true       | true
    }

    void "test setAccessToFilesWithUserNotification"() {
        given:
        setupData()

        List<UserProjectRole> userProjectRoles = (1..2).collect {
            createUserProjectRole(
                    project: createProject(unixGroup: UNIX_GROUP),
                    accessToFiles: false,
            )
        }

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.setAccessToFilesWithUserNotification(userProjectRoles[0], true)
        }

        then:
        userProjectRoles.accessToFiles.each { it == true }
        1 * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(_ as String, { it.contains("ADD") })
        1 * userProjectRoleService.mailHelperService.sendEmail({ it.contains("fileAccessChange") }, _ as String, _ as String, _ as List<String>)

        when:
        doWithAuth(OPERATOR) {
            userProjectRoleService.setAccessToFilesWithUserNotification(userProjectRoles[0], false)
        }

        then:
        userProjectRoles.accessToFiles.each { it == false }
        1 * userProjectRoleService.mailHelperService.sendEmailToTicketSystem(_ as String, { it.contains("REMOVE") })
    }

    @Unroll
    void "test #flag access as project user with #role rights; userProjectRole contains ProjectRole #projectRole; initialized with #flagInit; expect success"() {
        given:
        setupData()

        User user = getUser(role)
        UserProjectRole userProjectRole = createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
                (flag): flagInit,
                projectRoles: ProjectRole.findAllByName(projectRole.name())
        )

        // mock user existence in ldap
        userProjectRoleService.identityProvider = Mock(IdentityProvider) {
            isUserInIdpAndActivated(_) >> true
        }

        when:
        doWithAuth(user.username) {
            userProjectRoleService."set${flag.capitalize()}"(userProjectRole, !flagInit)
        }

        then:
        userProjectRole."${flag}" == result

        where:
        flag                     | flagInit | projectRole                 | role     | result
        //USER false -> (desired)true, Role PI
        "accessToFiles"          | false    | ProjectRole.Basic.PI        | USER     | true
        "manageUsers"            | false    | ProjectRole.Basic.PI        | USER     | true
        "manageUsersAndDelegate" | false    | ProjectRole.Basic.PI        | USER     | true
        "receivesNotifications"  | false    | ProjectRole.Basic.PI        | USER     | true

        //OPERATOR false -> (desired)true, Role PI
        "accessToFiles"          | false    | ProjectRole.Basic.PI        | OPERATOR | true
        "manageUsers"            | false    | ProjectRole.Basic.PI        | OPERATOR | true
        "manageUsersAndDelegate" | false    | ProjectRole.Basic.PI        | OPERATOR | true
        "receivesNotifications"  | false    | ProjectRole.Basic.PI        | OPERATOR | true
        "enabled"                | false    | ProjectRole.Basic.PI        | OPERATOR | true

        // ADMIN false -> (desired)true, Role PI
        "accessToFiles"          | false    | ProjectRole.Basic.PI        | ADMIN    | true
        "manageUsers"            | false    | ProjectRole.Basic.PI        | ADMIN    | true
        "manageUsersAndDelegate" | false    | ProjectRole.Basic.PI        | ADMIN    | true
        "receivesNotifications"  | false    | ProjectRole.Basic.PI        | ADMIN    | true
        "enabled"                | false    | ProjectRole.Basic.PI        | ADMIN    | true

        //USER true -> (desired)false, Role PI
        "accessToFiles"          | true     | ProjectRole.Basic.PI        | USER     | false
        "manageUsers"            | true     | ProjectRole.Basic.PI        | USER     | false
        "manageUsersAndDelegate" | true     | ProjectRole.Basic.PI        | USER     | false
        "receivesNotifications"  | true     | ProjectRole.Basic.PI        | USER     | false

        //OPERATOR true -> (desired)false, Role PI
        "accessToFiles"          | true     | ProjectRole.Basic.PI        | OPERATOR | false
        "manageUsers"            | true     | ProjectRole.Basic.PI        | OPERATOR | false
        "manageUsersAndDelegate" | true     | ProjectRole.Basic.PI        | OPERATOR | false
        "receivesNotifications"  | true     | ProjectRole.Basic.PI        | OPERATOR | false
        "enabled"                | true     | ProjectRole.Basic.PI        | OPERATOR | false

        // ADMIN true -> (desired)false, Role PI
        "accessToFiles"          | true     | ProjectRole.Basic.PI        | ADMIN    | false
        "manageUsers"            | true     | ProjectRole.Basic.PI        | ADMIN    | false
        "manageUsersAndDelegate" | true     | ProjectRole.Basic.PI        | ADMIN    | false
        "receivesNotifications"  | true     | ProjectRole.Basic.PI        | ADMIN    | false
        "enabled"                | true     | ProjectRole.Basic.PI        | ADMIN    | false

        //USER false -> (desired)true, Role Submitter
        "accessToFiles"          | false    | ProjectRole.Basic.SUBMITTER | USER     | true
        "manageUsers"            | false    | ProjectRole.Basic.SUBMITTER | USER     | true
        "manageUsersAndDelegate" | false    | ProjectRole.Basic.SUBMITTER | USER     | true
        "receivesNotifications"  | false    | ProjectRole.Basic.SUBMITTER | USER     | true
        "enabled"                | false    | ProjectRole.Basic.SUBMITTER | USER     | true

        //OPERATOR false -> (desired)true, Role Submitter

        "accessToFiles"          | false    | ProjectRole.Basic.SUBMITTER | OPERATOR | true
        "manageUsers"            | false    | ProjectRole.Basic.SUBMITTER | OPERATOR | true
        "manageUsersAndDelegate" | false    | ProjectRole.Basic.SUBMITTER | OPERATOR | true
        "receivesNotifications"  | false    | ProjectRole.Basic.SUBMITTER | OPERATOR | true
        "enabled"                | false    | ProjectRole.Basic.SUBMITTER | OPERATOR | true

        // ADMIN false -> (desired)true, Role Submitter
        "accessToFiles"          | false    | ProjectRole.Basic.SUBMITTER | ADMIN    | true
        "manageUsers"            | false    | ProjectRole.Basic.SUBMITTER | ADMIN    | true
        "manageUsersAndDelegate" | false    | ProjectRole.Basic.SUBMITTER | ADMIN    | true
        "receivesNotifications"  | false    | ProjectRole.Basic.SUBMITTER | ADMIN    | true
        "enabled"                | false    | ProjectRole.Basic.SUBMITTER | ADMIN    | true

        //USER true -> (desired)false, Role Submitter
        "accessToFiles"          | true     | ProjectRole.Basic.SUBMITTER | USER     | false
        "manageUsers"            | true     | ProjectRole.Basic.SUBMITTER | USER     | false
        "manageUsersAndDelegate" | true     | ProjectRole.Basic.SUBMITTER | USER     | false
        "receivesNotifications"  | true     | ProjectRole.Basic.SUBMITTER | USER     | false
        "enabled"                | true     | ProjectRole.Basic.SUBMITTER | USER     | false

        //OPERATOR true -> (desired)false, Role Submitter
        "accessToFiles"          | true     | ProjectRole.Basic.SUBMITTER | OPERATOR | false
        "manageUsers"            | true     | ProjectRole.Basic.SUBMITTER | OPERATOR | false
        "manageUsersAndDelegate" | true     | ProjectRole.Basic.SUBMITTER | OPERATOR | false
        "receivesNotifications"  | true     | ProjectRole.Basic.SUBMITTER | OPERATOR | false
        "enabled"                | true     | ProjectRole.Basic.SUBMITTER | OPERATOR | false

        // ADMIN true -> (desired)false, Role Submitter
        "accessToFiles"          | true     | ProjectRole.Basic.SUBMITTER | ADMIN    | false
        "manageUsers"            | true     | ProjectRole.Basic.SUBMITTER | ADMIN    | false
        "manageUsersAndDelegate" | true     | ProjectRole.Basic.SUBMITTER | ADMIN    | false
        "receivesNotifications"  | true     | ProjectRole.Basic.SUBMITTER | ADMIN    | false
        "enabled"                | true     | ProjectRole.Basic.SUBMITTER | ADMIN    | false
    }

    @Unroll
    void "test #flag access as project user with #role rights; userProjectRole contains ProjectRole #projectRole; initialized with #flagInit; expect failure "() {
        given:
        setupData()

        User user = getUser(role)
        UserProjectRole userProjectRole = createUserProjectRole(
                project: createProject(unixGroup: UNIX_GROUP),
                (flag): flagInit,
                projectRoles: ProjectRole.findAllByName(projectRole.name())
        )

        // mock user currently logged in
        userProjectRoleService.securityService = Mock(SecurityService) {
            getCurrentUser() >> user
        }

        when:
        doWithAuth(user.username) {
            userProjectRoleService."set${flag.capitalize()}"(userProjectRole, !flagInit)
        }

        then:
        thrown(InsufficientRightsException)

        where:
        flag      | flagInit | projectRole          | role
        //USER false -> (desired)true, Role PI
        "enabled" | false    | ProjectRole.Basic.PI | USER
        //USER true -> (desired)false, Role PI
        "enabled" | true     | ProjectRole.Basic.PI | USER
    }

    @Unroll
    void "test getEmailsOfToBeNotifiedProjectUsers with receivesNotifications #receivesNotifications roleEnabled #roleEnabled userEnabled #userEnabled result #result"() {
        given:
        setupData()

        List<String> output
        Project project = createProject()
        createUserProjectRole(
                user: createUser([
                        enabled: userEnabled,
                        email  : EMAIL_INTERN
                ]),
                project: project,
                receivesNotifications: receivesNotifications,
                enabled: roleEnabled,
        )

        when:
        output = doWithAuth(OPERATOR) {
            userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([project])
        }

        then:
        output == result

        where:
        receivesNotifications | roleEnabled | userEnabled || result
        true                  | true        | true        || [EMAIL_INTERN]
        true                  | true        | false       || []
        true                  | false       | true        || []
        true                  | false       | false       || []
        false                 | true        | true        || []
        false                 | true        | false       || []
        false                 | false       | true        || []
        false                 | false       | false       || []
    }

    @Unroll
    void "test getNumberOfValidUsersForProjects for given date"() {
        given:
        setupData()

        Instant baseDate = LocalDate.of(2022, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
        Date startDate = startDateOffset == null ? null : Date.from(baseDate.minus(startDateOffset, ChronoUnit.DAYS))
        Date endDate = endDateOffset == null ? null : Date.from(baseDate.minus(endDateOffset, ChronoUnit.DAYS))

        UserProjectRole userProjectRole

        autoTimestampEventListener.withoutDateCreated(User) {
            User user = createUser(dateCreated: Date.from(baseDate.minus(1, ChronoUnit.DAYS)))
            userProjectRole = createUserProjectRole(user: user)
        }

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
        createUserProjectRole(manageUsers: true)

        Project project = createProject()
        Closure<UserProjectRole> addUserProjectRole = { Map properties = [:] ->
            return createUserProjectRole([
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
        createUserProjectRole(projectRoles: [CollectionUtils.exactlyOneElement(ProjectRole.findAllByName(ProjectRole.AUTHORITY_PROJECT_ROLES.first()))])

        Project project = createProject()
        Closure<UserProjectRole> addUserProjectRole = { Map properties = [:] ->
            return createUserProjectRole([
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
        [bioinformatician, submitter, createProjectRole()].each { ProjectRole projectRole ->
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
            createUserProjectRole(projectRoles: [pr])
        }

        Project project = createProject()
        Closure<UserProjectRole> addUserProjectRole = { Map properties = [:] ->
            return createUserProjectRole([
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
        [pi, submitter, createProjectRole()].each { ProjectRole projectRole ->
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
        UserProjectRole userProjectRoleA = createUserProjectRole(project: projectA)
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
                createUser(),
                createUser(),
                createUser(),
        ]

        Project projectA = createProject()
        createUserProjectRole(project: projectA, user: users[0], enabled: false)
        createUserProjectRole(project: projectA, user: users[1], manageUsers: true)
        createUserProjectRole(project: projectA, user: users[2], accessToOtp: false)

        Project projectB = createProject()
        createUserProjectRole(project: projectB, user: users[0], enabled: false)
        createUserProjectRole(project: projectB, user: users[1], manageUsers: true)

        when:
        doWithAuth(OPERATOR) {
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
        UserProjectRole userProjectRole = createUserProjectRole()

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
                name: ProcessingOption.OptionName.AD_GROUP_ADD_USER_SNIPPET,
                value: "script.sh add \${unixGroup} \${some_other_property}",
        )

        when:
        userProjectRoleService.commandTemplate(createUserProjectRole(), UserProjectRoleService.OperatorAction.ADD)

        then:
        thrown(MissingPropertyException)
    }

    UserProjectRole createProjectUserForNotificationTest(Project project, boolean receivesNotifications, boolean userEnabled, boolean projectUserEnabled) {
        return createUserProjectRole(
                project: project ?: createProject(),
                user: createUser(enabled: userEnabled),
                receivesNotifications: receivesNotifications,
                enabled: projectUserEnabled,
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

    void "getEmailsOfToBeNotifiedProjectUsers, returns empty string for project without users"() {
        given:
        setupData()

        expect:
        userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([createProject()]) == []
    }

    void "getProjectUsersToBeNotified, only fully enabled project users with notification true of given project"() {
        given:
        setupData()

        setupProjectUserForAllNotificationCombinations()

        Project project = createProject()
        List<UserProjectRole> projectUsers = setupProjectUserForAllNotificationCombinations(project)

        List<UserProjectRole> expected = projectUsers.findAll { UserProjectRole projectUser ->
            projectUser.enabled && projectUser.user.enabled && projectUser.receivesNotifications
        }

        when:
        List<UserProjectRole> projectUsersToBeNotified = userProjectRoleService.getProjectUsersToBeNotified([project])

        then:
        TestCase.assertContainSame(projectUsersToBeNotified, expected)
    }

    void "getProjectUsersToBeNotified, on project without users, returns empty list"() {
        given:
        setupData()

        expect:
        userProjectRoleService.getProjectUsersToBeNotified([createProject()]) == []
    }

    void "projectsAssociatedToProjectAuthority, returns all projects associated to project Authorities inside a users project"() {
        given:
        setupData()
        User user = createUser()
        User projectAuthority1 = createUser()
        User projectAuthority2 = createUser()
        List<Project> projects = [createProject(), createProject(), createProject()]
        ProjectRole projectRolePI = CollectionUtils.atMostOneElement(ProjectRole.findAllByName(ProjectRole.Basic.PI.name()))
        projects.each { project -> createUserProjectRole([project: project, user: user]) }
        createUserProjectRole([
                project     : projects[0],
                projectRoles: [projectRolePI],
                user        : projectAuthority1,
        ])
        createUserProjectRole([
                project     : projects[1],
                projectRoles: [projectRolePI],
                user        : projectAuthority1,
        ])
        createUserProjectRole([
                project     : projects[1],
                projectRoles: [projectRolePI],
                user        : projectAuthority2,
        ])
        createUserProjectRole([
                project     : projects[2],
                projectRoles: [projectRolePI],
                user        : projectAuthority2,
        ])

        expect:
        userProjectRoleService.projectsAssociatedToProjectAuthority(user) == [
                (projectAuthority1): [projects[0], projects[1]],
                (projectAuthority2): [projects[1], projects[2]],
        ]
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

            _ * getMessageInternal("projectUser.notification.userDeactivated.subject", [], _) >>
                    '''deactivateUserIn
                    |${project}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.userDeactivated.body", [], _) >>
                    '''BodyWith
                    |${user}
                    |${project}
                    |${supportTeamSalutation}'''.stripMargin()

            _ * getMessageInternal("projectUser.notification.newProjectMember.body.userManagerAddedMember", [], _) >>
                    '''newProjectMember
                    |${executingUser}
                    |${userIdentifier}
                    |${projectRole}
                    |${projectName}'''.stripMargin()

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
                    |${linkProjectDirectory}
                    |${supportTeamSalutation}'''.stripMargin()
        }
    }
}
