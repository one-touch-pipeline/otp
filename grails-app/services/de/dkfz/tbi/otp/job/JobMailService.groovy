package de.dkfz.tbi.otp.job

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import org.codehaus.groovy.grails.commons.*
import org.joda.time.*

import java.text.*

class JobMailService {


    static final String PROCESSING_OPTION_EMAIL_RECIPIENT = "EmailRecipient"
    static final String PROCESSING_OPTION_STATISTIC_EMAIL_RECIPIENT = "Statistic"

    MailHelperService mailHelperService

    ProcessService processService

    GrailsApplication grailsApplication

    JobStatusLoggingService jobStatusLoggingService

    TrackingService trackingService


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


    public void sendErrorNotification(Job job, Throwable exceptionToBeHandled) {
        assert job: 'job may not be null'
        assert exceptionToBeHandled: 'exceptionToBeHandled may not be null'

        ProcessingStep step = ProcessingStep.getInstance(job.getProcessingStep().id)

        def object = step.processParameterObject
        if (!object) {
            return //general workflow, no processing
        }
        Collection<SeqTrack> seqTracks = object.containedSeqTracks
        String ilseNumbers = seqTracks*.ilseSubmission*.ilseNumber.unique().sort().join(', ')
        Collection<OtrsTicket> openTickets = seqTracks ? trackingService.findAllOtrsTickets(seqTracks).findAll { OtrsTicket otrsTicket ->
            !otrsTicket.finalNotificationSent
        } : []

        List<ClusterJob> clusterJobs = ClusterJob.findAllByProcessingStep(step)
        List<ClusterJobIdentifier> clusterJobIdentifiers = ClusterJobIdentifier.asClusterJobIdentifierList(clusterJobs)
        Collection<ClusterJobIdentifier> clusterJobIdentifiersToCheck = jobStatusLoggingService.failedOrNotFinishedClusterJobs(step, clusterJobIdentifiers)
        Collection<ClusterJob> clusterJobsToCheck = clusterJobIdentifiersToCheck.collect {
            ClusterJob.findByClusterJobIdentifier(it)
        }

        int restartedStepCount = restartCount(step)
        Process firstWorkflow = firstWorkflowJobId(step.process)

        StringBuilder message = new StringBuilder('''OTP job failed\ndata:\n''')

        Map otpWorkflow = [
                otpWorkflowId         : step.process.id,
                otpWorkflowStarted    : dateString(step.process.started),
                otpWorkflowName       : step.jobExecutionPlan.name,
                otpLink               : processService.processUrl(step.process),
                restartedWorkflowJobId: Process.findByRestarted(step.process)?.id,
                originWorkflowJobId   : firstWorkflow.id,
                originWorkflowStart   : dateString(firstWorkflow.started),
                objectInformation     : object.toString().replaceAll(/ ?<br> ?/, ' ').replaceAll(/\n/, ' '),
                ilseNumbers           : ilseNumbers,
                openTickets           : openTickets.join(', '),
        ]
        message << mapToString('Workflow', otpWorkflow)

        Map otpJob = [
                otpJobId           : step.id,
                otpJobStarted      : dateString(step.firstProcessingStepUpdate.date),
                otpJobFailed       : dateString(new Date()),
                restartedOtpJobId  : restartedStepCount ? ((RestartedProcessingStep) step).original.id.toString() : '',
                countOfJobRestarted: restartedStepCount,
        ]
        message << mapToString('OTP Job', otpJob)
        message << "  otpErrorMessage: ${exceptionToBeHandled.message}"

        clusterJobsToCheck.sort { ClusterJob clusterJob ->
            return clusterJob.id
        }.each { ClusterJob clusterJob ->
            Map clusterProperties = [
                    clusterId          : clusterJob.clusterJobId,
                    jobName            : clusterJob.clusterJobName,

                    queue              : dateString(clusterJob.queued),
                    start              : dateString(clusterJob.started),
                    ended              : dateString(clusterJob.ended),
                    runningHours       : clusterJob.started && clusterJob.ended ? clusterJob.getElapsedWalltime().standardHours : 'na',
                    logFile            : job.getLogFilePath(clusterJob),

                    clusterErrorMessage: '',
                    errorType          : '',
                    node               : '',
            ]

            message << mapToString('Cluster Job', clusterProperties)

            Map mapForLog = otpWorkflow + otpJob + clusterProperties
            log.info("""Error Statistic:
Failed ClusterJob Job Header: ${mapForLog.keySet().join(';')}
Failed ClusterJob Job Values: ${mapForLog.values().join(';')}""")
        }

        if (!clusterJobsToCheck) {
            Map mapForLog = otpWorkflow + otpJob + [otpErrorMessage: exceptionToBeHandled.message?.replaceAll('\n', ' ')]
            log.info("""Error Statistic:
Failed OTP Header: ${mapForLog.keySet().join(';')}
Failed OTP Values: ${mapForLog.values().join(';')}""")
        }

        String recipientsString = ProcessingOptionService.findOption(
                PROCESSING_OPTION_EMAIL_RECIPIENT,
                PROCESSING_OPTION_STATISTIC_EMAIL_RECIPIENT,
                object?.project,
        )
        if (recipientsString) {
            List<String> recipients = recipientsString.split(' ') as List
            String subject = "ERROR: Statistic information"

            mailHelperService.sendEmail(subject, message.toString(), recipients)
        }
    }


    private String dateString(DateTime date) {
        return dateString(date?.toDate())
    }

    private String dateString(Date date) {
        return date ? new SimpleDateFormat('yyyy-MM-dd HH:mm').format(date) : 'na'
    }

    int restartCount(ProcessingStep step) {
        return step instanceof RestartedProcessingStep ? restartCount(((RestartedProcessingStep) step).original) + 1 : 0
    }

    private String mapToString(String header, Map data) {
        return """

${header}:
${data.collect { key, value -> "  ${key}: ${value}" }.join('\n')}
"""
    }

    private Process firstWorkflowJobId(Process process) {
        Process previous = Process.findByRestarted(process)
        if (previous) {
            return firstWorkflowJobId(previous)
        } else {
            return process
        }
    }
}
