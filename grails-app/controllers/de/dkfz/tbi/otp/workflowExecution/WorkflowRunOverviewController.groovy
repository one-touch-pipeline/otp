/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.plugin.springsecurity.annotation.Secured
import grails.util.Pair

import java.sql.Timestamp

@Secured("hasRole('ROLE_ADMIN')")
class WorkflowRunOverviewController {

    static allowedMethods = [
            index: "GET",
    ]

    WorkflowRunOverviewService workflowRunOverviewService

    def index() {
        List<Workflow> workflows = Workflow.list().sort { a, b ->
            !a.enabled <=> !b.enabled ?: String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }

        Map<String, List<WorkflowRun.State>> states = [
                ("Input required"): [
                        WorkflowRun.State.WAITING_ON_USER,
                        WorkflowRun.State.FAILED,
                ],
                ("Not finished")  : [
                        WorkflowRun.State.PENDING,
                        WorkflowRun.State.WAITING_ON_SYSTEM,
                        WorkflowRun.State.RUNNING,
                ],
                ("Finished")      : [
                        WorkflowRun.State.SUCCESS,
                        WorkflowRun.State.SKIPPED,
                        WorkflowRun.State.FAILED_FINAL,
                        WorkflowRun.State.KILLED,
                ],
        ]

        Map<Pair<WorkflowRun.State, Workflow>, Long> runs = workflowRunOverviewService.numberOfRunsPerWorkflowAndState
        Map<Workflow, Timestamp> lastRuns = workflowRunOverviewService.lastRuns
        Map<Workflow, Timestamp> lastFails = workflowRunOverviewService.lastFailedRuns

        return [
                states   : states,
                workflows: workflows,
                lastRuns : lastRuns,
                lastFails: lastFails,
                runs     : runs,
        ]
    }
}
