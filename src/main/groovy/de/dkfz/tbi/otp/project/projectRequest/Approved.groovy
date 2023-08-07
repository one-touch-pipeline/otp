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

import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.ProjectRequest
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User

@Component
class Approved implements ProjectRequestState {

    @Autowired
    ProjectRequestPersistentStateService projectRequestPersistentStateService

    String displayName = "projectRequestState.displayName.approved"

    @Override
    List<ProjectRequestAction> getIndexActions(ProjectRequest projectRequest) {
        User currentUser = securityService.currentUser
        return (currentUser == projectRequest?.state?.currentOwner) ? [ProjectRequestAction.SAVE_INDEX] : []
    }

    @Override
    List<ProjectRequestAction> getViewActions(ProjectRequest projectRequest) {
        boolean currentUserIsOperator = securityService.ifAllGranted(Role.ROLE_OPERATOR)
        return currentUserIsOperator ? [ProjectRequestAction.CREATE, ProjectRequestAction.EDIT, ProjectRequestAction.REJECT, ProjectRequestAction.DELETE] : []
    }

    @Override
    Long submit(ProjectRequestCreationCommand cmd) {
        return null
    }

    @CompileDynamic
    @Override
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void reject(ProjectRequest projectRequest, String rejectComment) {
        projectRequestStateProvider.setState(projectRequest, RequesterEdit)
        projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, projectRequest.requester)
        commentService.saveCommentWithMaskedUsername(projectRequest, rejectComment)
        projectRequestService.sendOperatorRejectEmail(projectRequest, rejectComment)
    }

    @Override
    void passOn(ProjectRequest projectRequest) {
    }

    @Override
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProjectRequestCreationCommand edit(ProjectRequest projectRequest) throws ProjectRequestBeingEditedException {
        User currentUser = securityService.currentUser
        if (projectRequest.state.currentOwner == null || projectRequest.state.currentOwner == currentUser) {
            projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, currentUser)
            return ProjectRequestCreationCommand.fromProjectRequest(projectRequest)
        }
        throw new ProjectRequestBeingEditedException(messageSourceService.createMessage("projectRequest.edit.already"))
    }

    @Override
    void approve(ApprovalCommand cmd) {
    }

    @Override
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Long save(ProjectRequestCreationCommand cmd) throws ProjectRequestBeingEditedException {
        if (cmd.projectRequest.state.currentOwner == securityService.currentUser) {
            return projectRequestService.saveProjectRequestFromCommand(cmd).id
        }
        throw new ProjectRequestBeingEditedException(messageSourceService.createMessage("projectRequest.edit.already"))
    }

    @CompileDynamic
    @Override
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void create(ProjectRequest projectRequest) {
        projectRequestStateProvider.setState(projectRequest, Created)
    }

    @Override
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void delete(ProjectRequest projectRequest) {
        projectRequestService.deleteProjectRequest(projectRequest)
        projectRequestService.sendDeleteEmail(projectRequest)
    }
}
