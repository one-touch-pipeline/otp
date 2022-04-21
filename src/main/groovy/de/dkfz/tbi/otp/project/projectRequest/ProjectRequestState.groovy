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

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.project.ProjectRequest
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.utils.MessageSourceService

trait ProjectRequestState {

    @Autowired
    ProjectRequestService projectRequestService

    @Autowired
    ProjectRequestStateProvider projectRequestStateProvider

    @Autowired
    SecurityService securityService

    @Autowired
    MessageSourceService messageSourceService

    @Autowired
    CommentService commentService

    abstract String getDisplayName()

    abstract List<ProjectRequestAction> getIndexActions(ProjectRequest projectRequest)

    abstract List<ProjectRequestAction> getViewActions(ProjectRequest projectRequest)

    abstract Long submit(ProjectRequestCreationCommand cmd)

    abstract Long save(ProjectRequestCreationCommand cmd)

    abstract void reject(ProjectRequest projectRequest, String additionalComment)

    abstract void passOn(ProjectRequest projectRequest)

    abstract ProjectRequestCreationCommand edit(ProjectRequest projectRequest)

    abstract void approve(ApprovalCommand cmd)

    abstract void delete(ProjectRequest projectRequest)

    abstract void create(ProjectRequest projectRequest)
}
