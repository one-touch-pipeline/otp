package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.utils.CommentCommand
import grails.converters.JSON
import grails.util.GrailsNameUtils
import org.springframework.security.core.context.SecurityContextHolder
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.utils.DataTableCommand

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
    ExecutionService executionService
    CommentService commentService

    def index() {
        redirect(action: 'list')
    }

    @SuppressWarnings("EmptyMethod")
    def list() {
    }

    def listData(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        List<JobExecutionPlan> plans = jobExecutionPlanService.getAllJobExecutionPlans()
        dataToRender.iTotalRecords = plans.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List futures = []
        def auth = SecurityContextHolder.context.authentication
        plans.each { JobExecutionPlan mainThreadPlan ->
            def planId = mainThreadPlan.id
            // spin off threads to fetch the data in parallel
            futures << callAsync {
                SecurityContextHolder.context.authentication = auth
                JobExecutionPlan plan = JobExecutionPlan.get(planId)
                Process lastSucceededProcess = jobExecutionPlanService.getLastSucceededProcess(plan)
                Process lastFailedProcess = jobExecutionPlanService.getLastFailedProcess(plan)
                Process lastFinishedProcess = jobExecutionPlanService.getLastFinishedProcess(plan)
                Map data = [:]
                data.put("planId", planId)
                data.put("lastSucceededProcessId", lastSucceededProcess?.id)
                data.put("lastFinishedProcessId", lastFinishedProcess?.id)
                data.put("succeeded", jobExecutionPlanService.getNumberOfSuccessfulFinishedProcesses(plan))
                data.put("finished", jobExecutionPlanService.getNumberOfFinishedProcesses(plan))
                data.put("numberOfProcesses", jobExecutionPlanService.getNumberOfProcesses(plan))
                data.put("lastSuccessDate", lastSucceededProcess ? processService.getFinishDate(lastSucceededProcess) : null)
                data.put("lastFailureDate", lastFailedProcess ? processService.getFinishDate(lastFailedProcess) : null)
                data.put("duration", lastFinishedProcess ? processService.getDuration(lastFinishedProcess) : null)
                return data
            }
        }
        futures.each { future ->
            def data = future.get()
            JobExecutionPlan plan = JobExecutionPlan.get(data.planId)
            dataToRender.aaData << [
                calculateStatus(plan, Process.get(data.lastSucceededProcessId), Process.get(data.lastFinishedProcessId)),
                data.finished > 0 ? [succeeded: data.succeeded, finished: data.finished] : null,
                [id: data.planId, name: plan.name],
                data.numberOfProcesses,
                data.finished - data.succeeded,
                data.lastSuccessDate,
                data.lastFailureDate,
                data.duration
            ]
        }
        // perform sorting on fetched data
        // this is acceptable, as we do not use pagination for the process overview
        // fetch the data with multiple queries, that means we cannot sort in the query directly
        // so we have to sort the fetched data
        dataToRender.aaData.sort { a, b ->
            switch (cmd.iSortCol_0) {
                case 0: // status
                    return a[0] <=> b[0]
                case 1: // succeeded/finished
                    if (a[1] && b[1]) {
                        return a[1].succeeded/a[1].finished <=> b[1].succeeded/b[1].finished
                    } else if (a[1]) {
                        return 1
                    } else if (b[1]) {
                        return -1
                    } else {
                        return 0
                    }
                case 3: // number of processes
                    return a[3] <=> b[3]
                case 4: // number of failed
                    return a[4] <=> b[4]
                case 5: // last succeeded
                    return a[5] <=> b[5]
                case 6: // last failed
                    return a[6] <=> b[6]
                case 7: // duration
                    return a[7] <=> b[7]
                case 2: // id -> default
                default:
                    return a[2].id <=> b[2].id
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
            if (latest.state == ExecutionState.FAILURE) {
                actions << "restart"
            }
            dataToRender.aaData << [
                process.id,
                stateForExecutionState(process, latest.state),
                parameterData,
                process.started,
                latest.date,
                latest.processingStep.jobDefinition.name,
                [state: latest.state, error: latest.error ? latest.error.errorMessage : null, id: latest.processingStep.id],
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
                name: process.jobExecutionPlan.name,
                id: process.id,
                planId: process.jobExecutionPlan.id,
                parameter: processParameterData(process),
                comment: process.comment
        ]
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
            if (data.state == ExecutionState.FAILURE) {
                actions << "restart"
            }
            dataToRender.aaData << [
                data.step.id,
                data.state,
                data.step.jobDefinition.name,
                data.step.jobClass ? [name: data.step.jobClass, version: data.step.jobVersion] : null,
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
            jobName = step.previous?.getPbsJobDescription()
        } else {
            parameters = step.output.toList().sort { it.id }
            jobName = step.getPbsJobDescription()
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

    private PlanStatus calculateStatus(JobExecutionPlan plan, Process lastSuccess, Process lastFinished) {
        if (!plan.enabled) {
            return PlanStatus.DISABLED
        }
        final boolean active = jobExecutionPlanService.isProcessRunning(plan)
        if (!lastFinished && !active) {
            return PlanStatus.NEW
        }
        if (!lastFinished && active) {
            return PlanStatus.RUNNING
        }
        if (lastFinished == lastSuccess) {
            return active ? PlanStatus.RUNNING : PlanStatus.SUCCESS
        }
        // now only failed are here
        return active ? PlanStatus.RUNNINGFAILEDBEFORE : PlanStatus.FAILURE
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
