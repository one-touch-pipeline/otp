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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.web.mapping.LinkGenerator
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobDetailService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.time.Duration
import java.time.ZonedDateTime

class ErrorNotificationServiceSpec extends Specification
        implements ServiceUnitTest<ErrorNotificationService>, DataTest, DomainFactoryProcessingPriority, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
                SeqTrack,
                Workflow,
                WorkflowArtefact,
                WorkflowRun,
                WorkflowRunInputArtefact,
        ]
    }

    void "test sendMaintainer"() {
        given:
        ErrorNotificationService service = Spy(ErrorNotificationService)
        service.mailHelperService = Mock(MailHelperService)

        WorkflowStep step = createWorkflowStep()
        OtpRuntimeException e1 = new OtpRuntimeException('error job')
        OtpRuntimeException e2 = new OtpRuntimeException('error restart handler')

        when:
        service.sendMaintainer(step, e1, e2)

        then:
        1 * service.mailHelperService.sendEmailToTicketSystem(_ as String, _ as String) >> { String subject, String content ->
            assert subject.contains(step.workflowRun.priority.errorMailPrefix)
            assert subject.contains('RestartHandler')
            assert subject.contains(step.workflowRun.workflow.name)
            assert subject.contains(step.beanName)

            assert content.contains(step.workflowRun.workflow.name)
            assert content.contains(step.workflowRun.toString())
            assert content.contains(step.toString())
            assert content.contains(e1.message)
            assert content.contains(e2.message)
        }
    }

    void "test send"() {
        given:
        ErrorNotificationService service = Spy(ErrorNotificationService)
        service.mailHelperService = Mock(MailHelperService)

        WorkflowStep step = createWorkflowStep()

        when:
        service.send(step, WorkflowJobErrorDefinition.Action.STOP, "text", [])

        then:
        1 * service.createSubject(step, WorkflowJobErrorDefinition.Action.STOP) >> "header"
        1 * service.createWorkflowInformation(step) >> "workflow full"
        1 * service.createArtefactsInformation(step) >> "artefact"
        1 * service.createWorkflowStepInformation(step) >> "workflow step"
        1 * service.createLogInformation(step) >> "log"
        1 * service.mailHelperService.sendEmailToTicketSystem(_ as String, _ as String) >> { String subject, String content ->
            assert subject.contains("header")
            assert content.contains('workflow full')
            assert content.contains('artefact')
            assert content.contains('workflow step')
            assert content.contains('log')
        }
    }

    void "test createSubject"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        ErrorNotificationService service = Spy(ErrorNotificationService) {
            getSeqTracks(_) >> [seqTrack]
        }
        WorkflowStep step = createWorkflowStep()
        WorkflowJobErrorDefinition.Action action = WorkflowJobErrorDefinition.Action.STOP

        when:
        String header = service.createSubject(step, action)

        then:
        header.startsWith("${step.workflowRun.priority.errorMailPrefix}: ${action}: ")
        header.contains(seqTrack.individual.displayName)
        header.contains(step.workflowRun.project.name)
    }

    void "test createWorkflowInformation"() {
        given:
        ErrorNotificationService service = Spy(ErrorNotificationService) {
            getSeqTracks(_) >> { s -> [] }
        }

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            1 * link(_) >> { return "link" }
        }

        WorkflowStep step = createWorkflowStep()
        step.workflowError = createWorkflowError()

        String expectedExpression = [
                "^(?:.*\\n)*Workflow",
                "=+",
                "Name: ${step.workflowRun.workflow.name}",
                "Bean name: ${step.workflowRun.workflow.beanName}",
                "",
                "Workflow run",
                "=+",
                "ID: ${step.workflowRun.id}",
                "Restart count: ${step.workflowRun.restartCount}",
                "Submission IDs \\(Ilse\\): ",
                "Tickets: None",
                "Link: link",
        ].join('(?:.*\\n)+') + "[.\\n]*\$"

        when:
        String content = service.createWorkflowInformation(step)

        then:
        content ==~ expectedExpression
    }

    void "test createArtefactsInformation"() {
        given:
        ErrorNotificationService service = new ErrorNotificationService()

        WorkflowStep step = createWorkflowStep()
        step.workflowError = createWorkflowError()

        WorkflowArtefact inArtefact = createWorkflowArtefact()
        WorkflowRunInputArtefact workflowRunInputArtefact = createWorkflowRunInputArtefact(workflowRun: step.workflowRun, role: "input-role", workflowArtefact: inArtefact)
        WorkflowArtefact outputArtefact = createWorkflowArtefact(outputRole: "output-role", producedBy: step.workflowRun)

        String expectedExpression = [
                "^(?:.*\\n)*Input artefacts",
                "=+",
                "- ${workflowRunInputArtefact.role}: ${workflowRunInputArtefact.workflowArtefact.displayName}",
                "",
                "Output artefacts",
                "=+",
                "- ${outputArtefact.outputRole}: ${outputArtefact.displayName}",
        ].join('(?:.*\\n)+') + "[.\\n]*\$"

        when:
        String content = service.createArtefactsInformation(step)

        then:
        content ==~ expectedExpression
    }

    void "test createWorkflowStepInformation"() {
        given:
        ErrorNotificationService service = new ErrorNotificationService()

        WorkflowStep step = createWorkflowStep()
        step.workflowError = createWorkflowError()

        String expectedExpression = [
                "^(?:.*\\n)*Workflow step",
                "=+",
                "Bean name: ${step.beanName}",
                "ID: ${step.id}",
                "Restart count: ${step.restartCount}",
        ].join('(?:.*\\n)+') + "[.\\n]*\$"

        when:
        String content = service.createWorkflowStepInformation(step)

        then:
        content ==~ expectedExpression
    }

    void "test createLogInformation without ClusterJob nor WES pipeline"() {
        given:
        ErrorNotificationService service = new ErrorNotificationService()

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            2 * link(_) >> { return "link" }
        }

        service.workflowStepService = Mock(WorkflowStepService)

        WorkflowStep step = createWorkflowStep()
        step.workflowError = createWorkflowError()

        String expectedExpression = [
                "^(?:.*\\n)*OTP message",
                "=+",
                step.workflowError.message,
                "Error page: link",
                "",
                "Logs",
                "=+",
                "Log page: link",
                "",
                "Cluster jobs",
                "=+",
                "None",
                "",
                "WES job",
                "=+",
                "None",
        ].join('(?:.*\\n)+') + "[.\\n]*\$"

        when:
        String content = service.createLogInformation(step)

        then:
        content =~ expectedExpression
    }

    void "test createLogInformation with Cluster jobs"() {
        given:
        ErrorNotificationService service = new ErrorNotificationService()

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(_) >> { return "link" }
        }

        WorkflowStep prevStep = createWorkflowStep()

        List<ClusterJob> clusterJobs = (1..3).collect {
            createClusterJob([
                    workflowStep     : prevStep,
                    exitCode         : nextId,
                    queued           : ZonedDateTime.now().minusHours(4),
                    eligible         : ZonedDateTime.now().minusHours(3),
                    started          : ZonedDateTime.now().minusHours(2),
                    ended            : ZonedDateTime.now().minusHours(1),
                    requestedWalltime: Duration.ofHours(10),
                    jobLog           : "/tmp/log${it}",
                    checkStatus      : ClusterJob.CheckStatus.FINISHED,
                    exitStatus       : ClusterJob.Status.FAILED,
                    node             : "node_1",
            ])
        }

        WorkflowStep step = createWorkflowStep(previous: prevStep)
        step.workflowError = createWorkflowError()

        service.workflowStepService = Mock(WorkflowStepService) {
            1 * getPreviousRunningWorkflowStep(step) >> prevStep
        }
        service.clusterJobDetailService = Mock(ClusterJobDetailService) {
            0 * _
        }
        clusterJobs.each { clusterJob ->
            1 * service.clusterJobDetailService.calculateElapsedWalltime(clusterJob) >> Duration.ofHours(1)
        }

        String expectedExpression = [
                "^(?:.*\\n)*OTP message",
                "=+",
                step.workflowError.message,
                "",
                "Cluster jobs",
                "=+",
                clusterJobs.collect { ClusterJob clusterJob ->
                    [
                            "ID: ${clusterJob.clusterJobId}",
                            "Name: ${clusterJob.clusterJobName}",
                            "Running hours: ",
                            "Log file: ",
                            "Log page: link",
                            "Exit status: ",
                            "Exit code: ",
                            "Node: ",
                            "",
                    ]
                },
                "WES job",
                "=+",
                "None",
        ].flatten().join('(?:.*\\n)+') + "[.\\n]*\$"

        when:
        String content = service.createLogInformation(step)

        then:
        content ==~ expectedExpression
    }

    void "test createLogInformation with Wes jobs"() {
        given:
        ErrorNotificationService service = new ErrorNotificationService()

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(_) >> { return "link" }
        }

        WorkflowStep prevStep = createWorkflowStep()

        List<WesRun> wesRuns = (1..3).collect {
            createWesRun([
                    workflowStep: prevStep,
                    wesRunLog   : createWesRunLog([
                            runLog: createWesLog([
                                    exitCode: nextId,
                            ]),
                    ]),
            ])
        }

        WorkflowStep step = createWorkflowStep(previous: prevStep)
        step.workflowError = createWorkflowError()

        service.workflowStepService = Mock(WorkflowStepService) {
            1 * getPreviousRunningWorkflowStep(step) >> prevStep
        }

        String expectedExpression = [
                "^(?:.*\\n)*OTP message",
                "=+",
                step.workflowError.message,
                "",
                "Cluster jobs",
                "=+",
                "None",
                "",
                "WES job",
                "=+",
                wesRuns.collect { WesRun wesRun ->
                    WesRunLog wesRunLog = wesRun.wesRunLog
                    WesLog runLog = wesRunLog?.runLog
                    [
                            "WES run ID: ${wesRun.id}",
                            "WES identifier: ${wesRun.wesIdentifier}",
                            "Work directory: ${wesRun.workflowStep.workflowRun.workDirectory}/${wesRun.subPath}",

                            "State: ${wesRunLog.state}",
                            "Request: ${wesRunLog?.runRequest}",

                            "Name: ${runLog?.name}",
                            "Start time:",
                            "End time:",
                            "Log file command:",
                            "Stdout:",
                            "Stderr:",
                            "ExitCode:",
                            "",
                    ]
                },
        ].flatten().join('(?:.*\\n)+') + "[.\\n]*\$"

        when:
        String content = service.createLogInformation(step)

        then:
        content ==~ expectedExpression
    }
}
