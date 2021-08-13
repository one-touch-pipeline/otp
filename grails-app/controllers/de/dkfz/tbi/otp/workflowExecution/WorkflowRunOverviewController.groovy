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

import de.dkfz.tbi.util.TimeFormats

import java.sql.Timestamp

@Secured("hasRole('ROLE_ADMIN')")
class WorkflowRunOverviewController {

    static allowedMethods = [
            index: "GET",
    ]

    WorkflowRunOverviewService workflowRunOverviewService

    static final Map<String, List<WorkflowRun.State>> STATES = [
            ("Input required"): [
                    WorkflowRun.State.WAITING_FOR_USER,
                    WorkflowRun.State.FAILED,
            ].asImmutable(),
            ("Not finished")  : [
                    WorkflowRun.State.PENDING,
                    WorkflowRun.State.RUNNING_WES,
                    WorkflowRun.State.RUNNING_OTP,
            ].asImmutable(),
            ("Finished")      : [
                    WorkflowRun.State.SUCCESS,
                    WorkflowRun.State.OMITTED_MISSING_PRECONDITION,
                    WorkflowRun.State.FAILED_FINAL,
                    WorkflowRun.State.RESTARTED,
                    WorkflowRun.State.KILLED,
            ].asImmutable(),
    ].asImmutable()

    def index() {
        List<Workflow> workflows = Workflow.list().sort { a, b ->
            !a.enabled <=> !b.enabled ?: String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }

        Map<Pair<WorkflowRun.State, Workflow>, Long> runs = workflowRunOverviewService.numberOfRunsPerWorkflowAndState
        Map<Workflow, Timestamp> lastRuns = workflowRunOverviewService.lastRuns
        Map<Workflow, Timestamp> lastFails = workflowRunOverviewService.lastFailedRuns

        lastRuns.each { it.value = TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(it.value) }
        lastFails.each { it.value = TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(it.value) }

        return [
                states   : STATES,
                workflows: workflows,
                lastRuns : lastRuns,
                lastFails: lastFails,
                runs     : runs,
        ]
    }
}
