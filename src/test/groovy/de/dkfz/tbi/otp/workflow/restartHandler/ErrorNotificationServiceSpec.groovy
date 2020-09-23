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

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError

class ErrorNotificationServiceSpec extends Specification implements ServiceUnitTest<ErrorNotificationService>, DataTest, DomainFactoryProcessingPriority, WorkflowSystemDomainFactory {
    @Override
    Class[] getDomainClassesToMock() {
        [
                ProcessingOption,
                Workflow,
                WorkflowArtefact,
                WorkflowRun,
                WorkflowRunInputArtefact,
        ]
    }

    void "test send"() {
        given:
        ErrorNotificationService service = Spy(ErrorNotificationService)
        service.processingOptionService = new ProcessingOptionService()
        service.mailHelperService = Mock(MailHelperService)

        WorkflowStep step = createWorkflowStep()
        step.workflowRun.priority.errorMailPrefix = subjectPrefix
        DomainFactory.createProcessingOptionForErrorRecipient("error@example.com")

        when:
        service.send(step)

        then:
        1 * service.getInformation(step) >> { s -> "error-message" }
        1 * service.mailHelperService.sendEmail(_ as String, "error-message", ["error@example.com"]) >> { String subject, content, recipient ->
            assert subject.contains(step.workflowRun.priority.errorMailPrefix ?: "ERROR")
        }

        where:
        subjectPrefix << [null, "error-prefix"]
    }

    void "test getInformation"() {
        given:
        ErrorNotificationService service = Spy(ErrorNotificationService) {
            getSeqTracks(_) >> { s -> [] }
        }

        WorkflowStep step = createWorkflowStep(beanName: "step-bean-name", wesIdentifier: "wes-id")
        step.workflowError = new WorkflowError(stacktrace: "stack-trace")
        step.workflowRun.workflow.name = "workflow-name"
        step.workflowRun.workflow.beanName = "workflow-bean-name"
        WorkflowArtefact inArtefact = createWorkflowArtefact()
        createWorkflowRunInputArtefact(workflowRun: step.workflowRun, role: "input-role", workflowArtefact: inArtefact)
        createWorkflowArtefact(outputRole: "output-role", producedBy:  step.workflowRun)

        when:
        String content = service.getInformation(step)

        then:
        content.contains(step.beanName)
        content.contains(step.id as String)
        content.contains(step.workflowRun.workflow.name)
        content.contains(step.workflowRun.workflow.beanName)
        content.contains(step.workflowRun.id as String)
        content.contains(step.workflowRun.inputArtefacts.keySet().first())
        content.contains(step.workflowRun.inputArtefacts.values().first().toString())
        content.contains(step.workflowRun.outputArtefacts.keySet().first())
        content.contains(step.workflowRun.outputArtefacts.values().first().toString())
        content.contains(step.workflowError.stacktrace)
        content.contains(step.wesIdentifier)
    }
}
