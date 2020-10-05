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
import org.joda.time.DateTime

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.text.SimpleDateFormat
import java.time.LocalDateTime

@Transactional
class ErrorNotificationService {

    MailHelperService mailHelperService

    OtrsTicketService otrsTicketService

    ProcessingOptionService processingOptionService

    void sendMaintainer(WorkflowStep workflowStep, Throwable t) {
        String subject = [
                workflowStep.workflowRun.priority.errorMailPrefix,
                ": Exception in RestartHandler for: ",
                workflowStep.workflowRun.workflow,
                ' in ',
                workflowStep.beanName,
        ].join('')

        StringWriter sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        String body = [
                "Run id: ${workflowStep.workflowRun.id}",
                "Step id: ${workflowStep.id}",
                "Time: ${LocalDateTime.now()}",
                '\nStacktrace:',
                sw,
        ].join('\n')

        mailHelperService.sendEmail(subject, body, recipients)
    }

    void send(WorkflowStep workflowStep, WorkflowJobErrorDefinition.Action action, String checkText, List<JobErrorDefinitionWithLogWithIdentifier> matches) {
        assert workflowStep
        assert action
        assert checkText

        if (recipients) {
            String subject = createSubject(workflowStep, action)
            String body = [
                    getRestartInformation(action, checkText, matches),
                    createWorkflowInformation(workflowStep),
                    createArtefactsInformation(workflowStep),
                    createWorkflowStepInformation(workflowStep),
                    createLogInformation(workflowStep),
            ].join('\n')
            mailHelperService.sendEmail(subject, body, recipients)
        }
    }

    private List<String> getRecipients() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.EMAIL_RECIPIENT_ERRORS)
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

        return message.join("\n")
    }

    protected String createArtefactsInformation(WorkflowStep workflowStep) {
        assert workflowStep

        List<String> message = []

        message << header("Input artefacts")
        if (workflowStep.workflowRun.inputArtefacts) {
            workflowStep.workflowRun.inputArtefacts.sort { it.key }.each { k, v ->
                message << "${k}: ${v.displayName}"
            }
        } else {
            message << "None"
        }

        message << header("Output artefacts")
        if (workflowStep.workflowRun.outputArtefacts) {
            workflowStep.workflowRun.outputArtefacts.sort { it.key }.each { k, v ->
                message << "${k}: ${v.displayName}"
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

        message << header("Logs")
        //TODO: add link to logs

        message << header("Cluster jobs")
        if (workflowStep.clusterJobs) {
            workflowStep.clusterJobs.sort { it.id }.each { ClusterJob clusterJob ->
                message << "ID: ${clusterJob.clusterJobId}"
                message << "Name: ${clusterJob.clusterJobName}"
                message << "Queued time: ${dateString(clusterJob.queued)}"
                message << "Eligible time: ${dateString(clusterJob.eligible)}"
                message << "Start time: ${dateString(clusterJob.started)}"
                message << "End time: ${dateString(clusterJob.ended)}"
                message << "Running hours: ${clusterJob.started && clusterJob.ended ? clusterJob.elapsedWalltime.standardHours : 'na'}"
                message << "Log file: ${clusterJob.jobLog}"
                message << "Exit status: ${clusterJob.exitStatus}"
                message << "Exit code: ${clusterJob.exitCode}"
                message << "Node: ${clusterJob.node}"
                message << "Start count: ${clusterJob.startCount}"
                message << ""
            }
        } else {
            message << "None"
        }

        message << header("WES job")
        if (workflowStep.wesIdentifier) {
            message << "ID: ${workflowStep.wesIdentifier}"
        } else {
            message << "None"
        }

        return message.join("\n")
    }

    private String header(String s) {
        "\n${s}\n${"=" * s.length()}"
    }

    private String dateString(DateTime date) {
        return dateString(date?.toDate())
    }

    private String dateString(Date date) {
        return date ? new SimpleDateFormat('yyyy-MM-dd HH:mm', Locale.ENGLISH).format(date) : 'na'
    }

    String getSubmissionIds(Set<SeqTrack> seqTracks) {
        return seqTracks*.ilseSubmission*.ilseNumber.unique().sort().join(', ')
    }

    String getTicketUrls(Set<SeqTrack> seqTracks) {
        return (seqTracks ? otrsTicketService.findAllOtrsTickets(seqTracks).findAll { OtrsTicket otrsTicket ->
            !otrsTicket.finalNotificationSent
        }*.url : []).join(",")
    }

    @Deprecated
    Set<SeqTrack> getSeqTracks(WorkflowStep workflowStep) {
        assert workflowStep
        (workflowStep.workflowRun.inputArtefacts.values() + workflowStep.workflowRun.outputArtefacts.values())
                *.artefact.findAll { it.present }*.get()
                .collectMany { (it as ProcessParameterObject).containedSeqTracks } as Set
    }
}
