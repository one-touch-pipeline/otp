package de.dkfz.tbi.otp.administration

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN'])
class ShutdownController {
    ShutdownService shutdownService

    def index() {
        if (shutdownService.shutdownPlanned) {
            redirect action: "status"
        }
    }

    def status() {
        def shutdownInformation = shutdownService.currentPlannedShutdown
        if (!shutdownInformation) {
            redirect action: "index"
        }
        List runningJobs = shutdownService.runningJobs
        List resumableJobs = []
        List notResumableJobs = []
        runningJobs.each {
            if (shutdownService.isJobResumable(it)) {
                resumableJobs << it
            } else {
                notResumableJobs << it
            }
        }
        [shutdown: shutdownInformation, resumableJobs: resumableJobs, notResumableJobs: notResumableJobs]
    }

    def planShutdown() {
        boolean ok = false
        try {
            shutdownService.planShutdown(params.reason)
            ok = true
        } catch (RuntimeException e) {
            println(e.message)
            e.printStackTrace()
            ok = false
        }
        Map data = [success: ok]
        render data as JSON
    }

    def cancelShutdown() {
        boolean ok = false
        try {
            shutdownService.cancelShutdown()
            ok = true
        } catch (RuntimeException e) {
            println(e.message)
            e.printStackTrace()
            ok = false
        }
        Map data = [success: ok]
        render data as JSON
    }

    def closeApplication() {
        shutdownService.destroy()
        Map data = [shutdown: true]
        render data as JSON
    }
}
