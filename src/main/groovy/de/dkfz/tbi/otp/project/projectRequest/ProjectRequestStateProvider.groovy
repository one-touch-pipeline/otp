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

import org.springframework.aop.framework.AopProxyUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.ProjectRequest

import javax.annotation.PostConstruct

@Component
class ProjectRequestStateProvider {

    Collection<ProjectRequestState> projectRequestStates
    static final Class<ProjectRequestState> INITIAL_STATE = Initial

    @Autowired
    ProjectRequestService projectRequestService

    @Autowired
    ApplicationContext applicationContext

    @PostConstruct
    void initialize() {
        projectRequestStates = applicationContext.getBeansOfType(ProjectRequestState).values()
    }

    ProjectRequestState getInitialState() {
        return this.projectRequestStates.find { getStateBeanName(it) == getStateBeanName(INITIAL_STATE) }
    }

    /** Provides the current state class of a projectRequest. If the project request is null it provides the Initial state class. **/
    ProjectRequestState getCurrentState(ProjectRequest projectRequest) {
        if (projectRequest) {
            return this.projectRequestStates.find { getStateBeanName(it) == projectRequest.state.beanName }
        }
        return initialState
    }

    void setState(ProjectRequest projectRequest, Class<ProjectRequestState> stateClass) {
        projectRequest.state.beanName = getStateBeanName(stateClass)
        projectRequestService.saveProjectRequest(projectRequest)
    }

    static String getStateBeanName(Class<ProjectRequestState> stateClass) {
        return stateClass.simpleName.uncapitalize()
    }

    /**To obtain the right simpleName we need AopProxyUtils, because the secure spring annotations are shown in the beans class name**/
    static String getStateBeanName(ProjectRequestState state) {
        Class<ProjectRequestState> stateClass = AopProxyUtils.ultimateTargetClass(state) as Class<ProjectRequestState>
        return stateClass.simpleName.uncapitalize()
    }
}
