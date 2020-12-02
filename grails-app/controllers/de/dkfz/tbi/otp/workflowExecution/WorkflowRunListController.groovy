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
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.ToString
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.SqlUtil
import de.dkfz.tbi.otp.config.ConfigService

import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Secured("hasRole('ROLE_OPERATOR')")
class WorkflowRunListController implements CheckAndCall {

    ConfigService configService
    JobService jobService
    WorkflowService workflowService
    WorkflowStateChangeService workflowStateChangeService

    static allowedMethods = [
            index         : "GET",
            data          : "GET",
            setFailedFinal: "POST",
            restartStep   : "POST",
            restartRun    : "POST",
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

        Closure criteria = {
            if (cmd.name) {
                ilike("displayName", "%${SqlUtil.replaceWildcardCharactersInLikeExpression(cmd.name)}%")
            }
            if (cmd.states) {
                'in'("state", cmd.states)
            }
            if (cmd.workflow) {
                eq("workflow", cmd.workflow)
            }
            ne("state", WorkflowRun.State.LEGACY)
        }

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
        STATUS("", "state"),
        COMMENT("", "comment"),
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

    def setFailedFinal(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.setFailed") {
            workflowStateChangeService.changeStateToFinalFailed(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect action: "index", params: ["workflow.id": cmd.workflow?.id, state: cmd.state, name: cmd.name]
   }

    def restartStep(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.restartSteps") {
            jobService.createRestartedJobAfterJobFailures(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect action: "index", params: ["workflow.id": cmd.workflow?.id, state: cmd.state, name: cmd.name]
    }

    def restartRun(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.restartRuns") {
            workflowService.createRestartedWorkflows(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect action: "index", params: ["workflow.id": cmd.workflow?.id, state: cmd.state, name: cmd.name]
    }

    private LocalDateTime convertDateToLocalDateTime(Date date) {
        return date.toInstant().atZone(configService.timeZoneId).toLocalDateTime()
    }

    private String getFormattedDuration(LocalDateTime start, LocalDateTime end) {
        long millis = Duration.between(start, end) toMillis()
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))
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

class DataTablesCommand {
    int draw
    int start
    int length

    boolean isPagingEnabled() {
        return length != -1
    }

    /**
     * DataTables passes the ordering information in a way that can't be automatically bound to a command object,
     * so if you want to use ordering information, you need to pass the params object the processParams method first
     */
    List<Order> orderList

    void processParams(GrailsParameterMap parameterMap) {
        Map<Integer, Order> map = [:].withDefault { new Order() }
        parameterMap.findAll { String k, v ->  k.startsWith("order") }.each { String key, String value ->
            List keyFields = key.replace(']', '').split(/\[/)
            Order order = map[keyFields[1] as int]
            if (keyFields[2] == "column") {
                order.column = value as int
            } else if (keyFields[2] == "dir") {
                order.direction = Order.Dir.valueOf(value)
            }
        }
        orderList = map.sort { it.key }*.value
    }

    Map getDataToRender(List data, Integer recordsTotal = null, Integer recordsFiltered = null, Map<String, Object> additionalData = null,
                        String error = null) {
        Map result = [
                draw: draw,
                data: data,
                recordsTotal: recordsTotal ?: data.size(),
                recordsFiltered: recordsFiltered ?: recordsTotal ?: data.size(),
        ]
        if (error) {
            result["error"] = error
        }
        if (additionalData) {
            assert !additionalData.keySet().any { it in ["draw", "data", "recordsTotal", "recordsFiltered", "error"] }
            result.putAll(additionalData)
        }
        return result
    }

    @ToString
    class Order {
        int column
        Dir direction

        @SuppressWarnings("FieldName")
        enum Dir {
            asc, desc
        }
    }
}
