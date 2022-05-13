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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.utils.DataTablesCommand

@Secured("hasRole('ROLE_OPERATOR')")
class WorkflowRunListController extends AbstractWorkflowRunController {

    WorkflowService workflowService

    WorkflowRunService workflowRunService

    static allowedMethods = [
            index: "GET",
            data : "GET",
    ]

    Map index(RunShowCommand cmd) {
        List<Workflow> workflows = workflowService.list().sort { a, b ->
            !a.enabled <=> !b.enabled ?: String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }

        return [
                cmd      : cmd,
                workflows: workflows,
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
                cmd.orderList,
                cmd.start,
                cmd.length
        )

        WorkflowRunSearchResult result = workflowRunService.workflowOverview(workflowRunSearchCriteria)

        render cmd.getDataToRender(result.data, result.workflowsTotal, result.workflowsFiltered, [
                count: [result.workflowsFiltered, result.running, result.failed]
        ]) as JSON
    }
}

class RunShowCommand {
    Workflow workflow
    String state
    String name
}

class RunDataShowCommand extends DataTablesCommand {
    Workflow workflow

    void setState(String state) {
        states = state.split(",").collect { WorkflowRun.State.valueOf(it) }
    }
    List<WorkflowRun.State> states
    String name
}
