package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.job.processing.*
import grails.plugin.springsecurity.annotation.*
import org.springframework.validation.Errors

@Secured(['ROLE_ADMIN'])
class ShutdownController {
    ShutdownService shutdownService

    static allowedMethods = [
            index: "GET",
            planShutdown: "POST",
            cancelShutdown: "POST",
            closeApplication: "POST",
    ]

    def index() {
        ShutdownInformation shutdownInformation = shutdownService.currentPlannedShutdown
        if (shutdownInformation == null) {
            return [
                    shutdownSucceeded: shutdownService.shutdownSuccessful
            ]
        } else {
            List<ProcessingStep> runningJobs = shutdownService.runningJobs
            List<ProcessingStep> resumableJobs = []
            List<ProcessingStep> notResumableJobs = []
            runningJobs.each {
                if (shutdownService.isJobResumable(it)) {
                    resumableJobs << it
                } else {
                    notResumableJobs << it
                }
            }
            render(view: "status", model: [
                    shutdown: shutdownInformation,
                    resumableJobs: resumableJobs,
                    notResumableJobs: notResumableJobs,
            ])
        }
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
