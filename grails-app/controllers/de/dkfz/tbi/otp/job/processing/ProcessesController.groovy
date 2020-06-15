/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.processing

import grails.async.Promise
import grails.converters.JSON
import grails.util.GrailsNameUtils
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.restarting.RestartActionService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.util.TimestampHelper

import static grails.async.Promises.task
import static grails.async.Promises.waitAll

class ProcessesController {
    static allowedMethods = [
            restartWithProcess : "POST",
    ]

    private enum PlanStatus {
        /**
         * No plan has been executed yet
         */
        NEW,
        /**
         * The plan is disabled
         */
        DISABLED,
         /**
         * At least one Process is running
         */
        RUNNING,
        /**
         * At least one Process is running, but last execution failed
         */
        RUNNINGFAILEDBEFORE,
        /**
         * Last Process succeeded, no Process is running
         */
        SUCCESS,
        /**
         * Last Process failed, no Process is running
         */
        FAILURE
    }
    /**
     * Dependency Injection
     */
    JobExecutionPlanService jobExecutionPlanService
    ProcessService processService
    CommentService commentService
    RestartActionService restartActionService

    def index() {
        redirect(action: 'list')
    }

    @SuppressWarnings("EmptyMethod")
    def list() {
    }

    def listData(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        List<JobExecutionPlan> plans = jobExecutionPlanService.getJobExecutionPlans()
        List<JobExecutionPlan> plansWithPrevious = jobExecutionPlanService.getJobExecutionPlansWithPreviousVersions()
        dataToRender.iTotalRecords = plans.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List<Closure> tasks = [
                { jobExecutionPlanService.processCount(plansWithPrevious) },
                { jobExecutionPlanService.failedProcessCount(plansWithPrevious) },
                { jobExecutionPlanService.finishedProcessCount(plansWithPrevious) },
                { jobExecutionPlanService.lastProcessDate(plansWithPrevious, ExecutionState.SUCCESS) },
                { jobExecutionPlanService.lastProcessDate(plansWithPrevious, ExecutionState.FAILURE) },
        ]

        Authentication auth = SecurityContextHolder.context.authentication
        List<Promise> promises = tasks.collect { Closure taskClosure ->
            task {
                SecurityContextHolder.context.authentication = auth
                try {
                    SessionUtils.withNewSession(taskClosure)
                } finally {
                    SecurityContextHolder.context.authentication = null
                }
            }
        }

        def (processCounts, failedProcesses, finishedProcessCounts, lastSuccessDates, lastFailureDates) = waitAll(promises)

        plans.each { plan ->
            long allProcessesCount = processCounts[plan.name] ?: 0L
            long failedProcessCount = failedProcesses[plan.name] ?: 0L
            long finishedProcessesCount = finishedProcessCounts[plan.name] ?: 0L
            Date successDate = lastSuccessDates[plan.name]
            Date failureDate = lastFailureDates[plan.name]

            dataToRender.aaData << [
                    name                  : plan.name,
                    id                    : plan.id,
                    enabled               : plan.enabled,
                    allProcessesCount     : allProcessesCount,
                    finishedProcessesCount: finishedProcessesCount,
                    failedProcessesCount  : failedProcessCount,
                    runningProcessesCount : allProcessesCount - finishedProcessesCount,
                    lastSuccessfulDate    : TimestampHelper.asTimestamp(successDate),
                    lastFailureDate       : TimestampHelper.asTimestamp(failureDate),
            ]
        }

        // perform sorting on fetched data
        // this is acceptable, as we do not use pagination for the process overview
        // fetch the data with multiple queries, that means we cannot sort in the query directly
        // so we have to sort the fetched data
        dataToRender.aaData.sort { a, b ->
            switch (cmd.iSortCol_0) {
                case 1: // number of processes
                    return a.allProcessesCount <=> b.allProcessesCount
                case 2: // number of failed
                    return a.failedProcessesCount <=> b.failedProcessesCount
                case 3: // number of running
                    return a.runningProcessesCount <=> b.runningProcessesCount
                case 4: // last succeeded
                    return a.lastSuccessfulDate.value <=> b.lastSuccessfulDate.value
                case 5: // last failed
                    return a.lastFailureDate.value <=> b.lastFailureDate.value
                case 0:
                default:
                    return a.name.toLowerCase() <=> b.name.toLowerCase()
            }
        }
        // reverse sort order if descending
        if (!cmd.sortOrder) {
            dataToRender.aaData = dataToRender.aaData.reverse()
        }
        render dataToRender as JSON
    }

    def plan() {
        JobExecutionPlan plan = jobExecutionPlanService.getPlan(params.id as long)
        [
                name   : plan.name,
                id     : plan.id,
                state  : params.state,
                enabled: plan.enabled,
        ]
    }

