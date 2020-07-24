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
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

import static de.dkfz.tbi.otp.project.ProjectRequest.Status.*

@Rollback
@Integration
class ProjectRequestServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    ProjectRequestService getServiceWithMockedCurrentUser(User user = DomainFactory.createUser()) {
        return new ProjectRequestService(
                securityService: Mock(SecurityService) {
                    getCurrentUserAsUser() >> user
                },
                processingOptionService: new ProcessingOptionService(),
        )
    }

    @Unroll
    void "test get, when current user is #user, return project request"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()

        ProjectRequestService service = getServiceWithMockedCurrentUser((user == "pi") ? request.pi : request.requester)

        expect:
        request == service.get(request.id)

        where:
        user << ["pi", "requester"]
    }

    void "test get, when current user is neither PI nor requester, return null"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestService service = serviceWithMockedCurrentUser

        expect:
        !service.get(request.id)
    }

    private static List<ProjectRequest> getProjectRequestsForStatus(List<ProjectRequest.Status> status, Map properties = [:]) {
        return status.collect { ProjectRequest.Status stat ->
            Map situational = (stat == PROJECT_CREATED) ? [project: DomainFactory.createProject()] : [:]
            return DomainFactory.createProjectRequest([status: stat] + situational + properties)
        }
    }

    private static Map<String, List<ProjectRequest>> setupRequestsOfEachType(User user) {
        List<ProjectRequest.Status> open = [WAITING_FOR_PI]
        List<ProjectRequest.Status> resolved = [APPROVED_BY_PI_WAITING_FOR_OPERATOR, DENIED_BY_PI, DENIED_BY_OPERATOR, PROJECT_CREATED]
        getProjectRequestsForStatus(open + resolved)
        return [
                waitingForCurrentUser   : getProjectRequestsForStatus(open, [pi: user]),
                unresolvedRequestsOfUser: getProjectRequestsForStatus(open, [requester: user]),
                createdByUserAndResolved: getProjectRequestsForStatus(resolved, [requester: user]),
                resolvedWithUserAsPi    : getProjectRequestsForStatus(resolved, [pi: user]),
        ]
    }

    void "test getWaitingForCurrentUser"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequestService service = getServiceWithMockedCurrentUser(user)
        List<ProjectRequest> expected = setupRequestsOfEachType(user)["waitingForCurrentUser"]

        expect:
        TestCase.assertContainSame(service.waitingForCurrentUser, expected)
    }

    void "test getUnresolvedRequestsOfUser"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequestService service = getServiceWithMockedCurrentUser(user)
        List<ProjectRequest> expected = setupRequestsOfEachType(user)["unresolvedRequestsOfUser"]

        expect:
        TestCase.assertContainSame(service.unresolvedRequestsOfUser, expected)
    }

    void "test getCreatedByUserAndResolved"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequestService service = getServiceWithMockedCurrentUser(user)
        List<ProjectRequest> expected = setupRequestsOfEachType(user)["createdByUserAndResolved"]

        expect:
        TestCase.assertContainSame(service.createdByUserAndResolved, expected)
    }

    void "test getResolvedWithUserAsPi"() {
        given:
        User user = DomainFactory.createUser()
        ProjectRequestService service = getServiceWithMockedCurrentUser(user)
        List<ProjectRequest> expected = setupRequestsOfEachType(user)["resolvedWithUserAsPi"]

        expect:
        TestCase.assertContainSame(service.resolvedWithUserAsPi, expected)
    }

    void "test create"() {
        given:
        User pi = DomainFactory.createUser()
        User bioinformatician = DomainFactory.createUser()
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION,
                value: "service@mail.com",
        ])
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(
                name: "name",
                description: "description",
                organizationalUnit: "ou",
                projectType: Project.ProjectType.SEQUENCING,
                pi: pi.username,
                bioinformaticians: [bioinformatician.username],
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
        service.userProjectRoleService = Mock(UserProjectRoleService) {
            1 * createUserWithLdapData(pi.username) >> pi
            1 * createUserWithLdapData(bioinformatician.username) >> bioinformatician
        }

        when:
        service.create(cmd)

        then:
        noExceptionThrown()
    }

    void "test create, fails if user does not exist"() {
        given:
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(
                name: "name",
                description: "description",
                organizationalUnit: "ou",
                projectType: Project.ProjectType.SEQUENCING,
                pi: DomainFactory.createUser().username,
                bioinformaticians: ["non-existent"],
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
        service.userProjectRoleService = Mock(UserProjectRoleService) {
            1 * createUserWithLdapData(_) >> { throw new LdapUserCreationException("") }
        }

        when:
        service.create(cmd)

        then:
        thrown(LdapUserCreationException)
        ProjectRequest.all.empty
    }

    void "test update, when project is created"() {
        given:
        ProjectRequestService service = new ProjectRequestService()
        ProjectRequest request = DomainFactory.createProjectRequest()
        Project project = DomainFactory.createProject()

        when:
        service.update(request, project)

        then:
        request.project == project
        request.status == PROJECT_CREATED
    }

    @Unroll
    void "test approveRequest, when PI approves project request, fails"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest(status: WAITING_FOR_PI)
        ProjectRequestService service = Spy(ProjectRequestService) {
            0 * sendEmailOnApproval(_) >> null
        }
        service.securityService = Mock(SecurityService) {
            getCurrentUserAsUser() >> request.pi
        }

        when:
        Errors e = service.approveRequest(request, confirmConsent, confirmRecord)

        then:
        e == null
        thrown(RuntimeException)
        request.status == WAITING_FOR_PI

        where:
        confirmConsent | confirmRecord
        false          | false
        true           | false
        false          | true
    }

    @Unroll
    void "test denyRequest, when PI denies project request, fails"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest(
                status: currentStatus,
                project: currentStatus == PROJECT_CREATED ? DomainFactory.createProject() : null,
        )
        ProjectRequestService service = Spy(ProjectRequestService) {
            0 * sendEmailOnApproval(_) >> null
        }
        service.securityService = Mock(SecurityService) {
            getCurrentUserAsUser() >> { isPi ? request.pi : DomainFactory.createUser() }
        }

        when:
        Errors e = service.denyRequest(request)

        then:
        e == null
        thrown(AccessDeniedException)
        request.status == expectedStatus

        where:
        currentStatus                       | isPi  || expectedStatus
        APPROVED_BY_PI_WAITING_FOR_OPERATOR | true  || APPROVED_BY_PI_WAITING_FOR_OPERATOR
        PROJECT_CREATED                     | true  || PROJECT_CREATED
        WAITING_FOR_PI                      | false || WAITING_FOR_PI
    }

    void "test approveRequest, when PI approves project request"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest(status: WAITING_FOR_PI)
        ProjectRequestService service = Spy(ProjectRequestService) {
            1 * sendEmailOnApproval(_) >> null
        }
        service.securityService = Mock(SecurityService) {
            getCurrentUserAsUser() >> request.pi
        }

        when:
        Errors e = service.approveRequest(request, true, true)

        then:
        e == null
        noExceptionThrown()
        request.status == APPROVED_BY_PI_WAITING_FOR_OPERATOR
    }

    void "test denyRequest, when PI denies project request"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest(status: WAITING_FOR_PI)
        ProjectRequestService service = Spy(ProjectRequestService) {
            0 * sendEmailOnApproval(_) >> null
        }
        service.securityService = Mock(SecurityService) {
            getCurrentUserAsUser() >> request.pi
        }

        when:
        Errors e = service.denyRequest(request)

        then:
        e == null
        noExceptionThrown()
        request.status == DENIED_BY_PI
    }

    void "assertApprovalEligible, all negative cases"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest(status: status)
        User currentUser = isPi ? request.pi : DomainFactory.createUser()
        ProjectRequestService service = new ProjectRequestService(
                securityService: Mock(SecurityService) {
                    getCurrentUserAsUser() >> currentUser
                }
        )

        when:
        service.assertApprovalEligible(request)

        then:
        AccessDeniedException e = thrown()
        e.message ==~ /User '.*${currentUser.username}.*' not eligible to approve this request/

        where:
        isPi  | status
        false | DENIED_BY_OPERATOR
        false | WAITING_FOR_PI
        true  | DENIED_BY_OPERATOR
    }

    void "assertApprovalEligible, positive case"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest(status: WAITING_FOR_PI)
        ProjectRequestService service = new ProjectRequestService(
                securityService: Mock(SecurityService) {
                    getCurrentUserAsUser() >> request.pi
                }
        )

        when:
        service.assertApprovalEligible(request)

        then:
        noExceptionThrown()
    }

    void "assertTermsAndConditions, all negative cases"() {
        given:
        ProjectRequestService projectRequestService = new ProjectRequestService()

        when:
        projectRequestService.assertTermsAndConditions(confirmConsent, confirmRecordOfProcessingActivities)

        then:
        OtpRuntimeException e = thrown()
        e.message ==~ /Invalid state, conditions were not accepted/

        where:
        confirmConsent | confirmRecordOfProcessingActivities
        false          | false
        false          | true
        true           | false
    }

    void "assertTermsAndConditions, positive case"() {
        given:
        ProjectRequestService projectRequestService = new ProjectRequestService()

        when:
        projectRequestService.assertTermsAndConditions(true, true)

        then:
        noExceptionThrown()
    }

    void "addUserRolesAndPermissions, create correct userProjectRoles"() {
        given:
        ProjectRequestService projectRequestService = new ProjectRequestService(
                userProjectRoleService: Mock(UserProjectRoleService)
        )

        createAllBasicProjectRoles()

        Project project = createProject()

        User userPi = DomainFactory.createUser()
        User userSubmitterAndBioinf = DomainFactory.createUser()
        User userSubmitter2 = DomainFactory.createUser()

        ProjectRequest request = DomainFactory.createProjectRequest([
                pi: userPi,
                bioinformaticians: [userSubmitterAndBioinf],
                submitters: [userSubmitterAndBioinf, userSubmitter2],
                status: PROJECT_CREATED,
                project: project,
        ])

        Set<ProjectRole> pi = ProjectRole.findAllByNameInList([ProjectRole.Basic.PI]) as Set<ProjectRole>
        Set<ProjectRole> submitter_bioinformatician = ProjectRole.findAllByNameInList([
                ProjectRole.Basic.BIOINFORMATICIAN, ProjectRole.Basic.SUBMITTER,
        ]) as Set<ProjectRole>
        Set<ProjectRole> submitter = ProjectRole.findAllByNameInList([ProjectRole.Basic.SUBMITTER]) as Set<ProjectRole>

        when:
        projectRequestService.addUserRolesAndPermissions(request)

        then:
        1 * projectRequestService.userProjectRoleService.createUserProjectRole(userPi,
                project,
                pi,
        )
        1 * projectRequestService.userProjectRoleService.createUserProjectRole(userSubmitterAndBioinf,
                project,
                submitter_bioinformatician,
        )
        1 * projectRequestService.userProjectRoleService.createUserProjectRole(userSubmitter2,
                project,
                submitter,
        )
    }
}
