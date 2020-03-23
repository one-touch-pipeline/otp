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
package de.dkfz.tbi.otp.job

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.joda.time.DateTime

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.MailHelperService

import java.text.SimpleDateFormat

@Transactional
class JobMailService {

    MailHelperService mailHelperService

    ProcessService processService

    GrailsApplication grailsApplication

    JobStatusLoggingService jobStatusLoggingService

    OtrsTicketService otrsTicketService

    ProcessingOptionService processingOptionService


    void sendErrorNotification(Job job, Throwable exceptionToBeHandled) {
        assert job: 'job may not be null'
        assert exceptionToBeHandled: 'exceptionToBeHandled may not be null'
        sendErrorNotification(job, exceptionToBeHandled.message)
    }

    void sendErrorNotification(Job job, String errorMessage) {
        assert job: 'job may not be null'
        assert errorMessage: 'message may not be null'

        ProcessingStep step = ProcessingStep.getInstance(job.getProcessingStep().id)

        def object = step.processParameterObject
        String subjectPrefix = ""
        if (!object) {
            return //general workflow, no processing
        } else if (object.processingPriority >= ProcessingPriority.FAST_TRACK.priority) {
            subjectPrefix = "FASTTRACK "
        }
        Collection<SeqTrack> seqTracks = object.containedSeqTracks
        String ilseNumbers = seqTracks*.ilseSubmission*.ilseNumber.unique().sort().join(', ')
        Collection<String> openTickets = seqTracks ? otrsTicketService.findAllOtrsTickets(seqTracks).findAll { OtrsTicket otrsTicket ->
            !otrsTicket.finalNotificationSent
        }*.getUrl() : []

        List<ClusterJob> clusterJobs = ClusterJob.findAllByProcessingStep(step)
        List<ClusterJobIdentifier> clusterJobIdentifiers = ClusterJobIdentifier.asClusterJobIdentifierList(clusterJobs)
        Collection<ClusterJobIdentifier> clusterJobIdentifiersToCheck = jobStatusLoggingService.failedOrNotFinishedClusterJobs(step, clusterJobIdentifiers)
        Collection<ClusterJob> clusterJobsToCheck = clusterJobIdentifiersToCheck.collect {
            ClusterJob.findByClusterJobIdentifier(it, step)
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
        message << "  otpErrorMessage: ${errorMessage}"

        clusterJobsToCheck.sort { ClusterJob clusterJob ->
            return clusterJob.id
        }.each { ClusterJob clusterJob ->
            Map clusterProperties = [
                    clusterId          : clusterJob.clusterJobId,
                    jobName            : clusterJob.clusterJobName,

                    queue              : dateString(clusterJob.queued),
                    eligible           : dateString(clusterJob.eligible), //time when job is ready for start (no hold anymore)
                    start              : dateString(clusterJob.started),
                    ended              : dateString(clusterJob.ended),
                    runningHours       : clusterJob.started && clusterJob.ended ? clusterJob.getElapsedWalltime().standardHours : 'na',
                    logFile            : clusterJob.jobLog,

                    exitStatus         : clusterJob.exitStatus,
                    exitCode           : clusterJob.exitCode,
                    node               : clusterJob.node,
                    clusterStartCount  : clusterJob.startCount,
            ]

            message << mapToString('Cluster Job', clusterProperties)

            Map mapForLog = otpWorkflow + otpJob + clusterProperties
            log.info("""Error Statistic:
Failed ClusterJob Job Header: ${mapForLog.keySet().join(';')}
Failed ClusterJob Job Values: ${mapForLog.values().join(';')}""")
        }

        if (!clusterJobsToCheck) {
            Map mapForLog = otpWorkflow + otpJob + [otpErrorMessage: errorMessage?.replaceAll('\n', ' ')]
            log.info("""Error Statistic:
Failed OTP Header: ${mapForLog.keySet().join(';')}
Failed OTP Values: ${mapForLog.values().join(';')}""")
        }

        String recipientsString = processingOptionService.findOptionAsString(OptionName.EMAIL_RECIPIENT_ERRORS)
        if (recipientsString) {
            List<String> recipients = recipientsString.split(' ') as List
            String subject = "${subjectPrefix}ERROR: ${otpWorkflow.otpWorkflowName} ${object.individual?.displayName} ${object.project?.name}"

            mailHelperService.sendEmail(subject, message.toString(), recipients)
        }
    }


    private String dateString(DateTime date) {
        return dateString(date?.toDate())
    }

    private String dateString(Date date) {
        return date ? new SimpleDateFormat('yyyy-MM-dd HH:mm', Locale.ENGLISH).format(date) : 'na'
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
