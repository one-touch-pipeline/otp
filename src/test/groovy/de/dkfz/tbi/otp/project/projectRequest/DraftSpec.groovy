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
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.UserService

import javax.naming.OperationNotSupportedException

@SuppressWarnings('ExplicitFlushForSaveRule')
@SuppressWarnings('ExplicitFlushForDeleteRule')
class DraftSpec extends Specification implements UserDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                User,
                ProjectRequest,
                ProjectRequestPersistentState,
                ProjectRequestUser,
        ]
    }

    UserService userService
    ProjectRequestService projectRequestService
    ProjectRequestStateProvider projectRequestStateProvider
    ProjectRequestState state
    ProjectRequestPersistentStateService projectRequestPersistentStateService

    void setup() {
        userService = Mock(UserService)
        projectRequestService = Mock(ProjectRequestService)
        projectRequestStateProvider = Mock(ProjectRequestStateProvider)
        projectRequestPersistentStateService = Mock(ProjectRequestPersistentStateService)
        state = new Draft(userService: userService, projectRequestService: projectRequestService,
                projectRequestStateProvider: projectRequestStateProvider, projectRequestPersistentStateService: projectRequestPersistentStateService)
    }

    void "submit"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(projectRequest: projectRequest)

        when:
        Long result = state.submit(cmd)

        then:
        1 * projectRequestService.saveProjectRequestFromCommand(cmd) >> projectRequest
        1 * projectRequestStateProvider.setState(projectRequest, Check) >> _
        1 * projectRequestService.sendSubmitEmail(projectRequest)
        0 * _

        and:
        projectRequest.id == result
    }

    void "save"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(projectRequest: projectRequest)

        when:
        Long result = state.save(cmd)

        then:
        1 * userService.currentUser >> requester
        1 * projectRequestService.saveProjectRequestFromCommand(cmd) >> projectRequest
        1 * projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, requester)
        0 * _

        and:
        projectRequest.id == result
    }

    void "edit"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([
                requester: requester
        ])

        when:
        ProjectRequestCreationCommand result = state.edit(projectRequest)

        then:
        0 * _

        and:
        result
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
        1 * projectRequestService.sendDraftDeleteEmail(projectRequest)
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
