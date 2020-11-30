/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.project

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.security.authentication.AuthenticationTrustResolver
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

@Rollback
@Integration
class ProjectRequestUserServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    ProjectRequestUserService getServiceWithMockedCurrentUser(User user = DomainFactory.createUser()) {
        return new ProjectRequestUserService(
                securityService: Mock(SecurityService) {
                    getCurrentUserAsUser() >> user
                }
        )
    }

    void "createProjectRequestUser, basic use case"() {
        given:
        ProjectRequestUserService service = new ProjectRequestUserService()

        User user = DomainFactory.createUser()
        Set<ProjectRole> projectRoles = [
                DomainFactory.createProjectRole(),
                DomainFactory.createProjectRole(),
        ]
        ProjectRequestUser.ApprovalState state = ProjectRequestUser.ApprovalState.PENDING

        when:
        ProjectRequestUser pru = service.createProjectRequestUser(user, projectRoles, state, [
                accessToFiles         : true,
                manageUsers           : true,
                manageUsersAndDelegate: true,
        ])

        then:
        pru.user == user
        TestCase.assertContainSame(pru.projectRoles, projectRoles)
        pru.approvalState == state
        pru.accessToFiles
        pru.manageUsers
        pru.manageUsersAndDelegate
    }

    ProjectRequestUserCommand createProjectRequestUserCommand(Map properties = [:]) {
        return new ProjectRequestUserCommand([
                username     : DomainFactory.createUser(),
                projectRoles : [DomainFactory.createProjectRole()],
                accessToFiles: true,
                manageUsers  : true,
        ] + properties)
    }

    void "createProjectRequestUserFromCommand, translates all parameters"() {
        given:
        createAllBasicProjectRoles()

        User user = DomainFactory.createUser()
        ProjectRequestUserService service = new ProjectRequestUserService(
                userProjectRoleService: Mock(UserProjectRoleService) {
                    1 * createUserWithLdapData(user.username) >> user
                }
        )

        ProjectRequestUserCommand cmd = createProjectRequestUserCommand(
                username    : user.username,
                projectRoles: [pi, other],
        )

        when:
        ProjectRequestUser projectRequestUser = service.createProjectRequestUserFromCommand(cmd)

        then:
        projectRequestUser.user == user
        TestCase.assertContainSame(projectRequestUser.projectRoles, [pi, other])
        projectRequestUser.accessToFiles
        projectRequestUser.manageUsers
        projectRequestUser.manageUsersAndDelegate
        projectRequestUser.approvalState == ProjectRequestUser.ApprovalState.PENDING
    }

    void "createProjectRequestUserFromCommand, provides delegate permission and PENDING state to authority roles"() {
        given:
        createAllBasicProjectRoles()

        User user = DomainFactory.createUser()
        ProjectRequestUserService service = new ProjectRequestUserService(
                userProjectRoleService: Mock(UserProjectRoleService) {
                    1 * createUserWithLdapData(user.username) >> user
                }
        )

        ProjectRequestUserCommand cmd = createProjectRequestUserCommand(
                username    : user.username,
                projectRoles: roles as Set<ProjectRole>,
        )

        when:
        ProjectRequestUser projectRequestUser = service.createProjectRequestUserFromCommand(cmd)

        then:
        projectRequestUser.user == user
        TestCase.assertContainSame(projectRequestUser.projectRoles, roles as Set<ProjectRole>)
        projectRequestUser.manageUsersAndDelegate == expectedDelegate
        projectRequestUser.approvalState == expectedState

        where:
        roles                     || expectedDelegate | expectedState
        [pi]                      || true             | ProjectRequestUser.ApprovalState.PENDING
        [pi, other]               || true             | ProjectRequestUser.ApprovalState.PENDING
        [other]                   || false            | ProjectRequestUser.ApprovalState.NOT_APPLICABLE
        [other, bioinformatician] || false            | ProjectRequestUser.ApprovalState.NOT_APPLICABLE
    }

    void "createProjectRequestUsersFromCommands, filters out null objects"() {
        given:
        ProjectRequestUserService service = Spy(ProjectRequestUserService) {
            3 * createProjectRequestUserFromCommand(_) >> { DomainFactory.createProjectRequestUser() }
        }
        List<ProjectRequestUserCommand> list = [
                null,
                createProjectRequestUserCommand(),
                createProjectRequestUserCommand(),
                null,
                createProjectRequestUserCommand(),
                null,
        ]

        expect:
        service.createProjectRequestUsersFromCommands(list).size() == 3
    }

    void "ableToDelegateManagement, all cases"() {
        given:
        createAllBasicProjectRoles()
        ProjectRequestUserService service = new ProjectRequestUserService()

        expect:
        service.ableToDelegateManagement([pi] as Set<ProjectRole>)
        service.ableToDelegateManagement([pi, other] as Set<ProjectRole>)
        !service.ableToDelegateManagement([other, bioinformatician] as Set<ProjectRole>)
        !service.ableToDelegateManagement([] as Set<ProjectRole>)
    }

    void "startingStateFromRoles, all cases"() {
        given:
        createAllBasicProjectRoles()
        ProjectRequestUserService service = new ProjectRequestUserService()

        expect:
        service.startingStateFromRoles([pi] as Set<ProjectRole>) == ProjectRequestUser.ApprovalState.PENDING
        service.startingStateFromRoles([pi, other] as Set<ProjectRole>) == ProjectRequestUser.ApprovalState.PENDING
        service.startingStateFromRoles([other, bioinformatician] as Set<ProjectRole>) == ProjectRequestUser.ApprovalState.NOT_APPLICABLE
        service.startingStateFromRoles([] as Set<ProjectRole>) == ProjectRequestUser.ApprovalState.NOT_APPLICABLE
    }

    void "getProjectRequestUserOfCurrentUser, finds user"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestUser expected = request.users.first()
        ProjectRequestUserService service = getServiceWithMockedCurrentUser(expected.user)

        expect:
        service.getProjectRequestUserOfCurrentUser(request) == expected
    }

    void "getProjectRequestUserOfCurrentUser, no ProjectRequestUser for User"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestUserService service = getServiceWithMockedCurrentUser()

        when:
        service.getProjectRequestUserOfCurrentUser(request)

        then:
        AssertionError e = thrown()
        e.message =~ "Collection contains 0 elements. Expected 1"
    }

    void "setApprovalStateAsCurrentUser, all states"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestUser projectRequestUser = request.users.first()
        ProjectRequestUserService service = getServiceWithMockedCurrentUser(projectRequestUser.user)

        when:
        service.setApprovalStateAsCurrentUser(request, state)

        then:
        projectRequestUser.approvalState == state

        where:
        state << ProjectRequestUser.ApprovalState.values()
    }

    void "toUserProjectRole"() {
        given:
        ProjectRequestUserService service = new ProjectRequestUserService(
            userProjectRoleService: new UserProjectRoleService(
                    auditLogService        : Mock(AuditLogService),
                    configService          : Mock(ConfigService) {
                        _ * getRootPath() >> TestCase.uniqueNonExistentPath
                    },
                    springSecurityService  : new SpringSecurityService(
                            authenticationTrustResolver: Mock(AuthenticationTrustResolver) {
                                isAnonymous(_) >> false
                            }
                    ),
                    mailHelperService      : Mock(MailHelperService),
                    messageSourceService   : Mock(MessageSourceService),
                    processingOptionService: Mock(ProcessingOptionService) {
                        _ * findOptionAsString(_) >> "option"
                    },
                    ldapService: Mock(LdapService) {
                        isUserInLdapAndActivated(_) >> true
                    }
            )
        )
        createUserAndRoles()
        createAllBasicProjectRoles()
        Project project = createProject()
        ProjectRequestUser projectRequestUser = DomainFactory.createProjectRequestUser(
                projectRoles          : [other, pi],
                accessToFiles         : accessToFiles,
                manageUsers           : manageUsers,
                manageUsersAndDelegate: manageUsersAndDelegate,
        )

        when:
        // doWithAuth is required until the UserProjectRole service has been rewritten to use SecurityService
        UserProjectRole result = SpringSecurityUtils.doWithAuth(OPERATOR) {
            service.toUserProjectRole(project, projectRequestUser)
        }

        then:
        result.project == project
        result.user == projectRequestUser.user
        TestCase.assertContainSame(result.projectRoles, projectRequestUser.projectRoles)

        result.accessToFiles == accessToFiles
        result.manageUsers == manageUsers
        result.manageUsersAndDelegate == manageUsersAndDelegate
        result.fileAccessChangeRequested == accessToFiles

        result.accessToOtp
        result.receivesNotifications
        result.enabled

        where:
        accessToFiles | manageUsers | manageUsersAndDelegate
        true          | true        | true
        false         | false       | false
    }
}
