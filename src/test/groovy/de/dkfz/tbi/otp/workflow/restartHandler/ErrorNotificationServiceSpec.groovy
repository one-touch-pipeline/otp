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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.workflowExecution.*

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
        service.processingOptionService = new ProcessingOptionService()
        service.mailHelperService = Mock(MailHelperService)

        WorkflowStep step = createWorkflowStep()
        DomainFactory.createProcessingOptionForErrorRecipient("error@example.com")
        OtpRuntimeException e1 = new OtpRuntimeException('error job')
        OtpRuntimeException e2 = new OtpRuntimeException('error restart handler')

        when:
        service.sendMaintainer(step, e1, e2)

        then:
        1 * service.mailHelperService.sendEmail(_ as String, _ as String, ["error@example.com"]) >> { String subject, String content, recipient ->
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
        service.processingOptionService = new ProcessingOptionService()
        service.mailHelperService = Mock(MailHelperService)

        WorkflowStep step = createWorkflowStep()
        DomainFactory.createProcessingOptionForErrorRecipient("error@example.com")

        when:
        service.send(step, WorkflowJobErrorDefinition.Action.STOP, "text", [])

        then:
        1 * service.createSubject(step, WorkflowJobErrorDefinition.Action.STOP) >> "header"
        1 * service.createWorkflowInformation(step) >> "workflow full"
        1 * service.createArtefactsInformation(step) >> "artefact"
        1 * service.createWorkflowStepInformation(step) >> "workflow step"
        1 * service.createLogInformation(step) >> "log"
        1 * service.mailHelperService.sendEmail(_ as String, _ as String, ["error@example.com"]) >> { String subject, String content, recipient ->
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
                "${workflowRunInputArtefact.role}: ${workflowRunInputArtefact.workflowArtefact.displayName}",
                "",
                "Output artefacts",
                "=+",
                "${outputArtefact.outputRole}: ${outputArtefact.displayName}",
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

        WorkflowStep step = createWorkflowStep()
        step.workflowError = createWorkflowError()

        String expectedExpression = [
                "^(?:.*\\n)*OTP message",
                "=+",
                step.workflowError.message,
                "",
                "Logs",
                "=+",
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

        WorkflowStep step = createWorkflowStep()
        step.workflowError = createWorkflowError()

        List<ClusterJob> clusterJobs = (1..3).collect {
            createClusterJob([
                    workflowStep: step,
                    exitCode    : nextId,
            ])
        }

        String expectedExpression = [
                "^(?:.*\\n)*OTP message",
                "=+",
                step.workflowError.message,
                "",
                "Logs",
                "=+",
                "",
                "Cluster jobs",
                "=+",
                clusterJobs.collect { ClusterJob clusterJob ->
                    [
                            "ID: ${clusterJob.clusterJobId}",
                            "Name: ${clusterJob.clusterJobName}",
                            "Running hours: ",
                            "Log file: ",
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
}
