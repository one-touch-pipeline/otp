/*
 * Copyright 2011-2021 The OTP authors
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
import grails.validation.Validateable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.util.TimeFormats

@PreAuthorize("hasRole('ROLE_ADMIN')")
class CrashRepairController implements CheckAndCall {

    static allowedMethods = [
            index                       : "GET",
            runningWorkflowSteps        : "GET",
            restartWorkflowStep         : "POST",
            restartWorkflowRun          : "POST",
            markWorkflowStepAsFailed    : "POST",
            markWorkflowRunAsFinalFailed: "POST",
            startWorkflowSystem         : "POST",
    ]

    WorkflowStateChangeService workflowStateChangeService
    JobService jobService
    WorkflowService workflowService
    WorkflowSystemService workflowSystemService
    PropertiesValidationService propertiesValidationService
    CrashRepairService crashRepairService

    def index() {
        boolean processingOptionsValid = propertiesValidationService.validateProcessingOptions().isEmpty()

        return [
                workflowSystemEnabled : workflowSystemService.isEnabled(),
                processingOptionsValid: processingOptionsValid,
        ]
    }

    /**
     * GET all still running workflow steps after crash.
     * @return JSON list of failed workflow steps
     */
    def runningWorkflowSteps() {
        List<WorkflowStep> workflowSteps = crashRepairService.findStillRunningWorkflowStepsAfterCrash()
        workflowSteps.each { it.lastUpdated = TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(it.lastUpdated) }
        render(workflowSteps as JSON)
    }

    /**
     * Restart a given workflow step.
     * @param cmd WorkflowStepCommand
     * @return error/success msg
     */
    def restartWorkflowStep(WorkflowStepCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                jobService.createRestartedJobAfterSystemRestart(cmd.workflowStep)
                render([:] as JSON)
            } catch (WorkflowException workflowException) {
                log.error(workflowException.message)
                return response.sendError(HttpStatus.BAD_REQUEST.value(), workflowException.message)
            }
        }
    }

    /**
     * Restart a workflow run of a given workflow step.
     * @param cmd WorkflowStepCommand
     * @return error/success msg
     */
    def restartWorkflowRun(WorkflowStepCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                crashRepairService.restartWorkflowRun(cmd.workflowStep)
                render([:] as JSON)
            } catch (WorkflowException workflowException) {
                log.error(workflowException.message)
                return response.sendError(HttpStatus.BAD_REQUEST.value(), workflowException.message)
            }
        }
    }

    /**
     * Mark a workflow step as failed.
     * @param cmd WorkflowStepCommand
     */
    def markWorkflowStepAsFailed(WorkflowStepCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                workflowStateChangeService.changeStateToFailedWithManualChangedError(cmd.workflowStep)
                render([:] as JSON)
            } catch (WorkflowException workflowException) {
                log.error(workflowException.message)
                return response.sendError(HttpStatus.BAD_REQUEST.value(), workflowException.message)
            }
        }
    }

    /**
     * Mark a workflow step as final failed.
     * @param cmd WorkflowStepCommand
     * @return error/success msg
     */
    def markWorkflowRunAsFinalFailed(WorkflowStepCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                workflowStateChangeService.changeRunToFinalFailed(cmd.workflowStep)
                render([:] as JSON)
            } catch (WorkflowException workflowException) {
                log.error(workflowException.message)
                return response.sendError(HttpStatus.BAD_REQUEST.value(), workflowException.message)
            }
        }
    }

    /**
     * Start the workflow system.
     * @return error/success msg
     */
    def startWorkflowSystem() {
        try {
            workflowSystemService.startWorkflowSystem()
            render([:] as JSON)
        } catch (WorkflowException workflowException) {
            return response.sendError(HttpStatus.BAD_REQUEST.value(), workflowException.message)
        }
    }
}

class WorkflowStepCommand implements Validateable {
    WorkflowStep workflowStep
}
