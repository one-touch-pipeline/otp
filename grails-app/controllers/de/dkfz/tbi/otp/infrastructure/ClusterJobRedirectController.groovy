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
package de.dkfz.tbi.otp.infrastructure

import grails.validation.Validateable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ClusterJobRedirectController {

    static allowedMethods = [
            redirect: "GET",
    ]

    ClusterJobService clusterJobService

    def redirect(ClusterJobCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        // The clusterJobId is not unique. Therefore the newest clusterJobId is fetched and used here.
        ClusterJob dbClusterJob = clusterJobService.findNewestClusterJobByClusterJobId(cmd.clusterJob.clusterJobId)

        if (!dbClusterJob) {
            return response.sendError(HttpStatus.NOT_FOUND.value())
        }

        if (dbClusterJob.oldSystem) {
            redirect(controller: "processes", action: "process", id: dbClusterJob.processingStep.process.id)
        } else {
            redirect(controller: "workflowRunDetails", action: "index", id: dbClusterJob.workflowStep.workflowRun.id)
        }
    }
}

class ClusterJobCommand implements Validateable {
    ClusterJob clusterJob
}
