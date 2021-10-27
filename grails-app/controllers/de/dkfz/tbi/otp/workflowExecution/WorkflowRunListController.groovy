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
import groovy.transform.TupleConstructor
import org.hibernate.NullPrecedence
import org.hibernate.criterion.Order
import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.utils.DataTablesCommand
import de.dkfz.tbi.util.TimeFormats

@Secured("hasRole('ROLE_OPERATOR')")
class WorkflowRunListController extends AbstractWorkflowRunController {

    static allowedMethods = [
            index: "GET",
            data : "GET",
    ]

    Map index(RunShowCommand cmd) {
        List<Workflow> workflows = Workflow.list().sort { a, b ->
            !a.enabled <=> !b.enabled ?: String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())
        }

        return [
                cmd      : cmd,
                workflows: workflows,
                states   : WorkflowRunOverviewController.STATES,
                columns  : Column.values(),
        ]
    }

    @SuppressWarnings("AbcMetric")
    def data(RunDataShowCommand cmd) {
        cmd.processParams(params)

        Closure criteria = getCriteria(cmd.workflow, cmd.states, cmd.name)

        List data = WorkflowRun.createCriteria().list {
            criteria.delegate = delegate
            criteria()
            cmd.orderList.each { DataTablesCommand.Order dtOrder ->
                if (Column.fromDataTable(dtOrder.column) == Column.COMMENT) {
                    createAlias("comment", "comment", JoinType.LEFT_OUTER_JOIN)
                    if (dtOrder.direction == DataTablesCommand.Order.Dir.asc) {
                        addOrder(Order.asc("comment.modificationDate").nulls(NullPrecedence.LAST))
                    } else {
                        addOrder(Order.desc("comment.modificationDate").nulls(NullPrecedence.LAST))
                    }
                } else {
                    order(Column.fromDataTable(dtOrder.column).orderColumn, dtOrder.direction.name())
                }
            }
            firstResult(cmd.start)
            if (cmd.pagingEnabled) {
                maxResults(cmd.length)
            }
        }.collect { WorkflowRun r ->
            String duration = r.workflowSteps.empty ? "-" :
                    r.state in [WorkflowRun.State.PENDING,
                                WorkflowRun.State.RUNNING_WES,
                                WorkflowRun.State.RUNNING_OTP,] ?
                            getFormattedDuration(convertDateToLocalDateTime(r.workflowSteps.first().dateCreated), convertDateToLocalDateTime(new Date())) :
                            getFormattedDuration(convertDateToLocalDateTime(r.workflowSteps.first().dateCreated),
                                    convertDateToLocalDateTime(r.workflowSteps.last().lastUpdated))

            List<WorkflowStep> steps = r.workflowSteps.findAll { !it.obsolete }
            WorkflowStep lastStep = steps ? steps.last() : null
            return [
                    state      : r.state,
                    stateDesc  : r.state.description,
                    comment    : r.comment?.displayString()?.replaceAll("\n", ", ") ?: "",
                    workflow   : r.workflow.toString(),
                    displayName: r.displayName,
                    shortName  : r.shortDisplayName,
                    dateCreated: TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(r.dateCreated),
                    lastUpdated: lastStep?.lastUpdated ? TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(lastStep.lastUpdated) : "",
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
            "in"("state", [WorkflowRun.State.RUNNING_OTP, WorkflowRun.State.RUNNING_WES])
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
        STATUS("workflowRun.list.state", "state"),
        COMMENT("workflowRun.list.comment", "modificationDate"),
        WORKFLOW("workflowRun.list.workflow", ""),
        NAME("workflowRun.list.name", "displayName"),
        STEP("workflowRun.list.step", ""),
        CREATED("workflowRun.list.created", "dateCreated"),
        UPDATED("workflowRun.list.updated", "lastUpdated"),
        DURATION("workflowRun.list.duration", ""),
        ID("workflowRun.list.id", "id"),
        BUTTONS("", ""),

        final String message
        final String orderColumn

        static Column fromDataTable(int column) {
            if (column >= values().size() || column < 0) {
                return UPDATED
            }
            return values()[column]
        }
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
