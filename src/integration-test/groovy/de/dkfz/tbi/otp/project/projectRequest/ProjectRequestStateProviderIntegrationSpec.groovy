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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.project.ProjectRequest

@Rollback
@Integration
class ProjectRequestStateProviderIntegrationSpec extends Specification implements UserDomainFactory {

    @Autowired
    ProjectRequestStateProvider projectRequestStateProvider

    @Autowired
    static Check check

    List<Class<? extends ProjectRequestState>> projectRequestStates

    void "initialize, should provide the beans in projectRequestStates"() {
        given:
        projectRequestStates = [Approval, Approved, Check, Created, Draft, Initial, PiEdit, RequesterEdit]

        expect:
        projectRequestStateProvider.projectRequestStates*.class.containsAll(projectRequestStates)
    }

    void "getInitialState, should return the initial state class"() {
        expect:
        projectRequestStateProvider.initialState instanceof Initial
    }

    void "getCurrentState, should return"() {
        when:
        ProjectRequest projectRequest = createProjectRequest([
                state: createProjectRequestPersistentState([
                        beanName: "check"
                ])
        ])

        then:
        projectRequestStateProvider.getCurrentState(projectRequest) == check
    }

    void "setState, should return check when called with Check Class"() {
        given:
        ProjectRequest projectRequest = createProjectRequest()

        when:
        projectRequestStateProvider.setState(projectRequest, Approved)

        then:
        projectRequest.state.beanName == "approved"
    }

    void "getStateBeanName, should return initial for the state class Initial"() {
        expect:
        ProjectRequestStateProvider.getStateBeanName(Initial) == "initial"
    }

    void "getStateBeanName, should return check for bean state instances of Check"() {
        expect:
        ProjectRequestStateProvider.getStateBeanName(check) == "check"
    }
}
