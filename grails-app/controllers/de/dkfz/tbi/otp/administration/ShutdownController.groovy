/*
 * Copyright 2011-2019 The OTP authors
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

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.OtpException
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.util.TimeFormats

@PreAuthorize("hasRole('ROLE_ADMIN')")
class ShutdownController {
    ShutdownService shutdownService

    static allowedMethods = [
            index           : "GET",
            planShutdown    : "POST",
            cancelShutdown  : "POST",
            closeApplication: "POST",
    ]

    def index() {
        ShutdownInformation shutdownInformation = shutdownService.currentPlannedShutdown()
        if (shutdownInformation == null) {
            return [
                    shutdownSucceeded: shutdownService.shutdownSuccessful
            ]
        }
        List<ProcessingStep> runningJobs = shutdownService.runningJobs()
        List<ProcessingStep> resumableJobs = []
        List<ProcessingStep> notResumableJobs = []
        runningJobs.each {
            if (shutdownService.isJobResumable(it)) {
                resumableJobs << it
            } else {
                notResumableJobs << it
            }
        }

        List<WorkflowStep> restartableRunningWorkflowSteps = shutdownService.restartableRunningWorkflowSteps()
        List<WorkflowStep> notRestartableRunningWorkflowSteps = shutdownService.nonRestartableRunningWorkflowSteps()

        render(view: "status", model: [
                shutdown                          : shutdownInformation,
                shutdownInititated                : TimeFormats.DATE_TIME.getFormattedDate(shutdownInformation.initiated),
                resumableJobs                     : resumableJobs,
                notResumableJobs                  : notResumableJobs,
                restartableRunningWorkflowSteps   : restartableRunningWorkflowSteps,
                notRestartableRunningWorkflowSteps: notRestartableRunningWorkflowSteps,
        ])
    }

    def planShutdown(ShutdownCommand cmd) {
        try {
            Errors errors = shutdownService.planShutdown(cmd.reason)
            if (errors) {
                flash.message = new FlashMessage('Error while planning shutdown', errors)
            } else {
                flash.message = new FlashMessage("Server shutdown planned")
            }
        } catch (OtpException e) {
            log.error(e.message, e)
            flash.message = new FlashMessage("Server shutdown could not be scheduled", [e.message])
        }
        redirect(action: "index")
    }

    def cancelShutdown() {
        try {
            Errors errors = shutdownService.cancelShutdown()
            if (errors) {
                flash.message = new FlashMessage('Error while canceling shutdown', errors)
            } else {
                flash.message = new FlashMessage("Server shutdown canceled")
            }
        } catch (OtpException e) {
            log.error(e.message, e)
            flash.message = new FlashMessage("Server shutdown could not be canceled", [e.message])
        }
        redirect(action: "index")
    }

    def closeApplication() {
        try {
            shutdownService.destroy()
        } catch (Throwable e) {
            log.error(e.message, e)
            flash.message = new FlashMessage("Server could not be stopped", [e.message])
        }
        redirect(action: "index")
    }
}

class ShutdownCommand {
    String reason
}
