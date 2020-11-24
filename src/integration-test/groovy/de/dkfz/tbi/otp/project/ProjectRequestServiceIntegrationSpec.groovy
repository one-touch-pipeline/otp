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

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import grails.web.mapping.LinkGenerator
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

import static de.dkfz.tbi.otp.project.ProjectRequest.Status.*

@Rollback
@Integration
class ProjectRequestServiceIntegrationSpec extends Specification implements UserAndRoles, TaxonomyFactory {

    ProjectRequestService getServiceWithMockedCurrentUser(User user = DomainFactory.createUser()) {
        SecurityService mockedSecurityService = Mock(SecurityService) {
            getCurrentUserAsUser() >> user
        }
        return new ProjectRequestService(
                auditLogService          : Mock(AuditLogService),
                securityService          : mockedSecurityService,
                projectRequestUserService: new ProjectRequestUserService(
                        securityService: mockedSecurityService
                ),
                processingOptionService  : new ProcessingOptionService(),
                rolesService             : new RolesService(),
        )
    }

    void "get, when current user is requester, return project request"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestService service = getServiceWithMockedCurrentUser(request.requester)

        expect:
        request == service.get(request.id)
    }

    void "get, when current user is related project request user, return project request"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestService service = getServiceWithMockedCurrentUser(request.users.first().user)

        expect:
        request == service.get(request.id)
    }

    void "get, when current user is not part of request but is an admin, return project request"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        createUserAndRoles()
        ProjectRequestService service = getServiceWithMockedCurrentUser(getUser(ADMIN))

        expect:
        request == service.get(request.id)
    }

    void "get, when current user is not part of request and not an admin, return null"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestService service = serviceWithMockedCurrentUser

        expect:
        !service.get(request.id)
    }

    void "getApproversOfProjectRequest, request with users in all states"() {
        given:
        ProjectRequest request = createProjectRequestWithUsersInState(ProjectRequestUser.ApprovalState.values() as List<ProjectRequestUser.ApprovalState>)

        ProjectRequestService service = new ProjectRequestService()

        when:
        List<ProjectRequestUser> result = service.getApproversOfProjectRequest(request)

        then:
        TestCase.assertContainSame(result, request.users.findAll { it.approvalState != ProjectRequestUser.ApprovalState.NOT_APPLICABLE })
    }

    void "isUserPartOfRequest, all cases"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequest request = DomainFactory.createProjectRequest([
                requester: asRequester ? user : DomainFactory.createUser(),
        ], [
                user     : asApprover ? user : DomainFactory.createUser(),
        ])
        ProjectRequestService service = new ProjectRequestService()

        expect:
        service.isUserPartOfRequest(user, request) == expected

        where:
        asRequester | asApprover || expected
        false       | false      || false
        false       | true       || true
        true        | false      || true
        true        | true       || true
    }

    private static ProjectRequest createProjectRequestHelper(User requester, User requestUser, ProjectRequest.Status status) {
        return DomainFactory.createProjectRequest([
                requester: requester,
                status   : status,
        ], [
                user     : requestUser,
        ])
    }

    private static Map<String, List<ProjectRequest>> setupRequestsOfEachType(User currentUser) {
        User otherUser = DomainFactory.createUser()

        Map<String, List<ProjectRequest>> resultMap = [:].withDefault { [] }
        ProjectRequest.Status.values().each { ProjectRequest.Status status ->
            resultMap[(status.resolvedStatus ? "resolved" : "unresolved")].addAll([
                    createProjectRequestHelper(currentUser, otherUser, status),
                    createProjectRequestHelper(otherUser, currentUser, status),
            ])
            createProjectRequestHelper(otherUser, otherUser, status)
        }

        return resultMap
    }

    void "getUnresolvedRequestsOfUser"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequestService service = getServiceWithMockedCurrentUser(user)
        List<ProjectRequest> expected = setupRequestsOfEachType(user)["unresolved"]

        expect:
        TestCase.assertContainSame(service.unresolvedRequestsOfUser, expected)
    }

    void "getResolvedOfCurrentUser"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequestService service = getServiceWithMockedCurrentUser(user)
        List<ProjectRequest> expected = setupRequestsOfEachType(user)["resolved"]

        expect:
        TestCase.assertContainSame(service.resolvedOfCurrentUser, expected)
    }

    void "create, typical usecase"() {
        given:
        createAllBasicProjectRoles()

        User user1 = DomainFactory.createUser()
        User user2 = DomainFactory.createUser()
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION,
                value: "service@mail.com",
        ])
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(
                name              : "name",
                description       : "description",
                organizationalUnit: "ou",
                projectType       : Project.ProjectType.SEQUENCING,
                users             : [
                        null, // null objects should be disregarded
                        new ProjectRequestUserCommand(
                                username     : user1.username,
                                projectRoles : [pi],
                                accessToFiles: true,
                                manageUsers  : true,
                        ),
                        new ProjectRequestUserCommand(
                                username     : user2.username,
                                projectRoles : [other],
                                accessToFiles: false,
                                manageUsers  : false,
                        ),
                ],
        )

        ProjectRequestService service = serviceWithMockedCurrentUser
        service.linkGenerator = Mock(LinkGenerator) {
            1 * link(_) >> 'link'
        }
        service.messageSourceService = Mock(MessageSourceService) {
            2 * createMessage(_, _) >> "message"
        }
        service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _, _) >> null
        }
        service.projectRequestUserService = new ProjectRequestUserService(
                userProjectRoleService: Mock(UserProjectRoleService) {
                    1 * createUserWithLdapData(user1.username) >> user1
                    1 * createUserWithLdapData(user2.username) >> user2
                }
        )

        when:
        service.create(cmd)

        then:
        noExceptionThrown()
    }

    void "create, fails if user does not exist"() {
        given:
        createAllBasicProjectRoles()

        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(
                name              : "name",
                description       : "description",
                organizationalUnit: "ou",
                projectType       : Project.ProjectType.SEQUENCING,
                users             : [
                        new ProjectRequestUserCommand(
                                username     : "does-not-exist",
                                projectRoles : [pi],
                                accessToFiles: true,
                                manageUsers  : true,
                        ),
                ],
        )
        ProjectRequestService service = serviceWithMockedCurrentUser
        service.linkGenerator = Mock(LinkGenerator) {
            0 * link(_) >> 'link'
        }
        service.messageSourceService = Mock(MessageSourceService) {
            0 * createMessage(_, _) >> "message"
        }
        service.mailHelperService = Mock(MailHelperService) {
            0 * sendEmail(_, _, _) >> null
        }
        service.projectRequestUserService = new ProjectRequestUserService(
            userProjectRoleService: Mock(UserProjectRoleService) {
                1 * createUserWithLdapData(_) >> { throw new LdapUserCreationException("") }
            }
        )

        when:
        service.create(cmd)

        then:
        thrown(LdapUserCreationException)
        ProjectRequest.all.empty
    }

    void "setStatus, sets the status of the request and saves it"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()

        ProjectRequestService service = getServiceWithMockedCurrentUser(request.requester)

        when:
        service.setStatus(request, status)

        then:
        request.status ==  status
        !request.project

        where:
        status << ProjectRequest.Status.values() - PROJECT_CREATED
    }

    void "setStatus, for project expecting states"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        Project project = createProject()

        ProjectRequestService service = getServiceWithMockedCurrentUser(request.requester)

        when:
        service.setStatus(request, PROJECT_CREATED, project)

        then:
        request.status == PROJECT_CREATED
        request.project == project
    }

    void "setStatus, for project expecting states, without project, causes validation error"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()

        ProjectRequestService service = getServiceWithMockedCurrentUser(request.requester)

        when:
        service.setStatus(request, PROJECT_CREATED)

        then:
        AssertionError e = thrown()
        e.message =~ "Status expects a project, but none is given"
    }

    void "finish, when project is created"() {
        given:
        ProjectRequestService service = new ProjectRequestService()
        ProjectRequest request = DomainFactory.createProjectRequest()
        Project project = DomainFactory.createProject()

        when:
        service.finish(request, project)

        then:
        request.project == project
        request.status == PROJECT_CREATED
    }

    @Unroll
    void "allProjectRequestUsersInState, #testCase"() {
        given:
        ProjectRequest request = createProjectRequestWithUsersInState(states + ProjectRequestUser.ApprovalState.NOT_APPLICABLE)

        ProjectRequestService service = new ProjectRequestService()

        expect:
        service.allProjectRequestUsersInState(request, ProjectRequestUser.ApprovalState.APPROVED) == expected

        where:
        states                                                                                 ||  expected | testCase
        [ProjectRequestUser.ApprovalState.PENDING, ProjectRequestUser.ApprovalState.APPROVED]  ||  false    | "some in expected state"
        [ProjectRequestUser.ApprovalState.DENIED, ProjectRequestUser.ApprovalState.DENIED]     ||  false    | "all in common state but not expected state"
        [ProjectRequestUser.ApprovalState.APPROVED, ProjectRequestUser.ApprovalState.APPROVED] ||  true     | "all in expected state"
    }

    ProjectRequest createProjectRequestWithUsersInState(List<ProjectRequestUser.ApprovalState> states) {
        createAllBasicProjectRoles()
        return DomainFactory.createProjectRequest([
                users: states.collect { ProjectRequestUser.ApprovalState state ->
                    DomainFactory.createProjectRequestUser(
                            approvalState: state,
                            projectRoles : [pi],
                    )
                }
        ])
    }

    @Unroll
    void "approveRequest, fails without confirmed checkboxes"() {
        given:
        ProjectRequest request = createProjectRequestWithUsersInState([
                ProjectRequestUser.ApprovalState.PENDING,
        ])
        ProjectRequestUser user = request.users.find { it.approvalState == ProjectRequestUser.ApprovalState.PENDING }

        ProjectRequestService service = getServiceWithMockedCurrentUser(user.user)
        service.mailHelperService = Mock(MailHelperService) {
            0 * sendEmail(_, _, _) >> null
        }

        when:
        Errors e = service.approveRequest(request, confirmConsent, confirmRecord)

        then:
        e == null
        thrown(RuntimeException)
        user.approvalState == ProjectRequestUser.ApprovalState.PENDING
        request.status == WAITING_FOR_APPROVER

        where:
        confirmConsent | confirmRecord
        false          | false
        true           | false
        false          | true
    }

    void "approveRequest, user approves -> all approved, request is approved"() {
        given:
        ProjectRequest request = createProjectRequestWithUsersInState([
                ProjectRequestUser.ApprovalState.PENDING,
                ProjectRequestUser.ApprovalState.APPROVED,
                ProjectRequestUser.ApprovalState.NOT_APPLICABLE,
        ])
        ProjectRequestUser pendingUser = request.users.find { it.approvalState == ProjectRequestUser.ApprovalState.PENDING }

        ProjectRequestService service = getServiceWithMockedCurrentUser(pendingUser.user)
        service.linkGenerator = Mock(LinkGenerator) {
            1 * link(_) >> 'link'
        }
        service.messageSourceService = Mock(MessageSourceService) {
            2 * createMessage(_, _) >> "message"
        }
        service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _) >> null
        }

        when:
        service.approveRequest(request, true, true)

        then:
        request.status == WAITING_FOR_OPERATOR
        pendingUser.approvalState == ProjectRequestUser.ApprovalState.APPROVED
    }

    void "denyRequest, user denies -> all denied, request is denied"() {
        given:
        ProjectRequest request = createProjectRequestWithUsersInState([
                ProjectRequestUser.ApprovalState.PENDING,
                ProjectRequestUser.ApprovalState.DENIED,
                ProjectRequestUser.ApprovalState.NOT_APPLICABLE,
        ])
        ProjectRequestUser pendingUser = request.users.find { it.approvalState == ProjectRequestUser.ApprovalState.PENDING }

        ProjectRequestService service = getServiceWithMockedCurrentUser(pendingUser.user)

        when:
        service.denyRequest(request)

        then:
        request.status == DENIED_BY_APPROVER
        pendingUser.approvalState == ProjectRequestUser.ApprovalState.DENIED
    }

    void "approveRequest and denyRequest, user approves and denies while other approvals are still pending, request unchanged"() {
        given:
        ProjectRequest request = createProjectRequestWithUsersInState([
                ProjectRequestUser.ApprovalState.PENDING,
                ProjectRequestUser.ApprovalState.PENDING,
                ProjectRequestUser.ApprovalState.NOT_APPLICABLE,
        ])
        ProjectRequestUser pendingUser = request.users.find { it.approvalState == ProjectRequestUser.ApprovalState.PENDING }

        ProjectRequestService service = getServiceWithMockedCurrentUser(pendingUser.user)

        when: "test approve"
        service.approveRequest(request, true, true)

        then:
        request.status == WAITING_FOR_APPROVER
        pendingUser.approvalState == ProjectRequestUser.ApprovalState.APPROVED

        when: "test deny"
        service.denyRequest(request)

        then:
        request.status == WAITING_FOR_APPROVER
        pendingUser.approvalState == ProjectRequestUser.ApprovalState.DENIED
    }

    void "closeRequest, sets status to closed and nothing more"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()

        ProjectRequestService service = getServiceWithMockedCurrentUser(request.requester)

        when:
        service.closeRequest(request)

        then:
        request.status == CLOSED
    }

    void "closeRequest, not allowed if the user is not the requester"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()

        User approver = request.users.first().user
        ProjectRequestService service = getServiceWithMockedCurrentUser(approver)

        when:
        service.closeRequest(request)

        then:
        AccessDeniedException e = thrown()
        e.message ==~ /User '.*${approver.username}.*' is not eligible to close this request/
    }

    void "isUserEligibleToClose, test for multiple kinds of users"() {
        given:
        ProjectRequest projectRequest = DomainFactory.createProjectRequest()
        User unrelatedUser = DomainFactory.createUser()

        ProjectRequestService service = new ProjectRequestService()

        expect:
        service.isUserEligibleToClose(projectRequest.requester, projectRequest)
        !service.isUserEligibleToClose(projectRequest.users.first().user, projectRequest)
        !service.isUserEligibleToClose(unrelatedUser, projectRequest)
    }

    void "ensureEligibleToClose, all uneditable states"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequest request = DomainFactory.createProjectRequest([
                requester: user,
                status   : status,
        ])

        ProjectRequestService service = getServiceWithMockedCurrentUser(user)

        when:
        service.ensureEligibleToClose(request)

        then:
        AccessDeniedException e = thrown()
        e.message ==~ /User '.*${user.username}.*' is not eligible to close this request/

        where:
        status << [WAITING_FOR_OPERATOR, PROJECT_CREATED, CLOSED]
    }

    void "ensureEligibleToClose, positive case"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest([
                status   : WAITING_FOR_APPROVER,
        ])

        ProjectRequestService service = getServiceWithMockedCurrentUser(request.requester)

        when:
        service.ensureEligibleToClose(request)

        then:
        noExceptionThrown()
    }

    void "isUserEligibleApproverForRequest, all states"() {
        given:
        ProjectRequest projectRequest = DomainFactory.createProjectRequest()
        ProjectRequestUser projectRequestUser = DomainFactory.createProjectRequestUser(approvalState: state)
        projectRequest.addToUsers(projectRequestUser)

        ProjectRequestService service = new ProjectRequestService()

        expect:
        service.isUserEligibleApproverForRequest(projectRequestUser.user, projectRequest) == expected

        where:
        state                                           || expected
        ProjectRequestUser.ApprovalState.APPROVED       || true
        ProjectRequestUser.ApprovalState.DENIED         || true
        ProjectRequestUser.ApprovalState.PENDING        || true
        ProjectRequestUser.ApprovalState.NOT_APPLICABLE || false
    }

    void "ensureApprovalEligible, all uneditable states"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequest request = DomainFactory.createProjectRequest([
                requester: user,
                status   : status,
                project  : status == PROJECT_CREATED ? createProject() : null,
        ], [
                user     : user,
        ])

        ProjectRequestService service = getServiceWithMockedCurrentUser(user)

        when:
        service.ensureApprovalEligible(request)

        then:
        AccessDeniedException e = thrown()
        e.message ==~ /User '.*${user.username}.*' is not eligible to approve this request/

        where:
        status << [WAITING_FOR_OPERATOR, PROJECT_CREATED, CLOSED]
    }

    void "ensureApprovalEligible, unrelated user"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequest request = DomainFactory.createProjectRequest(
                status   : WAITING_FOR_APPROVER,
                requester: isRequester ? user : DomainFactory.createUser()
        )

        ProjectRequestService service = getServiceWithMockedCurrentUser(user)

        when:
        service.ensureApprovalEligible(request)

        then:
        AccessDeniedException e = thrown()
        e.message ==~ /User '.*${user.username}.*' is not eligible to approve this request/

        where:
        isRequester << [false, true]
    }

    void "ensureApprovalEligible, positive case"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequest request = DomainFactory.createProjectRequest([
                requester: isRequester ? user : DomainFactory.createUser(),
                status   : WAITING_FOR_APPROVER,
        ], [
                user     : isApprover ? user : DomainFactory.createUser(),
        ])

        ProjectRequestService service = getServiceWithMockedCurrentUser(user)

        when:
        service.ensureApprovalEligible(request)

        then:
        noExceptionThrown()

        where:
        isRequester | isApprover
        false       | true
        true        | true
    }

    void "ensureTermsAndConditions, all negative cases"() {
        given:
        ProjectRequestService projectRequestService = new ProjectRequestService()

        when:
        projectRequestService.ensureTermsAndConditions(confirmConsent, confirmRecordOfProcessingActivities)

        then:
        OtpRuntimeException e = thrown()
        e.message ==~ /Invalid state, conditions were not accepted/

        where:
        confirmConsent | confirmRecordOfProcessingActivities
        false          | false
        false          | true
        true           | false
    }

    void "ensureTermsAndConditions, positive case"() {
        given:
        ProjectRequestService projectRequestService = new ProjectRequestService()

        when:
        projectRequestService.ensureTermsAndConditions(true, true)

        then:
        noExceptionThrown()
    }

    void "addProjectRequestUsersToProject, create correct userProjectRoles"() {
        given:
        UserProjectRoleService userProjectRoleService = Mock(UserProjectRoleService)
        ProjectRequestService projectRequestService = new ProjectRequestService(
                userProjectRoleService   : userProjectRoleService,
                projectRequestUserService: new ProjectRequestUserService(
                        userProjectRoleService: userProjectRoleService,
                )
        )

        createAllBasicProjectRoles()

        Project project = createProject()

        User user1 = DomainFactory.createUser()
        User user2 = DomainFactory.createUser()
        User user3 = DomainFactory.createUser()

        ProjectRequest request = DomainFactory.createProjectRequest([
                status           : PROJECT_CREATED,
                project          : project,
                users            : [
                        DomainFactory.createProjectRequestUser([
                                user        : user1,
                                projectRoles: [pi],
                        ]),
                        DomainFactory.createProjectRequestUser([
                                user        : user2,
                                projectRoles: [coordinator],
                        ]),
                        DomainFactory.createProjectRequestUser([
                                user        : user3,
                                projectRoles: [bioinformatician, other],
                        ]),
                ],
        ])

        when:
        projectRequestService.addProjectRequestUsersToProject(request)

        then:
        1 * projectRequestService.userProjectRoleService.createUserProjectRole(user1, project, request.users[0].projectRoles)
        1 * projectRequestService.userProjectRoleService.createUserProjectRole(user2, project, request.users[1].projectRoles)
        1 * projectRequestService.userProjectRoleService.createUserProjectRole(user3, project, request.users[2].projectRoles)
    }

    void "findProjectRequestByProject, project is set"() {
        given:
        ProjectRequestService service = new ProjectRequestService()
        Project project = createProject()
        ProjectRequest projectRequest = DomainFactory.createProjectRequest(
                status : PROJECT_CREATED,
                project: project,
        )

        expect:
        service.findProjectRequestByProject(project) == projectRequest
    }

    void "findProjectRequestByProject, project without request, return null"() {
        given:
        ProjectRequestService service = new ProjectRequestService()
        Project project = createProject()

        expect:
        service.findProjectRequestByProject(project) == null
    }

    void "findProjectRequestByProject, more than one request, throws exception"() {
        given:
        ProjectRequestService service = new ProjectRequestService()
        Project project = createProject()
        2.times {
            DomainFactory.createProjectRequest(
                    status : PROJECT_CREATED,
                    project: project,
            )
        }

        when:
        service.findProjectRequestByProject(project)

        then:
        AssertionError e = thrown()
        e.message.contains("Collection contains 2 elements. Expected 1")
    }

    void "requesterIsEligibleToAccept"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequest request = DomainFactory.createProjectRequest([
                requester: expected ? user : DomainFactory.createUser(),
        ], [
                user     : expected ? user : DomainFactory.createUser(),
        ])
        ProjectRequestService service = getServiceWithMockedCurrentUser(user)

        expect:
        service.requesterIsEligibleToAccept(request) == expected

        where:
        expected << [true, false]
    }
}