    def planData(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        String sort
        switch (cmd.iSortCol_0) {
            case 2:
                sort = "started"
                break
            case 0:
            default:
                sort = "id"
                break
        }

        JobExecutionPlan plan = jobExecutionPlanService.getPlan(params.id as long)
        List<ExecutionState> restriction = []
        if (params.state == PlanStatus.FAILURE.toString()) {
            restriction = [
                    ExecutionState.FAILURE,
            ]
        } else if (params.state == PlanStatus.RUNNING.toString()) {
            restriction = [
                    ExecutionState.CREATED,
                    ExecutionState.STARTED,
            ]
        }
        Map<Process, ProcessingStepUpdate> processes = jobExecutionPlanService.getLatestUpdatesForPlan(plan, cmd.iDisplayLength, cmd.iDisplayStart, sort, cmd.sortOrder, restriction)
        dataToRender.iTotalRecords = jobExecutionPlanService.getNumberOfProcesses(plan, restriction)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        processes.each { Process process, ProcessingStepUpdate latest ->
            Map parameterData = processParameterData(process)
            def actions = []
            ExecutionState latestState = latest.state
            if (latestState == ExecutionState.FAILURE) {
                if (Process.findByRestarted(process)) {
                    latestState = ExecutionState.RESTARTED
                } else {
                    actions << "restart"
                }
            }
            dataToRender.aaData << [
                process.id,
                latestState,
                parameterData,
                TimestampHelper.asTimestamp(process.started),
                TimestampHelper.asTimestamp(latest.date),
                latest.processingStep.jobDefinition.name,
                [state: latestState, error: latest.error ? latest.error.errorMessage : null, id: latest.processingStep.id],
                process.comment?.comment?.encodeAsHTML(),
                [actions: actions],
            ]
        }
        render dataToRender as JSON
    }

    def enablePlan() {
        render jobExecutionPlanService.enablePlan(jobExecutionPlanService.getPlan(params.id as long))
    }

    def disablePlan() {
        render jobExecutionPlanService.disablePlan(jobExecutionPlanService.getPlan(params.id as long))
    }

    def process() {
        Process process = processService.getProcess(params.id as long)
        [
                process                 : process,
                name                    : process.jobExecutionPlan.name,
                id                      : process.id,
                operatorIsAwareOfFailure: process.operatorIsAwareOfFailure,
                hasError                : processService.getError(process),
                planId                  : process.jobExecutionPlan.id,
                parameter               : processParameterData(process),
                comment                 : process.comment,
                restartedProcess        : getRestartedProcess(process),
                showRestartButton       : showRestartButton(process),
        ]
    }

    boolean showRestartButton(Process process) {
        return (RestartableStartJob.isAssignableFrom(Class.forName(process.getStartJobClass())) &&
                !Process.findByRestarted(process))
    }

    Process getRestartedProcess(Process process) {
        return Process.findByRestarted(process)
    }

    def updateOperatorIsAwareOfFailure(OperatorIsAwareOfFailureSubmitCommand cmd) {
        Process process = cmd.process
        processService.setOperatorIsAwareOfFailureWithAuthentication(process, cmd.operatorIsAwareOfFailure)
        redirect action: 'process', params: [id: process.id]
    }

    def processData(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        Process process = processService.getProcess(params.id as long)
        List<ProcessingStep> steps = processService.getAllProcessingSteps(process, cmd.iDisplayLength, cmd.iDisplayStart, "id", cmd.sortOrder)
        dataToRender.iTotalRecords = processService.getNumberOfProcessingSteps(process)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List<Map> data = Collections.synchronizedList([])
        Authentication auth = SecurityContextHolder.context.authentication
        List<Promise> promises = steps.collect { ProcessingStep processingStep ->
            task {
                SecurityContextHolder.context.authentication = auth
                try {
                    SessionUtils.withNewSession {
                        ProcessingStepUpdate update = processService.getLatestProcessingStepUpdate(processingStep)
                        ExecutionState state = update?.state

                        List<String> actions = []
                        if (state == ExecutionState.FAILURE && !Process.findByRestarted(process)) {
                            actions << "restart"
                        }
                        data << [
                                processingStep: [
                                        id      : processingStep.id,
                                        jobName : processingStep.jobDefinition.name,
                                        jobClass: processingStep.jobClass,
                                ],
                                times: [
                                    creation  : TimestampHelper.asTimestamp(processService.getFirstUpdate(processingStep)),
                                    lastUpdate: TimestampHelper.asTimestamp(update?.date),
                                    duration  : processService.getProcessingStepDuration(processingStep),
                                ],
                                lastUpdate: [
                                        state: state,
                                ],
                                actions: actions,
                        ]
                    }
                } finally {
                    SecurityContextHolder.context.authentication = null
                }
            }
        }

        waitAll(promises)

        dataToRender.aaData = data.sort { -it.processingStep.id }

        render dataToRender as JSON
    }

