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

import grails.gorm.transactions.Transactional
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ngsdata.ProjectRoleService
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class ProjectRequestPersistentStateService {

    @Autowired
    ProjectRequestStateProvider projectRequestStateProvider

    void setCurrentOwner(ProjectRequestPersistentState state, User user) {
        if (state.currentOwner != user) {
            state.currentOwner = user
            saveProjectRequestPersistentState(state)
        }
    }

    void removeCurrentOwner(ProjectRequestPersistentState state) {
        setCurrentOwner(state, null)
    }

    ProjectRequestPersistentState saveProjectRequestPersistentStateForProjectRequest(
            ProjectRequest projectRequest, List<ProjectRequestUser> projectRequestUsers) {
        List<User> usersThatNeedToApprove = projectRequestUsers.findAll {
            ProjectRoleService.projectRolesContainAuthoritativeRole(it.projectRoles)
        }*.user
        Map<String, Object> projectRequestStateParameters = [
                currentOwner            : null,
                beanName                : ProjectRequestStateProvider.getStateBeanName(projectRequestStateProvider.getCurrentState(projectRequest)),
                usersThatAlreadyApproved: [] as Set,
                usersThatNeedToApprove  : usersThatNeedToApprove as Set,
        ]
        ProjectRequestPersistentState state
        if (projectRequest?.state) {
            InvokerHelper.setProperties(projectRequest.state, projectRequestStateParameters)
            state = projectRequest.state
        } else {
            state = new ProjectRequestPersistentState(projectRequestStateParameters)
            saveProjectRequestPersistentState(state)
        }
        return state
    }

    ProjectRequestPersistentState saveProjectRequestPersistentState(ProjectRequestPersistentState state) {
        return state.save(flush: true)
    }

    void approveUser(ProjectRequestPersistentState state, User user) {
        User userThatApproves = CollectionUtils.atMostOneElement(state.usersThatNeedToApprove.findAll { it == user })
        assert userThatApproves: "User doesn't need to approve this request."
        state.usersThatNeedToApprove.remove(userThatApproves)
        state.usersThatAlreadyApproved.add(userThatApproves)
        saveProjectRequestPersistentState(state)
    }

    static List<User> getAllProjectRequestAuthorities(ProjectRequestPersistentState state) {
        List<User> allAuthorities = (state.usersThatNeedToApprove as List) ?: []
        if (state.usersThatAlreadyApproved) {
            allAuthorities.addAll(state.usersThatAlreadyApproved)
        }
        return allAuthorities
    }

    boolean allProjectRequestUsersApproved(ProjectRequestPersistentState state) {
        return state.usersThatNeedToApprove.empty
    }

    void deleteProjectRequestState(ProjectRequestPersistentState state) {
        state.delete(flush: true)
    }
}
