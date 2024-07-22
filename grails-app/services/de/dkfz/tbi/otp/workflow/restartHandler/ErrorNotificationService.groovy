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
package de.dkfz.tbi.otp.workflow.restartHandler

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobDetailService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.StackTraceUtils
import de.dkfz.tbi.otp.utils.TimeFormats
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.time.LocalDateTime
import java.time.ZonedDateTime

@Transactional
class ErrorNotificationService {

    MailHelperService mailHelperService

    TicketService ticketService

    LinkGenerator grailsLinkGenerator

    WorkflowStepService workflowStepService

    ClusterJobDetailService clusterJobDetailService

    void sendMaintainer(WorkflowStep workflowStep, Throwable exceptionInJob, Throwable exceptionInExceptionHandling) {
        String subject = [
                workflowStep.workflowRun.priority.errorMailPrefix,
                ": Exception in RestartHandler for: ",
                workflowStep.workflowRun.workflow.displayName,
                ' in ',
                workflowStep.beanName,
        ].join('')

        String body = [
                "Workflow: ${workflowStep.workflowRun.workflow}",
                "Run: ${workflowStep.workflowRun}",
                "Step: ${workflowStep}",
                "Time: ${LocalDateTime.now()}",
                '\nStacktrace in job:',
                StackTraceUtils.getStackTrace(exceptionInJob),
                '\nStacktrace in exception handling:',
                StackTraceUtils.getStackTrace(exceptionInExceptionHandling),
        ].join('\n')

        mailHelperService.saveErrorMailInNewTransaction(subject, body)
    }

    void send(WorkflowStep workflowStep, WorkflowJobErrorDefinition.Action action, String checkText, List<JobErrorDefinitionWithLogWithIdentifier> matches) {
        assert workflowStep
        assert action
        assert checkText

        String subject = createSubject(workflowStep, action)
        String body = [
                getRestartInformation(action, checkText, matches),
                createWorkflowInformation(workflowStep),
                createArtefactsInformation(workflowStep),
                createWorkflowStepInformation(workflowStep),
                createLogInformation(workflowStep),
        ].join('\n')
        mailHelperService.saveErrorMailInNewTransaction(subject, body)
    }

    protected String createSubject(WorkflowStep workflowStep, WorkflowJobErrorDefinition.Action action) {
        String prefix = "${workflowStep.workflowRun.priority.errorMailPrefix}: ${action}:"
        Collection<SeqTrack> seqTracks = getSeqTracks(workflowStep)

        return [
                prefix,
                workflowStep.workflowRun.workflow.name,
                workflowStep.beanName,
                seqTracks*.individual*.displayName.unique().join(","),
                workflowStep.workflowRun.project.name,
        ].join(' ')
    }

    protected String getRestartInformation(WorkflowJobErrorDefinition.Action action, String checkText, List<JobErrorDefinitionWithLogWithIdentifier> matches) {
        List<String> message = []
        message << header("Action")
        message << ("final action: ${action}" as String)
        message << ("info to action: ${checkText}" as String)

        message << header("Error definitions")
        message << ("matching expression count: ${matches.size()}" as String)
        matches.each { JobErrorDefinitionWithLogWithIdentifier definition ->
            message << ("log identifier: ${definition.logWithIdentifier.identifier}" as String)
            message << ("definition name: ${definition.errorDefinition.name}" as String)
            message << ("type: ${definition.errorDefinition.sourceType}" as String)
            message << ("action: ${definition.errorDefinition.action}" as String)
            if (definition.errorDefinition.beanToRestart) {
                message << ("bean to restart: ${definition.errorDefinition.beanToRestart}" as String)
            }
            message << ("expression: ${definition.errorDefinition.errorExpression}" as String)
            if (definition.errorDefinition.mailText) {
                message << ("mail info: ${definition.errorDefinition.mailText}" as String)
            }
            message << ''
        }
        return message.join("\n")
    }

