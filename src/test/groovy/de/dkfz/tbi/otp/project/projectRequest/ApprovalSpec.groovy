/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.project.projectRequest

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User

import javax.naming.OperationNotSupportedException

@SuppressWarnings('ExplicitFlushForDeleteRule')
class ApprovalSpec extends Specification implements UserDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                User,
                ProjectRequest,
                ProjectRequestPersistentState,
                ProjectRequestUser,
        ]
    }

    SecurityService securityService
    ProjectRequestService projectRequestService
    ProjectRequestStateProvider projectRequestStateProvider
    ProjectRequestState state
    ProjectRequestPersistentStateService projectRequestPersistentStateService

    void setup() {
        securityService = Mock(SecurityService)
        projectRequestService = Mock(ProjectRequestService)
        projectRequestStateProvider = Mock(ProjectRequestStateProvider)
        projectRequestPersistentStateService = Mock(ProjectRequestPersistentStateService)
        state = new Approval(securityService: securityService, projectRequestService: projectRequestService,
                projectRequestStateProvider: projectRequestStateProvider, projectRequestPersistentStateService: projectRequestPersistentStateService)
    }

    void "reject"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])
        String rejectComment = "comment"

        when:
        state.reject(projectRequest, rejectComment)

        then:
        1 * projectRequestStateProvider.setState(projectRequest, RequesterEdit) >> _
        1 * projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, requester)
        1 * projectRequestService.sendPiRejectEmail(projectRequest, rejectComment)
        0 * _
    }

    void "approve proceeds to next state if all have approved"() {
        given:
        User requester = createUser()
        User pi = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])
        ApprovalCommand cmd = new ApprovalCommand(
                projectRequest: projectRequest,
                confirmConsent: true,
                confirmRecordOfProcessingActivities: true,
        )

        when:
        state.approve(cmd)

        then:
        1 * securityService.currentUserAsUser >> pi
        1 * projectRequestService.approveProjectRequest(projectRequest, pi)
        1 * projectRequestPersistentStateService.allProjectRequestUsersApproved(projectRequest.state) >> true
        1 * projectRequestStateProvider.setState(projectRequest, Approved)
        1 * projectRequestService.sendApprovedEmail(projectRequest)
        0 * _
    }

    void "approve stays in state when not all have approved"() {
        given:
        User requester = createUser()
        User pi = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])
        ApprovalCommand cmd = new ApprovalCommand(
                projectRequest: projectRequest,
                confirmConsent: true,
                confirmRecordOfProcessingActivities: true,
        )

        when:
        state.approve(cmd)

        then:
        1 * securityService.currentUserAsUser >> pi
        1 * projectRequestService.approveProjectRequest(projectRequest, pi)
        1 * projectRequestPersistentStateService.allProjectRequestUsersApproved(projectRequest.state) >> false
        1 * projectRequestService.sendPartiallyApprovedEmail(projectRequest)
        0 * _
    }

    void "delete"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])

        when:
        state.delete(projectRequest)

        then:
        1 * projectRequestService.deleteProjectRequest(projectRequest)
        1 * projectRequestService.sendDeleteEmail(projectRequest)
        0 * _
    }

    void "edit"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])

        when:
        state.edit(projectRequest)

        then:
        1 * securityService.currentUserAsUser >> requester
        1 * projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, requester)
        1 * projectRequestStateProvider.setState(projectRequest, PiEdit)
        0 * _
    }

    void "create should fail with OperationNotSupportedException"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])

        when:
        state.create(projectRequest)

        then:
        thrown(OperationNotSupportedException)
    }

}
