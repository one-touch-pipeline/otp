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

import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.utils.DataTablesCommand

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class WorkflowRunListController extends AbstractWorkflowRunController {

    WorkflowService workflowService

    WorkflowRunService workflowRunService

    WorkflowStepService workflowStepService

    private static final Map<BulkKillResult.FailureReason, String> FAILURE_REASON_CODES = [
            (BulkKillResult.FailureReason.NOT_FOUND)             : 'workflowRun.list.killRuns.reason.notFound',
            (BulkKillResult.FailureReason.PROJECT_NOT_ACCESSIBLE): 'workflowRun.list.killRuns.reason.projectNotAccessible',
            (BulkKillResult.FailureReason.NOT_KILLABLE)          : 'workflowRun.list.killRuns.reason.notKillable',
            (BulkKillResult.FailureReason.UNEXPECTED)            : 'workflowRun.list.killRuns.reason.unexpected',
    ].asImmutable()

    static allowedMethods = [
            index   : "GET",
            data    : "GET",
            killRuns: "POST",
    ]

    Map index(RunShowCommand cmd) {
        List<Workflow> workflows = workflowService.list().sort { a, b ->
            !a.enabled <=> !b.enabled ?: String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }

        return [
                cmd      : cmd,
                workflows: workflows,
                steps    : workflowStepService.currentWorkflowStepBeanNames,
                states   : WorkflowRunOverviewController.STATES,
                columns  : WorkflowRunListColumn.values(),
        ]
    }

    def data(RunDataShowCommand cmd) {
        cmd.processParams(params)

        WorkflowRunSearchCriteria workflowRunSearchCriteria = new WorkflowRunSearchCriteria(
                cmd.workflow,
                cmd.states,
                cmd.name,
                cmd.stepFilter,
                cmd.orderList,
                cmd.start,
                cmd.length
        )

        WorkflowRunSearchResult result = workflowRunService.workflowOverview(workflowRunSearchCriteria)

        render(cmd.getDataToRender(result.data, result.workflowsTotal, result.workflowsFiltered, [
                count: [result.workflowsFiltered, result.running, result.failed]
        ]) as JSON)
    }

    /**
     * Kills the selected runs via {@link WorkflowService#killWorkflowRuns(Set)} and shows a summary with a reason per failed run.
     * The failure lines are rendered unescaped by the flash message template, so they must only contain IDs and message texts.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def killRuns(BulkKillCommand cmd) {
        String redirectUri = cmd.errors.hasFieldErrors('redirect') ? '/workflowRunList/index' : cmd.redirect

        if (cmd.errors.hasFieldErrors('run')) {
            flash.message = new FlashMessage(message(code: 'workflowRun.list.killRuns.invalid') as String,
                    [message(code: 'workflowRun.list.killRuns.invalid') as String])
            redirect uri: redirectUri
            return
        }

        BulkKillResult result = workflowService.killWorkflowRuns(cmd.run)
        int successCount = result.successCount
        List<String> failures = result.failures.collect { Long runId, BulkKillResult.FailureReason reason ->
            "${runId}: ${message(code: FAILURE_REASON_CODES[reason])}".toString()
        }

        if (failures.isEmpty()) {
            flash.message = new FlashMessage(message(code: 'workflowRun.list.killRuns.allSuccess', args: [successCount]) as String)
        } else if (successCount) {
            flash.message = new FlashMessage(message(code: 'workflowRun.list.killRuns.partial',
                    args: [successCount, successCount + failures.size()]) as String, failures)
        } else {
            flash.message = new FlashMessage(message(code: 'workflowRun.list.killRuns.failed') as String, failures)
        }
        redirect uri: redirectUri
    }
}

class RunShowCommand {
    Workflow workflow
    String state
    String name
    String stepFilter
}

class RunDataShowCommand extends DataTablesCommand {
    Workflow workflow

    void setState(String state) {
        states = state.split(",").collect { WorkflowRun.State.valueOf(it) }
    }
    List<WorkflowRun.State> states
    String name
    String stepFilter
}