    def restartWithProcess(Process process) {
        StringBuilder stringBuilder = new StringBuilder()
        LogThreadLocal.withThreadLog(stringBuilder) {
            try {
                restartActionService.restartWorkflowWithProcess(process)
                flash.message = new FlashMessage(g.message(code: "processes.process.restartProcess.succeeded") as String)
            } catch (RuntimeException e) {
                log.debug("Restarting workflow failed.", e)
                flash.message = new FlashMessage(g.message(code: "processes.process.restartProcess.failed") as String, [e.message])
            }
        }
        log.debug("Output of restartActionService.restartWorkflowWithProcess: ${stringBuilder}")
        redirect action: "process", id: process.id
    }

    def processingStep() {
        ProcessingStep step = processService.getProcessingStep(params.id as long)
        return [
                step: step,
                hasLog: processService.processingStepLogExists(step),
                clusterJobs: ClusterJob.findAllByProcessingStep(step).sort { it.clusterJobId },
        ]
    }

    def processingStepLog(ProcessingStep step) {
        String log = processService.processingStepLog(step)
        return [
                log: log,
                step: step,
        ]
    }

    def processingStepClusterJobLog() {
        ClusterJob clusterJob = ClusterJob.findById(params.id as long)
        render contentType: "text/plain", text: processService.processingStepClusterJobLog(clusterJob)
    }

    def restartStep() {
        boolean ok = true
        String error = null
        try {
            processService.restartProcessingStep(processService.getProcessingStep(params.id as long))
        } catch (RuntimeException e) {
            ok = false
            error = e.message
        }
        Map data = [success: ok, error: error]
        render data as JSON
    }

    def processingStepDate(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        ProcessingStep step = processService.getProcessingStep(params.id as long)
        List<ProcessingStepUpdate> updates = processService.getAllUpdates(step, cmd.iDisplayLength, cmd.iDisplayStart, "id", cmd.sortOrder)
        dataToRender.iTotalRecords = processService.getNumberOfUpdates(step)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        updates.each { ProcessingStepUpdate update ->
            dataToRender.aaData << [
                update.id,
                TimestampHelper.asTimestamp(update.date),
                update.state,
                update.error,
            ]
        }
        render dataToRender as JSON
    }

    def parameterData(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        boolean input = Boolean.parseBoolean(params.input)
        ProcessingStep step = processService.getProcessingStep(params.id as long)
        String jobName
        // TODO: move into service
        List<Parameter> parameters = []
        if (input) {
            parameters = step.input.toList().sort { it.id }
            jobName = step.previous?.getClusterJobName()
        } else {
            parameters = step.output.toList().sort { it.id }
            jobName = step.getClusterJobName()
        }
        if (!cmd.sortOrder) {
            parameters = parameters.reverse()
        }
        dataToRender.iTotalRecords = parameters.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        parameters.each { Parameter param ->
            dataToRender.aaData << [
                param.id,
                param.type.name,
                param.type.description,
                formatParamValue(param),
                jobName,
            ]
        }
        render dataToRender as JSON
    }

    String formatParamValue(Parameter param) {
        if (param.type.name == JobParameterKeys.SCRIPT) {
            return "<pre>${param.value?.encodeAsHTML()}</pre>"
        } else {
            return param.value?.encodeAsHTML()
        }
    }

    def getProcessingErrorStackTrace() {
        render text: processService.getProcessingErrorStackTrace(params.id as long), contentType: "text/plain"
    }

    private Map<String,String> processParameterData(Process process) {
        ProcessParameter parameter = ProcessParameter.findByProcess(process)
        if (parameter) {
            if (parameter.className) {
                return [
                    controller: GrailsNameUtils.getShortName(parameter.className),
                    action: "show",
                    id: parameter.value,
                    text: parameter.toObject().toString(),
                ]
            } else {
                // not a class, just use the value
                return [text: parameter.value]
            }
        }
        return null
    }

    // params.id, params.comment, date
    def saveProcessComment(CommentCommand cmd) {
        Process process = processService.getProcess(cmd.id)
        commentService.saveComment(process, cmd.comment)
        def dataToRender = [date: process.comment.modificationDate.format('EEE, d MMM yyyy HH:mm'), author: process.comment.author]
        render dataToRender as JSON
    }
}

class OperatorIsAwareOfFailureSubmitCommand implements Serializable {
    Process process
    boolean operatorIsAwareOfFailure
}
