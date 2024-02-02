/*
 * Copyright 2011-2024 The OTP authors
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
import spock.lang.Unroll

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MessageSourceService

import javax.naming.OperationNotSupportedException

@SuppressWarnings('ExplicitFlushForDeleteRule')
@SuppressWarnings('ExplicitFlushForSaveRule')
class CheckSpec extends Specification implements UserDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                User,
                ProjectRequest,
                ProjectRequestPersistentState,
                ProjectRequestUser,
        ]
    }

    MessageSourceService messageSourceService
    SecurityService securityService
    ProjectRequestService projectRequestService
    ProjectRequestStateProvider projectRequestStateProvider
    ProjectRequestState state
    ProjectRequestPersistentStateService projectRequestPersistentStateService
    CommentService commentService

    void setup() {
        messageSourceService = Mock(MessageSourceService)
        securityService = Mock(SecurityService)
        projectRequestService = Mock(ProjectRequestService)
        projectRequestStateProvider = Mock(ProjectRequestStateProvider)
        projectRequestPersistentStateService = Mock(ProjectRequestPersistentStateService)
        commentService = Mock(CommentService)

        state = new Check(securityService: securityService, projectRequestService: projectRequestService, messageSourceService: messageSourceService,
                projectRequestStateProvider: projectRequestStateProvider, projectRequestPersistentStateService: projectRequestPersistentStateService,
                commentService: commentService)
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
        1 * projectRequestService.sendOperatorRejectEmail(projectRequest, rejectComment)
        1 * commentService.saveCommentWithMaskedUsername(projectRequest, rejectComment)
        0 * _
    }

    void "edit should throw ProjectRequestBeingEditedException when edited by other person"() {
        given:
        User requester = createUser()
        User otherUser = createUser()
        ProjectRequest projectRequest = createProjectRequest([requester: requester], [currentOwner: otherUser])

        when:
        state.edit(projectRequest)

        then:
        1 * messageSourceService.createMessage(_) >> "error"
        1 * securityService.currentUser >> requester

        and:
        thrown ProjectRequestBeingEditedException
    }

    @Unroll
    void "edit should be possible if #testDescription"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([requester: requester], [currentOwner: currentOwner])

        when:
        state.edit(projectRequest)

        then:
        1 * securityService.currentUser >> requester
        1 * projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, requester)

        where:
        testDescription        | currentOwner
        "no one edits"         | null
        "user is currentOwner" | createUser()
    }

    void "save should throw exception if user is not currentOwner"() {
        given:
        User requester = createUser()
        User otherUser = createUser()
        ProjectRequest projectRequest = createProjectRequest([requester: requester], [currentOwner: otherUser])
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(projectRequest: projectRequest)

        when:
        state.save(cmd)

        then:
        1 * messageSourceService.createMessage(_) >> "error"
        1 * securityService.currentUser >> requester
        0 * _

        and:
        thrown ProjectRequestBeingEditedException
    }

    void "save should proceed if user is current owner"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([requester: requester], [currentOwner: requester,])
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(projectRequest: projectRequest)

        when:
        Long result = state.save(cmd)

        then:
        1 * securityService.currentUser >> requester
        1 * projectRequestService.saveProjectRequestFromCommand(cmd) >> projectRequest
        0 * _

        and:
        result == projectRequest.id
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
}
