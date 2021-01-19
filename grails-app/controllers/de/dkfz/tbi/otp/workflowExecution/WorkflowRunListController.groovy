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
import grails.validation.Validateable
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.utils.DataTablesCommand

@Secured("hasRole('ROLE_OPERATOR')")
class WorkflowRunListController extends AbstractWorkflowRunController {

    static allowedMethods = [
            index         : "GET",
            data          : "GET",
    ]

    Map index(RunShowCommand cmd) {
        List<Workflow> workflows = Workflow.list().sort { a, b ->
            !a.enabled <=> !b.enabled ?: String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }

        return [
                cmd: cmd,
                workflows: workflows,
                states: WorkflowRunOverviewController.STATES,
                columns: Column.values(),
        ]
    }

    @SuppressWarnings("AbcMetric")
    def data(RunDataShowCommand cmd) {
        cmd.processParams(params)

        Closure criteria = getCriteria(cmd.workflow, cmd.states, cmd.name)

        List data = WorkflowRun.createCriteria().list {
            criteria.delegate = delegate
            criteria()
            cmd.orderList.each {
                order(Column.fromDataTable(it.column).columnName, it.direction.name())
            }
            firstResult(cmd.start)
            if (cmd.pagingEnabled) {
                maxResults(cmd.length)
            }
        }.collect { WorkflowRun r ->
            String duration = r.workflowSteps.empty ? "-" :
                    r.state in [WorkflowRun.State.PENDING,
                                WorkflowRun.State.WAITING_ON_SYSTEM,
                                WorkflowRun.State.RUNNING,] ?
                            getFormattedDuration(convertDateToLocalDateTime(r.workflowSteps.first().dateCreated), convertDateToLocalDateTime(new Date())) :
                            getFormattedDuration(convertDateToLocalDateTime(r.workflowSteps.first().dateCreated),
                                    convertDateToLocalDateTime(r.workflowSteps.last().lastUpdated))

            List<WorkflowStep> steps = r.workflowSteps.findAll { !it.obsolete }
            WorkflowStep lastStep = steps ? steps.last() : null
            return [
                    state      : r.state,
                    comment    : r.comment?.displayString()?.replaceAll("\n", ", ") ?: "",
                    workflow   : r.workflow.toString(),
                    name       : r.displayName,
                    dateCreated: r.dateCreated,
                    lastUpdated: lastStep?.lastUpdated,
                    duration   : duration,
                    id         : r.id,
                    step       : lastStep?.beanName,
                    stepId     : lastStep?.id,
                    steps      : (steps - lastStep).reverse()*.beanName,
                    stepIds    : (steps - lastStep).reverse()*.id,
            ]
        }
        int workflowsFiltered = WorkflowRun.createCriteria().count {
            criteria.delegate = delegate
            criteria()
        }
        int running = WorkflowRun.createCriteria().count {
            criteria.delegate = delegate
            criteria()
            "in"("state", [WorkflowRun.State.RUNNING, WorkflowRun.State.WAITING_ON_SYSTEM])
        }
        int failed = WorkflowRun.createCriteria().count {
            criteria.delegate = delegate
            criteria()
            eq("state", WorkflowRun.State.FAILED)
        }
        int workflowsTotal = WorkflowRun.countByStateNotEqual(WorkflowRun.State.LEGACY)

        render cmd.getDataToRender(data, workflowsTotal, workflowsFiltered, [count: [workflowsFiltered, running, failed]]) as JSON
    }

    @TupleConstructor
    enum Column {
        CHECKBOX("", ""),
        STATUS("Status", "state"),
        COMMENT("Comment", "comment"),
        WORKFLOW("workflowRun.list.workflow", ""),
        NAME("workflowRun.list.name", "displayName"),
        STEP("workflowRun.list.step", ""),
        CREATED("workflowRun.list.created", "dateCreated"),
        UPDATED("workflowRun.list.updated", "lastUpdated"),
        DURATION("workflowRun.list.duration", ""),
        ID("workflowRun.list.id", "id"),
        BUTTONS("", ""),

        final String message
        final String columnName

        static Column fromDataTable(int column) {
            if (column >= values().size() || column < 0) {
                return UPDATED
            }
            return values()[column]
        }
    }
}

class RunUpdateCommand extends RunShowCommand implements Validateable {
    List<Long> step = []

    static constraints = {
        workflow nullable: true
        state nullable: true
        name nullable: true
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
