package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

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
            assert params.ids : 'No ids given'
            assert params.message : 'No message given'
            List<Long> ids = params.ids.split(',').collect{it as long}
            crashRecoveryService.markJobsAsFailed(ids, params.message)
        } catch (Throwable e) {
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
            assert params.ids : 'No ids given'
            assert params.message : 'No message given'
            List<Long> ids = params.ids.split(',').collect{it as long}
            crashRecoveryService.restartJobs(ids, params.message)
        } catch (Throwable e) {
            success = false
            error = e.message
        }
        def data = [success: success, error: error]
        render data as JSON
    }

    def markFinished() {
        def data = performMarkAsFinished(false, params)
        render data as JSON
    }

    def markSucceeded() {
        def data = performMarkAsFinished(true, params)
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
        List<Long> ids = params.ids.split(',').collect{it as long}
        [parametersPerJobs: crashRecoveryService.getOutputParametersOfJobs(ids)]
    }

    private performMarkAsFinished(boolean successOrFinished, Map params) {
        boolean success = true
        String error = null
        try {
            assert params.ids : 'No ids given'
            assert params.parameters : 'No parameters given'
            List<Long> ids = params.ids.split(',').collect{it as long}
            def jsonParameters = JSON.parse(params["parameters"])
            Map parameters = ids.collectEntries{[(it): [:]]}
            jsonParameters.each {
                String[] keys = it.key.split('!')
                assert keys.length == 2 : "Expect 2 parts, found ${keys.length} parts, value: ${keys}"
                parameters.get(keys[0] as long).put(keys[1], it.value)
            }

            if (successOrFinished) {
                crashRecoveryService.markJobsAsSucceeded(ids, parameters)
            } else {
                crashRecoveryService.markJobsAsFinished(ids, parameters)
            }
        } catch (Throwable e) {
            success = false
            error = e.message
        }
        return [success: success, error: error]
    }
}
