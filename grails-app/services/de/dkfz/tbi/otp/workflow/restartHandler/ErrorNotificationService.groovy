/*
 * Copyright 2011-2020 The OTP authors
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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobDetailService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.StackTraceUtils
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.wes.WesLog
import de.dkfz.tbi.otp.workflowExecution.wes.WesRun
import de.dkfz.tbi.otp.workflowExecution.wes.WesRunLog
import de.dkfz.tbi.util.TimeFormats

import java.time.LocalDateTime
import java.time.ZonedDateTime

@CompileDynamic
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

        mailHelperService.sendEmailToTicketSystem(subject, body)
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
        mailHelperService.sendEmailToTicketSystem(subject, body)
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
        message << "final action: ${action}"
        message << "info to action: ${checkText}"

        message << header("Error definitions")
        message << "matching expression count: ${matches.size()}"
        matches.each { JobErrorDefinitionWithLogWithIdentifier definition ->
            message << "log identifier: ${definition.logWithIdentifier.identifier}"
            message << "definition name: ${definition.errorDefinition.name}"
            message << "type: ${definition.errorDefinition.sourceType}"
            message << "action: ${definition.errorDefinition.action}"
            if (definition.errorDefinition.beanToRestart) {
                message << "bean to restart: ${definition.errorDefinition.beanToRestart}"
            }
            message << "expression: ${definition.errorDefinition.errorExpression}"
            if (definition.errorDefinition.mailText) {
                message << "mail info: ${definition.errorDefinition.mailText}"
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
        message << "Name: ${workflowStep.workflowRun.workflow.name}"
        message << "Bean name: ${workflowStep.workflowRun.workflow.beanName}"

        message << header("Workflow run")
        message << "ID: ${workflowStep.workflowRun.id}, started at: ${dateString(workflowStep.workflowRun.dateCreated)}"
        message << "DisplayName: ${workflowStep.workflowRun.displayName}"
        message << "Restart count: ${workflowStep.workflowRun.restartCount}"
        if (workflowStep.workflowRun.restartCount > 0) {
            message << "Restarted from ID: ${workflowStep.workflowRun.restartedFrom?.id}, " +
                    "started at: ${dateString(workflowStep.workflowRun.restartedFrom?.dateCreated)}"
        }
        if (workflowStep.workflowRun.restartCount > 1) {
            WorkflowRun originalRun = workflowStep.workflowRun.originalRestartedFrom
            message << "Original ID: ${originalRun.id}, started at: ${dateString(originalRun.dateCreated)}"
        }
        message << "Submission IDs (Ilse): ${getSubmissionIds(seqTracks) ?: "None"}"
        message << "Tickets: ${getTicketUrls(seqTracks).join(', ') ?: "None"}"
        message << "Link: ${grailsLinkGenerator.link(controller: 'workflowRunDetails', action: 'index', id: workflowStep.workflowRun.id, absolute: 'true')}"

        return message.join("\n")
    }

    protected String createArtefactsInformation(WorkflowStep workflowStep) {
        assert workflowStep

        List<String> message = []

        message << header("Input artefacts")
        if (workflowStep.workflowRun.inputArtefacts) {
            workflowStep.workflowRun.inputArtefacts.sort { it.key }.each { k, v ->
                message << "- ${k}: ${v.displayName.replace('\n', '\n    ')}"
            }
        } else {
            message << "None"
        }

        message << header("Output artefacts")
        if (workflowStep.workflowRun.outputArtefacts) {
            workflowStep.workflowRun.outputArtefacts.sort { it.key }.each { k, v ->
                message << "- ${k}: ${v.displayName.replace('\n', '\n    ')}"
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
        message << "Bean name: ${workflowStep.beanName}"
        message << "ID: ${workflowStep.id}, started at: ${dateString(workflowStep.dateCreated)}, failed at: ${dateString(new Date())}"
        message << "Restart count: ${workflowStep.restartCount}"
        if (workflowStep.restartCount > 0) {
            message << "Restarted from ID: ${workflowStep.restartedFrom?.id}, started at: ${dateString(workflowStep.restartedFrom?.dateCreated)}"
        }
        if (workflowStep.restartCount > 1) {
            WorkflowStep originalStep = workflowStep.originalRestartedFrom
            message << "Original ID: ${originalStep.id}, started at: ${dateString(originalStep.dateCreated)}"
        }

        return message.join("\n")
    }

    protected String createLogInformation(WorkflowStep workflowStep) {
        assert workflowStep

        List<String> message = []

        message << header("OTP message")
        message << workflowStep.workflowError.message ?: "None"
        message << "Error page: ${grailsLinkGenerator.link(controller: 'workflowRunDetails', action: 'showError', id: workflowStep.id, absolute: 'true')}"

        message << header("Logs")
        message << "Log page: ${grailsLinkGenerator.link(controller: 'workflowRunDetails', action: 'showLogs', id: workflowStep.id, absolute: 'true')}"

        message << header("Cluster jobs")

        WorkflowStep prevRunningWorkflowStep = workflowStepService.getPreviousRunningWorkflowStep(workflowStep)

        if (prevRunningWorkflowStep?.clusterJobs) {
            prevRunningWorkflowStep.clusterJobs.sort { it.id }.each { ClusterJob clusterJob ->
                message << "ID: ${clusterJob.clusterJobId}"
                message << "Name: ${clusterJob.clusterJobName}"
                message << "Queued time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) clusterJob.queued)}"
                message << "Eligible time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) clusterJob.eligible)}"
                message << "Start time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) clusterJob.started)}"
                message << "End time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) clusterJob.ended)}"
                message << "Running hours: ${clusterJob.started && clusterJob.ended ? clusterJobDetailService.calculateElapsedWalltime(job).toHours() : 'na'}"
                message << "Requested walltime: ${clusterJob.requestedWalltime}"
                message << "Log file: ${clusterJob.jobLog}"
                message << "Log page: ${grailsLinkGenerator.link(controller: 'clusterJobDetail', action: 'show', id: clusterJob.id, absolute: 'true')}"
                message << "Exit status: ${clusterJob.exitStatus}"
                message << "Exit code: ${clusterJob.exitCode}"
                message << "Node: ${clusterJob.node}"
                message << "Used memory: ${clusterJob.usedMemory}"
                message << "Requested memory: ${clusterJob.requestedMemory}"
                message << ""
            }
        } else {
            message << "None"
        }

        message << header("WES jobs")
        if (prevRunningWorkflowStep?.wesRuns) {
            prevRunningWorkflowStep.wesRuns.sort { it.id }.each { WesRun wesRun ->
                message << "WES run ID: ${wesRun.id}"
                message << "WES identifier: ${wesRun.wesIdentifier}"
                message << "Work directory: ${wesRun.workflowStep.workflowRun.workDirectory}/${wesRun.subPath}"

                WesRunLog wesRunLog = wesRun.wesRunLog
                message << "State: ${wesRunLog.state}"
                message << "Request: ${wesRunLog?.runRequest}"

                WesLog runLog = wesRunLog?.runLog
                message << "Name: ${runLog?.name}"
                message << "Start time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) runLog?.startTime)}"
                message << "End time: ${TimeFormats.DATE_TIME.getFormattedZonedDateTime((ZonedDateTime) runLog?.endTime)}"
                message << "Log file command: ${runLog?.cmd?.replaceAll('\n', '\n    ')}"
                message << "Stdout: ${runLog?.stdout}"
                message << "Stderr: ${runLog?.stderr}"
                message << "ExitCode: ${runLog?.exitCode}"
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
        } : []
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