    protected String createWorkflowInformation(WorkflowStep workflowStep) {
        assert workflowStep

        Collection<SeqTrack> seqTracks = getSeqTracks(workflowStep)

        List<String> message = []

        message << header("Workflow")
        message << ("Name: ${workflowStep.workflowRun.workflow.name}" as String)
        message << ("Bean name: ${workflowStep.workflowRun.workflow.beanName}" as String)

        message << header("Workflow run")
        message << ("ID: ${workflowStep.workflowRun.id}, started at: ${dateString(workflowStep.workflowRun.dateCreated)}" as String)
        message << ("DisplayName: ${workflowStep.workflowRun.displayName}" as String)
        message << ("Restart count: ${workflowStep.workflowRun.restartCount}" as String)
        if (workflowStep.workflowRun.restartCount > 0) {
            message << ("Restarted from ID: ${workflowStep.workflowRun.restartedFrom?.id}, " +
                    "started at: ${dateString(workflowStep.workflowRun.restartedFrom?.dateCreated)}" as String)
        }
        if (workflowStep.workflowRun.restartCount > 1) {
            WorkflowRun originalRun = workflowStep.workflowRun.originalRestartedFrom
            message << ("Original ID: ${originalRun.id}, started at: ${dateString(originalRun.dateCreated)}" as String)
        }
        message << ("Submission IDs (Ilse): ${getSubmissionIds(seqTracks) ?: "None"}" as String)
        message << ("Tickets: ${getTicketUrls(seqTracks).join(', ') ?: "None"}" as String)
        message << ("Link: " +
                "${grailsLinkGenerator.link(controller: 'workflowRunDetails', action: 'index', id: workflowStep.workflowRun.id, absolute: 'true')}") as String

        return message.join("\n")
    }

    protected String createArtefactsInformation(WorkflowStep workflowStep) {
        assert workflowStep

        List<String> message = []

        message << header("Input artefacts")
        if (workflowStep.workflowRun.inputArtefacts) {
            workflowStep.workflowRun.inputArtefacts.sort { it.key }.each { k, v ->
                message << ("- ${k}: ${v.displayName.replace('\n', '\n    ')}" as String)
            }
        } else {
            message << "None"
        }

        message << header("Output artefacts")
        if (workflowStep.workflowRun.outputArtefacts) {
            workflowStep.workflowRun.outputArtefacts.sort { it.key }.each { k, v ->
                message << ("- ${k}: ${v.displayName.replace('\n', '\n    ')}" as String)
            }
        } else {
            message << "None"
        }

        return message.join("\n")
    }

    protected String createWorkflowStepInformation(WorkflowStep workflowStep) {
        assert workflowStep

        List<String> message = []

        message << header("Workflow step")
        message << ("Bean name: ${workflowStep.beanName}" as String)
        message << ("ID: ${workflowStep.id}, started at: ${dateString(workflowStep.dateCreated)}, failed at: ${dateString(new Date())}" as String)
        message << ("Restart count: ${workflowStep.restartCount}" as String)
        if (workflowStep.restartCount > 0) {
            message << ("Restarted from ID: ${workflowStep.restartedFrom?.id}, started at: ${dateString(workflowStep.restartedFrom?.dateCreated)}" as String)
        }
        if (workflowStep.restartCount > 1) {
            WorkflowStep originalStep = workflowStep.originalRestartedFrom
            message << ("Original ID: ${originalStep.id}, started at: ${dateString(originalStep.dateCreated)}" as String)
        }

        return message.join("\n")
    }

