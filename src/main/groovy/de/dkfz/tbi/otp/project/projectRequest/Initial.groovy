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
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.ProjectRequest

import javax.naming.OperationNotSupportedException

@Component
class Initial implements ProjectRequestState {

    @Autowired
    ProjectRequestPersistentStateService projectRequestPersistentStateService

    String displayName = "projectRequestState.displayName.initial"

    @Override
    List<ProjectRequestAction> getIndexActions(ProjectRequest projectRequest) {
        return [ProjectRequestAction.SUBMIT_INDEX, ProjectRequestAction.SAVE_DRAFT]
    }

    @Override
    List<ProjectRequestAction> getViewActions(ProjectRequest projectRequest) {
        return []
    }

    @CompileDynamic
    @Override
    Long submit(ProjectRequestCreationCommand cmd) {
        ProjectRequest createdProjectRequest = projectRequestService.saveProjectRequestFromCommand(cmd)
        projectRequestStateProvider.setState(createdProjectRequest, Check)
        projectRequestService.sendSubmitEmail(createdProjectRequest)
        return createdProjectRequest.id
    }

    @CompileDynamic
    @Override
    Long save(ProjectRequestCreationCommand cmd) {
        ProjectRequest projectRequest = projectRequestService.saveProjectRequestFromCommand(cmd)
        projectRequestStateProvider.setState(projectRequest, Draft)
        projectRequestService.sendDraftCreateEmail(projectRequest)
        projectRequestPersistentStateService.setCurrentOwner(projectRequest.state, securityService.currentUser)
        return projectRequest.id
    }

    @Override
    void reject(ProjectRequest projectRequest, String additionalComment) {
    }

    @Override
    void passOn(ProjectRequest projectRequest) {
    }

    @Override
    ProjectRequestCreationCommand edit(ProjectRequest projectRequest) {
        return null
    }

    @Override
    void approve(ApprovalCommand cmd) {
    }

    @Override
    void delete(ProjectRequest projectRequest) {
    }

    @Override
    void create(ProjectRequest projectRequest) {
        throw new OperationNotSupportedException("Project can not be created in Initial state")
    }
}
