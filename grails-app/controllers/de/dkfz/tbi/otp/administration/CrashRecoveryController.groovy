/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.administration

import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.utils.DataTableCommand

/**
 * @deprecated This is for the old workflow system. For the new workflow system there is the CrashRepairController with it's own view.
 * @see de.dkfz.tbi.otp.administration.CrashRepairController
 */
@Deprecated
@PreAuthorize("hasRole('ROLE_ADMIN')")
class CrashRecoveryController {

    CrashRecoveryService crashRecoveryService
    /** SchedulerService required to restart the scheduler */
    PropertiesValidationService propertiesValidationService
    SchedulerService schedulerService

    static allowedMethods = [
            index          : "GET",
            datatable      : "POST",
            markFailed     : "GET",
            restart        : "GET",
            markFinished   : "GET",
            markSucceeded  : "GET",
            startScheduler : "GET",
            parametersOfJob: "GET",
    ]

    def index() {
        boolean crashRecovery = crashRecoveryService.crashRecovery
        boolean processingOptionsValid = propertiesValidationService.validateProcessingOptions().isEmpty()
        return [
                crashRecovery         : crashRecovery,
                processingOptionsValid: processingOptionsValid,
        ]
    }

    def datatable(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        List<ProcessingStep> crashedJobs = crashRecoveryService.crashedJobs()
        dataToRender.iTotalRecords = crashedJobs.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        crashedJobs.each { ProcessingStep step ->
            boolean resumable = isJobResumable(step)
            dataToRender.aaData << [
                    step.id,
                    [id: step.process.jobExecutionPlan.id, name: step.process.jobExecutionPlan.name],
                    step.process.id,
                    [id: step.id, name: step.jobDefinition.name],
                    ["class": step.jobClass],
                    resumable,
            ]
        }
        render(dataToRender as JSON)
    }

    private boolean isJobResumable(ProcessingStep step) {
        Class jobClass = grailsApplication.classLoader.loadClass(step.jobClass)
        return jobClass.isAnnotationPresent(ResumableJob) || SometimesResumableJob.isAssignableFrom(jobClass)
    }

    def markFailed() {
        boolean success = true
        String error = null
        try {
            assert params.ids: 'No ids given'
            assert params.message: 'No message given'
            List<Long> ids = params.ids.split(',').collect { it as long }
            crashRecoveryService.markJobsAsFailed(ids, params.message)
        } catch (Throwable e) {
            success = false
            error = e.message
        }
        Map data = [success: success, error: error]
        render(data as JSON)
    }

    def restart() {
        boolean success = true
        String error = null
        try {
            assert params.ids: 'No ids given'
            assert params.message: 'No message given'
            List<Long> ids = params.ids.split(',').collect { it as long }
            crashRecoveryService.restartJobs(ids, params.message)
        } catch (Throwable e) {
            success = false
            error = e.message
        }
        Map data = [success: success, error: error]
        render(data as JSON)
    }

    def markFinished() {
        def data = performMarkAsFinished(false, params)
        render(data as JSON)
    }

    def markSucceeded() {
        def data = performMarkAsFinished(true, params)
        render(data as JSON)
    }

    def startScheduler() {
        if (!crashRecoveryService.crashRecovery) {
            Map data = [success: false, error: "Not in Crash Recovery"]
            render(data as JSON)
            return
        }
        schedulerService.startup()
        Map data = [success: !crashRecoveryService.crashRecovery]
        render(data as JSON)
    }

    def parametersOfJob() {
        List<Long> ids = params.ids.split(',').collect { it as long }
        [parametersPerJobs: crashRecoveryService.getOutputParametersOfJobs(ids)]
    }

    private performMarkAsFinished(boolean successOrFinished, Map params) {
        boolean success = true
        String error = null
        try {
            assert params.ids: 'No ids given'
            assert params.parameters: 'No parameters given'
            List<Long> ids = params.ids.split(',').collect { it as long }
            def jsonParameters = JSON.parse(params["parameters"])
            Map parameters = ids.collectEntries { [(it): [:]] }
            jsonParameters.each {
                String[] keys = it.key.split('!')
                assert keys.length == 2: "Expect 2 parts, found ${keys.length} parts, value: ${keys}"
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
