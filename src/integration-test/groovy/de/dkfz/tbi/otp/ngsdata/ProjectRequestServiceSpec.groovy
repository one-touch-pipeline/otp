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
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import grails.web.mapping.LinkGenerator
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.Errors
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

import static de.dkfz.tbi.otp.ngsdata.ProjectRequest.Status.*

@Rollback
@Integration
class ProjectRequestServiceSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    void "test get, when current user is #value, return project request"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestService service = new ProjectRequestService()
        service.springSecurityService = Mock(SpringSecurityService) {
            getCurrentUser() >>  {
                return (user == "pi") ? request.pi : request.requester
            }
        }

        expect:
        request == service.get(request.id)

        where:
        user << ["pi", "requester"]
    }

    void "test get, when current user is neither PI nor requester, return null"() {
        given:
        ProjectRequest request = DomainFactory.createProjectRequest()
        ProjectRequestService service = new ProjectRequestService()
        service.springSecurityService = Mock(SpringSecurityService) {
            getCurrentUser() >> DomainFactory.createUser()
        }

        expect:
        !service.get(request.id)
    }

    void "test getCreatedAndApproved"() {
        given:
        User user = DomainFactory.createUser()
        List<ProjectRequest> requests = []
        DomainFactory.createProjectRequest(pi: user, status: WAITING_FOR_PI)
        requests << DomainFactory.createProjectRequest(pi: user, status: APPROVED_BY_PI_WAITING_FOR_OPERATOR)
        requests << DomainFactory.createProjectRequest(pi: user, status: DENIED_BY_PI)
        requests << DomainFactory.createProjectRequest(pi: user, status: DENIED_BY_OPERATOR)
        requests << DomainFactory.createProjectRequest(pi: user, status: PROJECT_CREATED, project: DomainFactory.createProject())
        requests << DomainFactory.createProjectRequest(requester: user, status: WAITING_FOR_PI)
        requests << DomainFactory.createProjectRequest(requester: user, status: APPROVED_BY_PI_WAITING_FOR_OPERATOR)
        requests << DomainFactory.createProjectRequest(requester: user, status: DENIED_BY_PI)
        requests << DomainFactory.createProjectRequest(requester: user, status: DENIED_BY_OPERATOR)
        requests << DomainFactory.createProjectRequest(requester: user, status: PROJECT_CREATED, project: DomainFactory.createProject())

        ProjectRequestService service = new ProjectRequestService()
        service.springSecurityService = Mock(SpringSecurityService) {
            getCurrentUser() >> user
        }

        expect:
        TestCase.assertContainSame( service.getCreatedAndApproved(), requests )
    }

    void "test getWaiting"() {
        User user = DomainFactory.createUser()
        List<ProjectRequest> requests = []
        requests << DomainFactory.createProjectRequest(pi: user, status: WAITING_FOR_PI)
        DomainFactory.createProjectRequest(pi: user, status: APPROVED_BY_PI_WAITING_FOR_OPERATOR)
        DomainFactory.createProjectRequest(pi: user, status: DENIED_BY_PI)
        DomainFactory.createProjectRequest(pi: user, status: DENIED_BY_OPERATOR)
        DomainFactory.createProjectRequest(pi: user, status: PROJECT_CREATED, project: DomainFactory.createProject())
        DomainFactory.createProjectRequest(requester: user, status: WAITING_FOR_PI)
        DomainFactory.createProjectRequest(requester: user, status: APPROVED_BY_PI_WAITING_FOR_OPERATOR)
        DomainFactory.createProjectRequest(requester: user, status: DENIED_BY_PI)
        DomainFactory.createProjectRequest(requester: user, status: DENIED_BY_OPERATOR)
        DomainFactory.createProjectRequest(requester: user, status: PROJECT_CREATED, project: DomainFactory.createProject())

        ProjectRequestService service = new ProjectRequestService()
        service.springSecurityService = Mock(SpringSecurityService) {
            getCurrentUser() >> user
        }

        expect:
        TestCase.assertContainSame( service.getWaiting(), requests )
    }

    void "test create"() {
        given:
        User pi = DomainFactory.createUser()
        User deputyPi = DomainFactory.createUser()
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(
                name: "name",
                description: "description",
                organizationalUnit: "ou",
                projectType: Project.ProjectType.SEQUENCING,
                pi: pi.username,
                deputyPis: [deputyPi.username],
        )
        ProjectRequestService service = new ProjectRequestService()
        service.springSecurityService = Mock(SpringSecurityService) {
            getCurrentUser() >> DomainFactory.createUser()
        }
        service.linkGenerator = Mock(LinkGenerator) {
            1 * link(_) >> 'link'
        }
        service.messageSourceService = Mock(MessageSourceService) {
            1 * createMessage(_, _) >> "message"
        }
        service.messageSource = Mock(PluginAwareResourceBundleMessageSource) {
            getMessageInternal(_, _, _) >> "whatever"
        }
        service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _) >> null
        }
        service.userProjectRoleService = Mock(UserProjectRoleService) {
            1 * createUserWithLdapData(pi.username) >> pi
            1 * createUserWithLdapData(deputyPi.username) >> deputyPi
        }

        when:
        service.create(cmd)

        then:
        notThrown()
    }

    void "test create, fails if user does not exist"() {
        given:
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(
                name: "name",
                description: "description",
                organizationalUnit: "ou",
                projectType: Project.ProjectType.SEQUENCING,
                pi: DomainFactory.createUser().username,
                deputyPis: ["non-existent"],
        )
        ProjectRequestService service = new ProjectRequestService()
        service.springSecurityService = Mock(SpringSecurityService) {
            getCurrentUser() >> DomainFactory.createUser()
        }
        service.linkGenerator = Mock(LinkGenerator) {
            0 * link(_) >> 'link'
        }
        service.messageSourceService = Mock(MessageSourceService) {
            0 * createMessage(_, _) >> "message"
        }
        service.messageSource = Mock(PluginAwareResourceBundleMessageSource) {
            getMessageInternal(_, _, _) >> "whatever"
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
        notThrown()
        request.status == expectedStatus

        where:
        currentStatus                       | isPi  | wantedStatus                        | confirmConsent | confirmRecord || mail | expectedStatus
        WAITING_FOR_PI                      | true  | APPROVED_BY_PI_WAITING_FOR_OPERATOR | true           | true          || 1    | APPROVED_BY_PI_WAITING_FOR_OPERATOR
        WAITING_FOR_PI                      | true  | DENIED_BY_PI                        | true           | true          || 0    | DENIED_BY_PI
    }
}
