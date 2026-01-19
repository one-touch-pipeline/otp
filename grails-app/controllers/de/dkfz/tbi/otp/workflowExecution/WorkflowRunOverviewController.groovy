/*
 * Copyright 2011-2026 The OTP authors
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

import grails.util.Pair
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

@PreAuthorize("hasRole('ROLE_ADMIN')")
class WorkflowRunOverviewController {

    static allowedMethods = [
            index: "GET",
    ]

    private static final Map<String, List<WorkflowRun.State>> STATES = [
            ("Input required"): [
                    WorkflowRun.State.WAITING_FOR_USER,
                    WorkflowRun.State.FAILED,
                    WorkflowRun.State.FAILED_WAITING,
            ].asImmutable(),
            ("Not finished")  : [
                    WorkflowRun.State.PENDING,
                    WorkflowRun.State.RUNNING_WES,
                    WorkflowRun.State.RUNNING_OTP,
            ].asImmutable(),
            ("Finished")      : [
                    WorkflowRun.State.SUCCESS,
                    WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
                    WorkflowRun.State.FAILED_FINAL,
                    WorkflowRun.State.RESTARTED,
                    WorkflowRun.State.KILLED,
            ].asImmutable(),
    ].asImmutable()

    WorkflowService workflowService
    WorkflowRunOverviewService workflowRunOverviewService
    ReferenceGenomeService referenceGenomeService
    SeqTypeService seqTypeService
    WorkflowSystemService workflowSystemService

    def index() {
        boolean workflowSystemStatus = workflowSystemService.isEnabled()
        List<Workflow> workflows = workflowService.list().sort { a, b ->
            !a.enabled <=> !b.enabled ?: String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }

        Map<Pair<WorkflowRun.State, Workflow>, Long> runs = workflowRunOverviewService.numberOfRunsPerWorkflowAndState
        Map<Workflow, String> lastRuns = workflowRunOverviewService.latestRuns
        Map<Workflow, String> lastFails = workflowRunOverviewService.latestFailedRuns
        Map<Workflow, String> lastSuccesses = workflowRunOverviewService.latestSuccessfulRuns

        List<ReferenceGenome> refGenomes = referenceGenomeService.list().sort { a, b ->
            String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name)
        }
        List<SeqType> seqTypes = seqTypeService.list().sort {
            it.displayNameWithLibraryLayout
        }

        return [
                states              : STATES,
                workflows           : workflows,
                workflowSystemStatus: workflowSystemStatus,
                lastRuns            : lastRuns,
                lastFails           : lastFails,
                lastSuccesses       : lastSuccesses,
                runs                : runs,
                seqTypes            : seqTypes,
                refGenomes          : refGenomes,
        ]
    }
}
