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
package de.dkfz.tbi.otp.job

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobDetailService
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.util.TimeFormats

import java.time.ZonedDateTime

@CompileDynamic
@Transactional
class JobMailService {

    MailHelperService mailHelperService

    ProcessService processService

    JobStatusLoggingService jobStatusLoggingService

    TicketService ticketService

    ClusterJobService clusterJobService

    ClusterJobDetailService clusterJobDetailService

    void sendErrorNotification(Job job, String errorMessage) {
        assert job: 'job may not be null'
        assert errorMessage: 'message may not be null'

        ProcessingStep step = ProcessingStep.getInstance(job.processingStep.id)

        ProcessParameterObject object = step.processParameterObject
        if (!object) {
            return // general workflow, no processing
        }
        String subjectPrefix = object.processingPriority?.errorMailPrefix ?: "ERROR"
        Collection<SeqTrack> seqTracks = object.containedSeqTracks
        String ilseNumbers = seqTracks*.ilseSubmission*.ilseNumber.unique().sort().join(', ')
        Set<Ticket> openTickets = seqTracks ? ticketService.findAllTickets(seqTracks).findAll { Ticket ticket ->
            !ticket.finalNotificationSent
        } : []
        Set<String> openTicketsUrl = openTickets.collect {
            ticketService.buildTicketDirectLink(it)
        }

        List<ClusterJob> clusterJobs = ClusterJob.findAllByProcessingStep(step)
        List<ClusterJobIdentifier> clusterJobIdentifiers = ClusterJobIdentifier.asClusterJobIdentifierList(clusterJobs)
        Collection<ClusterJobIdentifier> clusterJobIdentifiersToCheck = jobStatusLoggingService.failedOrNotFinishedClusterJobs(step, clusterJobIdentifiers)
        Collection<ClusterJob> clusterJobsToCheck = clusterJobIdentifiersToCheck.collect {
            clusterJobService.getClusterJobByIdentifier(it, step)
        }

        int restartedStepCount = restartCount(step)
        Process firstWorkflow = firstWorkflowJobId(step.process)

        StringBuilder message = new StringBuilder('''OTP job failed\ndata:\n''')

        Map otpWorkflow = [
                otpWorkflowId         : step.process.id,
                otpWorkflowStarted    : dateString(step.process.started),
                otpWorkflowName       : step.jobExecutionPlan.name,
                otpLink               : processService.processUrl(step.process),
                restartedWorkflowJobId: CollectionUtils.atMostOneElement(Process.findAllByRestarted(step.process))?.id,
                originWorkflowJobId   : firstWorkflow.id,
                originWorkflowStart   : dateString(firstWorkflow.started),
                objectInformation     : object.toString().replaceAll(/ ?<br> ?/, ' ').replaceAll(/\n/, ' '),
                ilseNumbers           : ilseNumbers,
                openTickets           : openTicketsUrl.join(', '),
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
                    queue              : TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime)clusterJob.queued),
                    // time when job is ready for start (no hold anymore)
                    eligible           : TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime)clusterJob.eligible),
                    start              : TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime)clusterJob.started),
                    ended              : TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime)clusterJob.ended),
                    runningHours       : clusterJob.started && clusterJob.ended ? clusterJobDetailService.calculateElapsedWalltime(clusterJob).toHours() : 'na',
                    logFile            : clusterJob.jobLog,
                    exitStatus         : clusterJob.exitStatus,
                    exitCode           : clusterJob.exitCode,
                    node               : clusterJob.node,
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

        String subject = "${subjectPrefix}: ${otpWorkflow.otpWorkflowName} ${object.individual?.displayName} ${object.project?.name}"

        mailHelperService.sendEmailToTicketSystem(subject, message.toString())
    }

    private String dateString(Date date) {
        return date ? TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(date) : 'na'
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
        Process previous = CollectionUtils.atMostOneElement(Process.findAllByRestarted(process))
        if (previous) {
            return firstWorkflowJobId(previous)
        }
        return process
    }
}
