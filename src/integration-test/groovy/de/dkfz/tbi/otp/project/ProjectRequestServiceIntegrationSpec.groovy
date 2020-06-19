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
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

import static de.dkfz.tbi.otp.project.ProjectRequest.Status.*

@Rollback
@Integration
class ProjectRequestServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    ProjectRequestService getServiceWithMockedCurrentUser(User user = DomainFactory.createUser()) {
        return new ProjectRequestService(
                springSecurityService: Mock(SpringSecurityService) {
                    getCurrentUser() >> user
                },
                processingOptionService: new ProcessingOptionService(),
        )
    }

    void "test get, when current user is #value, return project request"() {
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
    void "test update, when PI approves/denies project request, fails"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest(
                status: currentStatus,
                project: currentStatus == PROJECT_CREATED ? DomainFactory.createProject() : null,
        )
        ProjectRequestService service = Spy(ProjectRequestService) {
            mail * sendEmailOnApproval(_) >> null
        }
        service.springSecurityService = Mock(SpringSecurityService) {
            getCurrentUser() >> { isPi ? request.pi : DomainFactory.createUser() }
        }

        when:
        Errors e = service.update(request, wantedStatus, confirmConsent, confirmRecord)

        then:
        e == null
        thrown(exc)
        request.status == expectedStatus

        where:
        currentStatus                       | isPi  | wantedStatus                        | confirmConsent | confirmRecord || mail | expectedStatus     | exc
        DENIED_BY_PI                        | true  | APPROVED_BY_PI_WAITING_FOR_OPERATOR | true           | true          || 0    | DENIED_BY_PI       | AccessDeniedException
        DENIED_BY_OPERATOR                  | true  | APPROVED_BY_PI_WAITING_FOR_OPERATOR | true           | true          || 0    | DENIED_BY_OPERATOR | AccessDeniedException
        APPROVED_BY_PI_WAITING_FOR_OPERATOR | true  | DENIED_BY_PI                        | true           | true          || 0    | APPROVED_BY_PI_WAITING_FOR_OPERATOR | AccessDeniedException
        PROJECT_CREATED                     | true  | APPROVED_BY_PI_WAITING_FOR_OPERATOR | true           | true          || 0    | PROJECT_CREATED                     | AccessDeniedException
        PROJECT_CREATED                     | true  | DENIED_BY_PI                        | true           | true          || 0    | PROJECT_CREATED                     | AccessDeniedException

        WAITING_FOR_PI                      | false | APPROVED_BY_PI_WAITING_FOR_OPERATOR | true           | true          || 0    | WAITING_FOR_PI                      | AccessDeniedException
        WAITING_FOR_PI                      | false | DENIED_BY_PI                        | true           | true          || 0    | WAITING_FOR_PI                      | AccessDeniedException

        WAITING_FOR_PI                      | true  | WAITING_FOR_PI                      | true           | true          || 0    | WAITING_FOR_PI                      | RuntimeException
        WAITING_FOR_PI                      | true  | DENIED_BY_OPERATOR                  | true           | true          || 0    | WAITING_FOR_PI                      | RuntimeException
        WAITING_FOR_PI                      | true  | PROJECT_CREATED                     | true           | true          || 0    | WAITING_FOR_PI                      | RuntimeException

        WAITING_FOR_PI                      | true  | APPROVED_BY_PI_WAITING_FOR_OPERATOR | false          | false         || 0    | WAITING_FOR_PI                      | RuntimeException
        WAITING_FOR_PI                      | true  | APPROVED_BY_PI_WAITING_FOR_OPERATOR | true           | false         || 0    | WAITING_FOR_PI                      | RuntimeException
        WAITING_FOR_PI                      | true  | APPROVED_BY_PI_WAITING_FOR_OPERATOR | false          | true          || 0    | WAITING_FOR_PI                      | RuntimeException
    }

    void "test update, when PI approves/denies project request"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest(
                status: currentStatus,
                project: currentStatus == PROJECT_CREATED ? DomainFactory.createProject() : null,
        )
        ProjectRequestService service = Spy(ProjectRequestService) {
            mail * sendEmailOnApproval(_) >> null
        }
        service.springSecurityService = Mock(SpringSecurityService) {
            getCurrentUser() >> { isPi ? request.pi : DomainFactory.createUser() }
        }

        when:
        Errors e = service.update(request, wantedStatus, confirmConsent, confirmRecord)

        then:
        e == null
        noExceptionThrown()
        request.status == expectedStatus

        where:
        currentStatus                       | isPi  | wantedStatus                        | confirmConsent | confirmRecord || mail | expectedStatus
        WAITING_FOR_PI                      | true  | APPROVED_BY_PI_WAITING_FOR_OPERATOR | true           | true          || 1    | APPROVED_BY_PI_WAITING_FOR_OPERATOR
        WAITING_FOR_PI                      | true  | DENIED_BY_PI                        | true           | true          || 0    | DENIED_BY_PI
    }
}
