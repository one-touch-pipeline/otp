package de.dkfz.tbi.otp.administration

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

    def datatable() {
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

        List crashedJobs = crashRecoveryService.crashedJobs()
        dataToRender.iTotalRecords = crashedJobs.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.offset = start
        dataToRender.iSortCol_0 = params.iSortCol_0
        dataToRender.sSortDir_0 = params.sSortDir_0

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
}
