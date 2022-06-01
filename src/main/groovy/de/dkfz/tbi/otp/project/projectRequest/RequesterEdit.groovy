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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.ProjectRequest
import de.dkfz.tbi.otp.security.User

import javax.naming.OperationNotSupportedException

@Component
class RequesterEdit implements ProjectRequestState {

    @Autowired
    ProjectRequestPersistentStateService projectRequestPersistentStateService

    @Override
    List<ProjectRequestAction> getIndexActions(ProjectRequest projectRequest) {
        if (securityService.currentUserAsUser == projectRequest.requester) {
            return [ProjectRequestAction.SUBMIT_INDEX, ProjectRequestAction.SAVE_INDEX]
        }
    }

    @Override
    List<ProjectRequestAction> getViewActions(ProjectRequest projectRequest) {
        User currentUser = securityService.currentUserAsUser
        return currentUser == projectRequest.requester ? [ProjectRequestAction.SUBMIT_VIEW, ProjectRequestAction.EDIT, ProjectRequestAction.DELETE] : []
    }

    @Override
    @PreAuthorize("hasPermission(#cmd.projectRequest, 'PROJECT_REQUEST_CURRENT_OWNER')")
    Long submit(ProjectRequestCreationCommand cmd) {
        projectRequestStateProvider.setState(cmd.projectRequest, Check)
        ProjectRequest projectRequest = projectRequestService.saveProjectRequestFromCommand(cmd)
        projectRequestService.sendSubmitEmail(projectRequest)
        return projectRequest.id
    }

    @Override
    @PreAuthorize("hasPermission(#projectRequest, 'PROJECT_REQUEST_CURRENT_OWNER')")
    ProjectRequestCreationCommand edit(ProjectRequest projectRequest) {
        return ProjectRequestCreationCommand.fromProjectRequest(projectRequest)
    }

    @Override
    void approve(ApprovalCommand cmd) {
    }

    @Override
    @PreAuthorize("hasPermission(#cmd.projectRequest, 'PROJECT_REQUEST_CURRENT_OWNER')")
    Long save(ProjectRequestCreationCommand cmd) {
        ProjectRequest projectRequest = projectRequestService.saveProjectRequestFromCommand(cmd)
        projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, securityService.currentUserAsUser)
        return projectRequest.id
    }

    @Override
    void reject(ProjectRequest projectRequest, String additionalComment) {
    }

    @Override
    void passOn(ProjectRequest projectRequest) {
    }

    @Override
    @PreAuthorize("hasPermission(#projectRequest, 'PROJECT_REQUEST_CURRENT_OWNER')")
    void delete(ProjectRequest projectRequest) {
        projectRequestService.deleteProjectRequest(projectRequest)
        projectRequestService.sendDeleteEmail(projectRequest)
    }

    @Override
    void create(ProjectRequest projectRequest) {
        throw new OperationNotSupportedException("Project can not be created in RequestEdit state")
    }
}
