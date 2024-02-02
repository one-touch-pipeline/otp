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

import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User

import javax.naming.OperationNotSupportedException

@SuppressWarnings('ExplicitFlushForDeleteRule')
@SuppressWarnings('ExplicitFlushForSaveRule')
class PiEditSpec extends Specification implements UserDomainFactory, DataTest {

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
        state = new PiEdit(securityService: securityService, projectRequestService: projectRequestService,
                projectRequestStateProvider: projectRequestStateProvider, projectRequestPersistentStateService: projectRequestPersistentStateService)
    }

    @Unroll
    void "edit should return the cmd"() {
        given:
        ProjectRequest projectRequest = createProjectRequest()

        when:
        ProjectRequestCreationCommand result = state.edit(projectRequest)

        then:
        0 * _
        result.projectRequest == projectRequest
    }

    void "save"() {
        given:
        User requester = createUser()
        ProjectRequest projectRequest = createProjectRequest([requester: requester], [currentOwner: requester])
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand(projectRequest: projectRequest)

        when:
        Long result = state.save(cmd)

        then:
        1 * projectRequestStateProvider.setState(cmd.projectRequest, Approval)
        1 * projectRequestService.saveProjectRequestFromCommand(cmd) >> projectRequest
        1 * projectRequestService.sendPiEditedEmail(projectRequest)
        0 * _

        and:
        result == projectRequest.id
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
