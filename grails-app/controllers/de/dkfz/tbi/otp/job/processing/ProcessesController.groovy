package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import grails.converters.JSON
import grails.util.GrailsNameUtils
import org.springframework.security.core.context.SecurityContextHolder


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
    def jobExecutionPlanService
    def processService

    def index() {
        redirect(action: 'list')
    }

    @SuppressWarnings("EmptyMethod")
    def list() {
    }

    def listData() {
        // input validation
        int start = 0
        int length = 10
        if (params.iDisplayStart) {
            start = params.iDisplayStart as int
        }
        if (params.iDisplayLength) {
            length = Math.min(100, params.iDisplayLength as int)
        }
        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []

        List<JobExecutionPlan> plans = jobExecutionPlanService.getAllJobExecutionPlans()
        dataToRender.iTotalRecords = plans.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.offset = start
        dataToRender.iSortCol_0 = params.iSortCol_0
        dataToRender.sSortDir_0 = params.sSortDir_0

        // TODO: sorting
        List futures = []
        def auth = SecurityContextHolder.context.authentication
        plans.each { JobExecutionPlan plan ->
            // spin off threads to fetch the data in parallel
            futures << callAsync {
                SecurityContextHolder.context.authentication = auth
                Map data = [:]
                data.put("plan", plan)
                data.put("lastSuccess", jobExecutionPlanService.getLastSucceededProcess(plan))
                data.put("lastFailure", jobExecutionPlanService.getLastFailedProcess(plan))
                data.put("lastFinished", jobExecutionPlanService.getLastFinishedProcess(plan))
                data.put("succeeded", jobExecutionPlanService.getNumberOfSuccessfulFinishedProcesses(plan))
                data.put("finished", jobExecutionPlanService.getNumberOfFinishedProcesses(plan))
                data.put("numberOfProcesses", jobExecutionPlanService.getNumberOfProcesses(plan))
                data.put("lastSuccessDate", data.lastSuccess ? processService.getFinishDate(data.lastSuccess) : null)
                data.put("lastFailureDate", data.lastFailure ? processService.getFinishDate(data.lastFailure) : null)
                data.put("duration", data.lastFinished ? processService.getDuration(data.lastFinished) : null)
                return data
            }
        }
        futures.each { future ->
            def data = future.get()
            dataToRender.aaData << [
                calculateStatus(data.plan, data.lastSuccess, data.lastFinished),
                data.finished > 0 ? [succeeded: data.succeeded, finished: data.finished] : null,
                [id: data.plan.id, name: data.plan.name],
                data.numberOfProcesses,
                data.lastSuccessDate,
                data.lastFailureDate,
                data.duration
            ]
        }
        render dataToRender as JSON
    }

    def plan() {
        JobExecutionPlan plan = jobExecutionPlanService.getPlan(params.id as long)
        [name: plan.name, id: plan.id]
    }

    def planData() {
        // input validation
        int start = 0
        int length = 10
        if (params.iDisplayStart) {
            start = params.iDisplayStart as int
        }
        if (params.iDisplayLength) {
            length = Math.min(100, params.iDisplayLength as int)
        }
        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []
        int sortColumn = 0
        if (params.iSortCol_0) {
            sortColumn = params.iSortCol_0
        }
        String sort
        switch (sortColumn) {
        case 2:
            sort = "started"
            break
        case 0:
        default:
            sort = "id"
            break
        }
        boolean sortOrder = false
        if (params.sSortDir_0) {
            if (params.sSortDir_0 == "asc") {
                sortOrder = true
            } else if (params.sSortDir_0 == "desc") {
                sortOrder = false
            }
        }

        JobExecutionPlan plan = jobExecutionPlanService.getPlan(params.id as long)
        Map<Process, ProcessingStepUpdate> processes = jobExecutionPlanService.getLatestUpdatesForPlan(plan, length, start, sort, sortOrder)
        dataToRender.iTotalRecords = jobExecutionPlanService.getNumberOfProcesses(plan)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.offset = start
        dataToRender.iSortCol_0 = sortColumn
        dataToRender.sSortDir_0 = sortOrder ? "asc" : "desc"

        processes.each { Process process, ProcessingStepUpdate latest ->
            ProcessParameter parameter = ProcessParameter.findByProcess(process)
            String parameterData = null
            if (parameter) {
                if (parameter.className) {
                    def object = ProcessParameter.executeQuery("FROM " + parameter.className + " WHERE id=" + parameter.value)
                    parameterData = g.link(controller: GrailsNameUtils.getShortName(parameter.className), action: "show", id: parameter.value) { object[0].toString() }
                } else {
                    // not a class, just use the value
                    parameterData = parameter.value
                }
            }
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
                [actions: actions]
            ]
        }
        render dataToRender as JSON
    }

    def process() {
        Process process = processService.getProcess(params.id as long)
        [name: process.jobExecutionPlan.name, id: process.id, planId: process.jobExecutionPlan.id]
    }

    def processData() {
        // input validation
        int start = 0
        int length = 10
        if (params.iDisplayStart) {
            start = params.iDisplayStart as int
        }
        if (params.iDisplayLength) {
            length = Math.min(100, params.iDisplayLength as int)
        }
        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []
        boolean sortOrder = false
        if (params.sSortDir_0) {
            if (params.sSortDir_0 == "asc") {
                sortOrder = true
            } else if (params.sSortDir_0 == "desc") {
                sortOrder = false
            }
        }

        Process process = processService.getProcess(params.id as long)
        List<ProcessingStep> steps = processService.getAllProcessingSteps(process, length, start, "id", sortOrder)
        dataToRender.iTotalRecords = processService.getNumberOfProcessessingSteps(process)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.offset = start
        dataToRender.iSortCol_0 = 0
        dataToRender.sSortDir_0 = sortOrder ? "asc" : "desc"
        steps.each { ProcessingStep step ->
            ExecutionState state = processService.getState(step)
            def actions = []
            if (state == ExecutionState.FAILURE) {
                actions << "restart"
            }
            dataToRender.aaData << [
                step.id,
                state, // last reached status
                step.jobDefinition.name,
                step.jobClass ? [name: step.jobClass, version: step.jobVersion] : null,
                processService.getFirstUpdate(step), // started
                processService.getLastUpdate(step), // last update
                processService.getProcessingStepDuration(step), // duration
                [state: state, error: processService.getError(step)],
                [actions: actions]
            ]
        }
        render dataToRender as JSON
    }

    def processingStep() {
        ProcessingStep step = processService.getProcessingStep(params.id as long)
        [step: step]
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

    def processingStepDate() {
        // input validation
        int start = 0
        int length = 10
        if (params.iDisplayStart) {
            start = params.iDisplayStart as int
        }
        if (params.iDisplayLength) {
            length = Math.min(100, params.iDisplayLength as int)
        }
        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []
        boolean sortOrder = false
        if (params.sSortDir_0) {
            if (params.sSortDir_0 == "asc") {
                sortOrder = true
            } else if (params.sSortDir_0 == "desc") {
                sortOrder = false
            }
        }

        ProcessingStep step = processService.getProcessingStep(params.id as long)
        List<ProcessingStepUpdate> updates = processService.getAllUpdates(step, length, start, "id", sortOrder)
        dataToRender.iTotalRecords = processService.getNumberOfUpdates(step)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.offset = start
        dataToRender.iSortCol_0 = 0
        dataToRender.sSortDir_0 = sortOrder ? "asc" : "desc"
        updates.each { ProcessingStepUpdate update ->
            dataToRender.aaData << [
                update.id,
                update.date,
                update.state,
                update.error ? update.error.errorMessage : null
            ]
        }
        render dataToRender as JSON
    }

    def parameterData() {
        // input validation
        int start = 0
        int length = 10
        if (params.iDisplayStart) {
            start = params.iDisplayStart as int
        }
        if (params.iDisplayLength) {
            length = Math.min(100, params.iDisplayLength as int)
        }
        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []
        boolean sortOrder = true
        if (params.sSortDir_0) {
            if (params.sSortDir_0 == "asc") {
                sortOrder = true
            } else if (params.sSortDir_0 == "desc") {
                sortOrder = false
            }
        }
        boolean input = Boolean.parseBoolean(params.input)

        ProcessingStep step = processService.getProcessingStep(params.id as long)
        // TODO: move into service
        List<Parameter> parameters = []
        if (input) {
            parameters = step.input.toList().sort { it.id }
        } else {
            parameters = step.output.toList().sort { it.id }
        }
        if (!sortOrder) {
            parameters = parameters.reverse()
        }
        dataToRender.iTotalRecords = parameters.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.offset = start
        dataToRender.iSortCol_0 = 0
        dataToRender.sSortDir_0 = sortOrder ? "asc" : "desc"
        parameters.each { Parameter param ->
            dataToRender.aaData << [
                param.id,
                param.type.name,
                param.type.description,
                param.value
            ]
        }
        render dataToRender as JSON
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
}
