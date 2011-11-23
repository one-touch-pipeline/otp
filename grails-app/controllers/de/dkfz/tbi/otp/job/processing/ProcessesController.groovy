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
        dataToRender.iTotalDisplayRecords = plans.size()
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

        JobExecutionPlan plan = jobExecutionPlanService.getPlan(params.id as long)
        List<JobExecutionPlan> processes = jobExecutionPlanService.getAllProcesses(plan)
        dataToRender.iTotalRecords = processes.size()
        dataToRender.iTotalDisplayRecords = processes.size()
        dataToRender.offset = start
        dataToRender.iSortCol_0 = params.iSortCol_0
        dataToRender.sSortDir_0 = params.sSortDir_0

        // TODO: sorting
        processes.reverse().each { Process process ->
            ProcessingStep latest = processService.getLatestProcessingStep(process)
            ExecutionState lastState = processService.getState(process)
            dataToRender.aaData << [
                process.id,
                stateForExecutionState(process, lastState),
                process.started,
                processService.getLastUpdate(latest),
                latest.jobDefinition.name,
                lastState
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
