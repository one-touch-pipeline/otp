package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import grails.plugins.springsecurity.Secured

@Secured(['ROLE_ADMIN'])
class CrashRecoveryController {
    /**
     * Dependency Injection of CrashRecoveryService
     **/
    def crashRecoveryService
    /**
     * Dependency Injection of SchedulerService.
     * Required to restart the scheduler
     **/
    def schedulerService

    def index() {
        if (!crashRecoveryService.crashRecovery) {
            redirect action: "noRecovery"
        }
    }

    def noRecovery() {
        if (crashRecoveryService.crashRecovery) {
            redirect action: "index"
        }
    }

    def datatable(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        List crashedJobs = crashRecoveryService.crashedJobs()
        dataToRender.iTotalRecords = crashedJobs.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        // TODO: sorting
        crashedJobs.each { step ->
            dataToRender.aaData << [
                    step.id,
                    [id: step.process.jobExecutionPlan.id, name: step.process.jobExecutionPlan.name],
                    step.process.id,
                    [id: step.id, name: step.jobDefinition.name],
                    ["class": step.jobClass, version: step.jobVersion]
                ]
        }
        render dataToRender as JSON
    }

    def markFailed() {
        boolean success = true
        String error = null
        try {
            crashRecoveryService.markJobAsFailed(params.id as Long, params.message)
        } catch (RuntimeException e) {
            success = false
            error = e.message
        }
        def data = [success: success, error: error]
        render data as JSON
    }

    def restart() {
        boolean success = true
        String error = null
        try {
            crashRecoveryService.restartJob(params.id as Long, params.message)
        } catch (RuntimeException e) {
            success = false
            error = e.message
        }
        def data = [success: success, error: error]
        render data as JSON
    }

    def markFinished() {
        def data = performMarkAsFinished(false, params.id as Long, params)
        render data as JSON
    }

    def markSucceeded() {
        def data = performMarkAsFinished(true, params.id as Long, params)
        render data as JSON
    }

    def startScheduler() {
        if (!crashRecoveryService.crashRecovery) {
            def data = [success: false, error: "Not in Crash Recovery"]
            render data as JSON
            return
        }
        schedulerService.startup()
        def data = [success: !crashRecoveryService.crashRecovery]
        render data as JSON
    }

    def parametersOfJob() {
        [parameters: crashRecoveryService.getOutputParametersOfJob(params.id as Long)]
    }

    private performMarkAsFinished(boolean successOrFinished, Long processingStepId, Map params) {
        boolean success = true
        String error = null
        def jsonParameters = JSON.parse(params["parameters"])
        Map<String, String> parameters = [:]
        jsonParameters.each {
            parameters.put(it.key, it.value)
        }
        try {
            if (successOrFinished) {
                crashRecoveryService.markJobAsSucceeded(processingStepId, parameters)
            } else {
                crashRecoveryService.markJobAsFinished(processingStepId, parameters)
            }
        } catch (RuntimeException e) {
            success = false
            error = e.message
        }
        return [success: success, error: error]
    }
}
