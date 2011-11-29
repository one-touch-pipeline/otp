package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import grails.converters.JSON

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
        plans.each { JobExecutionPlan plan ->
            Process lastSuccess = jobExecutionPlanService.getLastSucceededProcess(plan)
            Process lastFailure = jobExecutionPlanService.getLastFailedProcess(plan)
            Process lastFinished = jobExecutionPlanService.getLastFinishedProcess(plan)
            dataToRender.aaData << [
                calculateStatus(plan, lastSuccess, lastFailure, lastFinished),
                '2', // TODO: health value
                [id: plan.id, name: plan.name],
                jobExecutionPlanService.getNumberOfProcesses(plan),
                lastSuccess ? processService.getFinishDate(lastSuccess) : null,
                lastFailure ? processService.getFinishDate(lastFailure) : null,
                lastFinished ? processService.getDuration(lastFinished) : null
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
        List<JobExecutionPlan> processes = jobExecutionPlanService.getAllProcesses(plan, length, start, sort, sortOrder)
        dataToRender.iTotalRecords = jobExecutionPlanService.getNumberOfProcesses(plan)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.offset = start
        dataToRender.iSortCol_0 = sortColumn
        dataToRender.sSortDir_0 = sortOrder ? "asc" : "desc"

        // TODO: sorting for additional columns
        processes.each { Process process ->
            ProcessingStep latest = processService.getLatestProcessingStep(process)
            ExecutionState lastState = processService.getState(process)
            dataToRender.aaData << [
                process.id,
                stateForExecutionState(process, lastState),
                process.started,
                processService.getLastUpdate(latest),
                latest.jobDefinition.name,
                [state: lastState, error: processService.getError(process), id: latest.id]
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
            dataToRender.aaData << [
                step.id,
                state, // last reached status
                step.jobDefinition.name,
                step.jobClass ? [name: step.jobClass, version: step.jobVersion] : null,
                processService.getFirstUpdate(step), // started
                processService.getLastUpdate(step), // last update
                processService.getProcessingStepDuration(step), // duration
                [state: processService.getState(step), error: processService.getError(step)]
            ]
        }
        render dataToRender as JSON
    }

    def processingStep() {
        ProcessingStep step = processService.getProcessingStep(params.id as long)
        [step: step]
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

    private PlanStatus calculateStatus(JobExecutionPlan plan, Process lastSuccess, Process lastFailure, Process lastFinished) {
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
