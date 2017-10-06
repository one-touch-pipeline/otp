package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.restarting.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.converters.*
import grails.util.*
import groovyx.gpars.*
import org.springframework.security.core.*
import org.springframework.security.core.context.*

import java.util.concurrent.*

@SuppressWarnings("GrailsPublicControllerMethod")
class ProcessesController {
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
        dataToRender.iTotalRecords = plans.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        Map<String, Long> processCounts
        Map<String, Long> successProcessCounts
        Map<String, Long> finishedProcessCounts
        Map<String, Long> failedProcessCounts
        Map<String, Date> lastSuccessDates
        Map<String, Date> lastFailureDates

        Authentication auth = SecurityContextHolder.context.authentication

        GParsPool.withPool() {
            Future failedProcessesCount = {
                SecurityContextHolder.context.authentication = auth
                try {
                    jobExecutionPlanService.failedProcessCount()
                } finally {
                    SecurityContextHolder.context.authentication = null
                }
            }.async()()

            Closure lastSuccessDatesClosure = {
                SecurityContextHolder.context.authentication = auth
                try {
                    jobExecutionPlanService.lastProcessDate(ExecutionState.SUCCESS)
                } finally {
                    SecurityContextHolder.context.authentication = null
                }
            }
            Closure lastSuccessDatesClosureAsync = lastSuccessDatesClosure.async()
            Future lastSuccessDatesFuture = lastSuccessDatesClosureAsync()

            Closure lastFailureDatesClosure = {
                SecurityContextHolder.context.authentication = auth
                try {
                    jobExecutionPlanService.lastProcessDate(ExecutionState.FAILURE)
                } finally {
                    SecurityContextHolder.context.authentication = null
                }
            }
            Closure lastFailureDatesClosureAsync = lastFailureDatesClosure.async()
            Future lastFailureDatesFuture = lastFailureDatesClosureAsync()

            processCounts = jobExecutionPlanService.processCount()
            finishedProcessCounts = jobExecutionPlanService.finishedProcessCount()
            failedProcessCounts = failedProcessesCount.get()
            lastSuccessDates = lastSuccessDatesFuture.get()
            lastFailureDates = lastFailureDatesFuture.get()

            dataToRender.aaData = plans.collectParallel { plan ->
                long allProcessesCount = processCounts[plan.name] ?: 0L
                long finishedProcessesCount = finishedProcessCounts[plan.name] ?: 0L
                long failedProcessCount = failedProcessCounts[plan.name] ?: 0L
                Date successDate = lastSuccessDates[plan.name]
                Date failureDate = lastFailureDates[plan.name]

                [
                        name                    : plan.name,
                        id                      : plan.id,
                        enabled                 : plan.enabled,
                        allProcessesCount       : allProcessesCount,
                        finishedProcessesCount  : finishedProcessesCount,
                        failedProcessesCount    : failedProcessCount,
                        runningProcessesCount   : allProcessesCount - finishedProcessesCount,
                        lastSuccessfulDate      : successDate,
                        lastFailureDate         : failureDate,
                ]
            }
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
                    return a.lastSuccessfulDate <=> b.lastSuccessfulDate
                case 5: // last failed
                    return a.lastFailureDate <=> b.lastFailureDate
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
        [name: plan.name, id: plan.id, failed: Boolean.parseBoolean(params.failed), enabled: plan.enabled]
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
        ExecutionState restriction = null
        if (Boolean.parseBoolean(params.failed)) {
            restriction = ExecutionState.FAILURE
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
                stateForExecutionState(process, latest.state),
                parameterData,
                process.started,
                latest.date,
                latest.processingStep.jobDefinition.name,
                [state: latestState, error: latest.error ? latest.error.errorMessage : null, id: latest.processingStep.id],
                process.comment?.comment?.encodeAsHTML(),
                [actions: actions]
            ]
        }
        render dataToRender as JSON
    }

    def planVisualization() {
        JobExecutionPlan plan = jobExecutionPlanService.getPlan(params.id as long)
        render jobExecutionPlanService.planInformation(plan) as JSON
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
                process: process,
                name: process.jobExecutionPlan.name,
                id: process.id,
                operatorIsAwareOfFailure: process.operatorIsAwareOfFailure,
                hasError: processService.getError(process),
                planId: process.jobExecutionPlan.id,
                parameter: processParameterData(process),
                comment: process.comment,
                restartedProcess: getRestartedProcess(process),
                showRestartButton: showRestartButton(process),
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
        dataToRender.iTotalRecords = processService.getNumberOfProcessessingSteps(process)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        List futures = []
        def auth = SecurityContextHolder.context.authentication
        steps.each { ProcessingStep step ->
            // spin off threads to fetch the data in parallel
            futures << callAsync {
                SecurityContextHolder.context.authentication = auth
                Map data = [:]
                ProcessingStepUpdate update = processService.getLatestProcessingStepUpdate(step)
                data.put("step", step)
                data.put("state", update?.state)
                data.put("firstUpdate", processService.getFirstUpdate(step))
                data.put("lastUpdate", update?.date)
                data.put("duration", processService.getProcessingStepDuration(step))
                data.put("error", update?.error?.errorMessage)
                return data
            }
        }
        futures.each { future ->
            def data = future.get()
            def actions = []
            if (data.state == ExecutionState.FAILURE && !Process.findByRestarted(process)) {
                actions << "restart"
            }
            dataToRender.aaData << [
                data.step.id,
                data.state,
                data.step.jobDefinition.name,
                data.step.jobClass ? [name: data.step.jobClass] : null,
                data.firstUpdate,
                data.lastUpdate,
                data.duration,
                [state: data.state, error: data.error],
                [actions: actions]
            ]
        }
        render dataToRender as JSON
    }

    def processVisualization() {
        Process process = processService.getProcess(params.id as long)
        render processService.processInformation(process) as JSON
    }

    def restartWithProcess() {
        def data = [success: true]
        StringBuilder stringBuilder = new StringBuilder()
        LogThreadLocal.withThreadLog(stringBuilder) {
            try {
                restartActionService.restartWorkflowWithProcess(params.id as long)
            } catch (RuntimeException e) {
                data = [success: false, error: e.message]
            }
        }
        log.debug("Output of restartActionService.restartWorkflowWithProcess: ${stringBuilder}")
        render data as JSON
    }

    def processingStep() {
        ProcessingStep step = processService.getProcessingStep(params.id as long)
        [step: step, hasLog: processService.processingStepLogExists(step)]
    }

    def processingStepLog() {
        ProcessingStep step = processService.getProcessingStep(params.id as long)
        render processService.processingStepLog(step)
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
        def data = [success: ok, error: error]
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
                update.date,
                update.state,
                update.error
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
                jobName
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

    private PlanStatus stateForExecutionState(Process process, ExecutionState state) {
        if (process.finished && state == ExecutionState.SUCCESS) {
            return PlanStatus.SUCCESS
        }
        if (process.finished && state == ExecutionState.FAILURE) {
            return PlanStatus.FAILURE
        }
        return PlanStatus.RUNNING
    }

    private Map<String,String> processParameterData(Process process) {
        ProcessParameter parameter = ProcessParameter.findByProcess(process)
        if (parameter) {
            if (parameter.className) {
                return [
                    controller: GrailsNameUtils.getShortName(parameter.className),
                    action: "show",
                    id: parameter.value,
                    text: parameter.toObject().toString()
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
