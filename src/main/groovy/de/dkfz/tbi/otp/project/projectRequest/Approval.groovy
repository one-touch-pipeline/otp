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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.ProjectRequest
import de.dkfz.tbi.otp.security.*

@Component
class Approval implements ProjectRequestState {

    @Autowired
    ProjectRequestUserService projectRequestUserService

    @Autowired
    SecurityService securityService

    @Autowired
    ProjectRequestPersistentStateService projectRequestPersistentStateService

    @Override
    List<ProjectRequestAction> getIndexActions(ProjectRequest projectRequest) {
        return []
    }

    @Override
    List<ProjectRequestAction> getViewActions(ProjectRequest projectRequest) {
        User currentUser = securityService.currentUserAsUser
        boolean currentUserIsOperator = SpringSecurityUtils.ifAllGranted(Role.ROLE_OPERATOR)
        List<ProjectRequestAction> actions = []
        if (projectRequest.state.usersThatNeedToApprove.contains(currentUser)) {
            actions.addAll([ProjectRequestAction.APPROVE, ProjectRequestAction.REJECT])
        }
        if (ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state).contains(currentUser)) {
            actions.add(ProjectRequestAction.EDIT)
        }
        if (currentUserIsOperator) {
            actions.add(ProjectRequestAction.DELETE)
        }
        return actions
    }

    @Override
    Long submit(ProjectRequestCreationCommand cmd) {
        return null
    }

    @Override
    Long save(ProjectRequestCreationCommand cmd) {
        return null
    }

    @Override
    @PreAuthorize("hasPermission(#projectRequest, 'PROJECT_REQUEST_NEEDED_PIS')")
    void reject(ProjectRequest projectRequest, String rejectComment) {
        projectRequestStateProvider.setState(projectRequest, RequesterEdit)
        projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, projectRequest.requester)
        projectRequestService.sendPiRejectEmail(projectRequest, rejectComment)
    }

    @Override
    void passOn(ProjectRequest projectRequest) {
    }

    @Override
    @PreAuthorize("hasPermission(#projectRequest, 'PROJECT_REQUEST_PI')")
    ProjectRequestCreationCommand edit(ProjectRequest projectRequest) {
        User currentUser = securityService.currentUserAsUser
        projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, currentUser)
        projectRequestStateProvider.setState(projectRequest, PiEdit)
        return ProjectRequestCreationCommand.fromProjectRequest(projectRequest)
    }

    @Override
    @PreAuthorize("hasPermission(#cmd.projectRequest, 'PROJECT_REQUEST_NEEDED_PIS')")
    void approve(ApprovalCommand cmd) throws SwitchedUserDeniedException {
        ProjectRequest projectRequest = cmd?.projectRequest
        projectRequestService.approveProjectRequest(projectRequest, securityService.currentUserAsUser)
        if (projectRequestPersistentStateService.allProjectRequestUsersApproved(projectRequest.state)) {
            projectRequestStateProvider.setState(projectRequest, Approved)
            projectRequestService.sendApprovedEmail(projectRequest)
        } else {
            projectRequestService.sendPartiallyApprovedEmail(projectRequest)
        }
    }

    @Override
    @Secured("hasRole('ROLE_OPERATOR')")
    void delete(ProjectRequest projectRequest) {
        projectRequestService.deleteProjectRequest(projectRequest)
        projectRequestService.sendDeleteEmail(projectRequest)
    }

    @Override
    void create(ProjectRequest projectRequest) {
        assert ("Project can not created in Approval state")
    }
}