    protected String createLogInformation(WorkflowStep workflowStep) {
        assert workflowStep

        List<String> message = []

        message << header("OTP message")
        message << workflowStep.workflowError.message ?: "None"
        message << ("Error page: " +
                "${grailsLinkGenerator.link(controller: 'workflowRunDetails', action: 'showError', id: workflowStep.id, absolute: 'true')}") as String

        message << header("Logs")
        message << ("Log page: " +
                "${grailsLinkGenerator.link(controller: 'workflowRunDetails', action: 'showLogs', id: workflowStep.id, absolute: 'true')}") as String

        message << header("Cluster jobs")

        WorkflowStep prevRunningWorkflowStep = workflowStepService.getPreviousRunningWorkflowStep(workflowStep)

        if (prevRunningWorkflowStep?.clusterJobs) {
            prevRunningWorkflowStep.clusterJobs.sort { it.id }.each { ClusterJob clusterJob ->
                message << ("ID: ${clusterJob.clusterJobId}" as String)
                message << ("Name: ${clusterJob.clusterJobName}" as String)
                message << ("Queued time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) clusterJob.queued)}" as String)
                message << ("Eligible time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) clusterJob.eligible)}" as String)
                message << ("Start time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) clusterJob.started)}" as String)
                message << ("End time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) clusterJob.ended)}" as String)
                String runningHour = clusterJob.started && clusterJob.ended ? clusterJobDetailService.calculateElapsedWalltime(clusterJob).toHours() : 'na'
                message << ("Running hours: ${runningHour}" as String)
                message << ("Requested walltime: ${clusterJob.requestedWalltime}" as String)
                message << ("Log file: ${clusterJob.jobLog}" as String)
                message << ("Log page: " +
                        "${grailsLinkGenerator.link(controller: 'clusterJobDetail', action: 'show', id: clusterJob.id, absolute: 'true')}") as String
                message << ("Exit status: ${clusterJob.exitStatus}" as String)
                message << ("Exit code: ${clusterJob.exitCode}" as String)
                message << ("Node: ${clusterJob.node}" as String)
                message << ("Used memory: ${clusterJob.usedMemory}" as String)
                message << ("Requested memory: ${clusterJob.requestedMemory}" as String)
                message << ""
            }
        } else {
            message << "None"
        }

        message << header("WES jobs")
        if (prevRunningWorkflowStep?.wesRuns) {
            prevRunningWorkflowStep.wesRuns.sort { it.id }.each { WesRun wesRun ->
                message << ("WES run ID: ${wesRun.id}" as String)
                message << ("WES identifier: ${wesRun.wesIdentifier}" as String)
                message << ("Work directory: ${wesRun.workflowStep.workflowRun.workDirectory}/${wesRun.subPath}" as String)

                WesRunLog wesRunLog = wesRun.wesRunLog
                message << ("State: ${wesRunLog.state}" as String)
                message << ("Request: ${wesRunLog?.runRequest}" as String)

                WesLog runLog = wesRunLog?.runLog
                message << ("Name: ${runLog?.name}" as String)
                message << ("Start time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) runLog?.startTime)}" as String)
                message << ("End time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) runLog?.endTime)}" as String)
                message << ("Log file command: ${runLog?.cmd?.replaceAll('\n', '\n    ')}" as String)
                message << ("Stdout: ${runLog?.stdout}" as String)
                message << ("Stderr: ${runLog?.stderr}" as String)
                message << ("ExitCode: ${runLog?.exitCode}" as String)
                message << ""
            }
        } else {
            message << "None"
        }

        return message.join("\n")
    }

    private String header(String s) {
        return "\n${s}\n${"=" * s.length()}"
    }

    private String dateString(Date date) {
        return TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(date)
    }

    String getSubmissionIds(Set<SeqTrack> seqTracks) {
        return seqTracks*.ilseSubmission*.ilseNumber.unique().sort().join(', ')
    }

    String getTicketUrls(Set<SeqTrack> seqTracks) {
        Set<Ticket> tickets = seqTracks ? ticketService.findAllTickets(seqTracks).findAll { Ticket ticket ->
            !ticket.finalNotificationSent
        } : [] as Set
        return tickets.collect {
            ticketService.buildTicketDirectLink(it)
        }.join(',')
    }

    Set<SeqTrack> getSeqTracks(WorkflowStep workflowStep) {
        assert workflowStep
        return (workflowStep.workflowRun.inputArtefacts.values() + workflowStep.workflowRun.outputArtefacts.values())
                *.artefact.findAll { it.present }*.get()
                .collectMany { it.containedSeqTracks } as Set
    }
}
