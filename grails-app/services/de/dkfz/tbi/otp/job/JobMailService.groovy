package de.dkfz.tbi.otp.job

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.processing.ProcessService
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.utils.MailHelperService
import org.codehaus.groovy.grails.commons.GrailsApplication

class JobMailService {

    MailHelperService mailHelperService

    ProcessService processService

    GrailsApplication grailsApplication


    public void sendErrorNotificationIfFastTrack(ProcessingStep step, Throwable exceptionToBeHandled) {
        assert step: 'step may not be null'
        assert exceptionToBeHandled: 'exceptionToBeHandled may not be null'
        sendErrorNotificationIfFastTrack(step, exceptionToBeHandled.toString())
    }

    public void sendErrorNotificationIfFastTrack(ProcessingStep step, String errorMessage) {
        assert step: 'step may not be null'
        assert errorMessage: 'message may not be null'

        def object = step.processParameterObject
        if (!object || object.processingPriority < ProcessingPriority.FAST_TRACK_PRIORITY) {
            return
        }

        String jobExecutionPlanName = step.jobExecutionPlan.name
        String objectInformation = object.toString()
        String link = processService.processUrl(step.process)
        String recipient = grailsApplication.config.otp.mail.notification.fasttrack.to
        assert recipient

        String subject = "FASTTRACK ERROR: ${jobExecutionPlanName} ${object instanceof Run ? object.name : object.project?.name}"
        String message = """\
FastTrack failed:
workflow: ${jobExecutionPlanName}
object: ${objectInformation}
error: ${errorMessage}
link: ${link}
"""

        mailHelperService.sendEmail(subject, message, recipient)
    }
}
