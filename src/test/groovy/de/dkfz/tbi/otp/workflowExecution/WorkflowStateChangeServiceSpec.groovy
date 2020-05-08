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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

class WorkflowStateChangeServiceSpec extends Specification implements ServiceUnitTest<WorkflowStateChangeService>, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Workflow,
                WorkflowRun,
                WorkflowRunInputArtefact,
        ]
    }

    void "test changeStateToSkipped"() {
        given:
        WorkflowStep workflowStep = DomainFactory.createWorkflowStep()
        SkippedMessage skippedMessage = new SkippedMessage(message: "asdf", category: SkippedMessage.Category.WORKFLOW_COVERAGE_REJECTION)
        WorkflowArtefact wa1 = DomainFactory.createWorkflowArtefact()
        workflowStep.workflowRun.outputArtefacts = [asdf: wa1]
        workflowStep.workflowRun.save(flush: true)

        WorkflowArtefact wa2 = DomainFactory.createWorkflowArtefact(state: WorkflowArtefact.State.FAILED)
        WorkflowRun wr2 = DomainFactory.createWorkflowRun(state: WorkflowRun.State.FAILED, outputArtefacts: [asdf: wa2])
        DomainFactory.createWorkflowRunInputArtefact(workflowRun: wr2, workflowArtefact: wa1)

        WorkflowArtefact wa3 = DomainFactory.createWorkflowArtefact(state: WorkflowArtefact.State.PLANNED_OR_RUNNING)
        WorkflowRun wr3 = DomainFactory.createWorkflowRun(state: WorkflowRun.State.PENDING, outputArtefacts: [asdf: wa3])
        DomainFactory.createWorkflowRunInputArtefact(workflowRun: wr3, workflowArtefact: wa2)

        when:
        service.changeStateToSkipped(workflowStep, skippedMessage)

        then:
        workflowStep.state == WorkflowStep.State.SKIPPED
        workflowStep.workflowRun.state == WorkflowRun.State.SKIPPED
        workflowStep.workflowRun.skippedMessage == skippedMessage
        wa1.state == WorkflowArtefact.State.SKIPPED

        wa2.state == WorkflowArtefact.State.FAILED
        wr2.state == WorkflowRun.State.FAILED

        wa3.state == WorkflowArtefact.State.SKIPPED
        wr3.state == WorkflowRun.State.SKIPPED
        wr3.skippedMessage == skippedMessage
    }

    void "test changeStateToWaiting"() {
        given:
        WorkflowStep workflowStep = DomainFactory.createWorkflowStep()

        when:
        service.changeStateToWaiting(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.SUCCESS
        workflowStep.workflowRun.state == WorkflowRun.State.WAITING
    }

    void "test changeStateToFinalFailed"() {
        given:
        WorkflowStep workflowStep = DomainFactory.createWorkflowStep()
        WorkflowArtefact wa1 = DomainFactory.createWorkflowArtefact()
        workflowStep.workflowRun.outputArtefacts = [asdf: wa1]
        workflowStep.workflowRun.save(flush: true)

        WorkflowArtefact wa2 = DomainFactory.createWorkflowArtefact(state: WorkflowArtefact.State.FAILED)
        WorkflowRun wr2 = DomainFactory.createWorkflowRun(state: WorkflowRun.State.FAILED, outputArtefacts: [asdf: wa2])
        DomainFactory.createWorkflowRunInputArtefact(workflowRun: wr2, workflowArtefact: wa1)

        WorkflowArtefact wa3 = DomainFactory.createWorkflowArtefact(state: WorkflowArtefact.State.PLANNED_OR_RUNNING)
        WorkflowRun wr3 = DomainFactory.createWorkflowRun(state: WorkflowRun.State.PENDING, outputArtefacts: [asdf: wa3])
        DomainFactory.createWorkflowRunInputArtefact(workflowRun: wr3, workflowArtefact: wa2)

        when:
        service.changeStateToFinalFailed(workflowStep)

        then:
        workflowStep.workflowRun.state == WorkflowRun.State.FAILED_FINAL
        wa1.state == WorkflowArtefact.State.FAILED

        wa2.state == WorkflowArtefact.State.FAILED
        wr2.state == WorkflowRun.State.FAILED

        wa3.state == WorkflowArtefact.State.SKIPPED
        wr3.state == WorkflowRun.State.SKIPPED
        wr3.skippedMessage.category == SkippedMessage.Category.PREREQUISITE_WORKFLOW_RUN_NOT_SUCCESSFUL
        wr3.skippedMessage.message == "Previous run failed"
    }

    void "test changeStateToFailed"() {
        given:
        WorkflowStep workflowStep = DomainFactory.createWorkflowStep()

        when:
        service.changeStateToFailed(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.FAILED
        workflowStep.workflowRun.state == WorkflowRun.State.FAILED
    }

    void "test changeStateToSuccess, is not last step"() {
        given:
        WorkflowStep workflowStep = DomainFactory.createWorkflowStep(beanName: "1st job bean")
        WorkflowArtefact workflowArtefact = DomainFactory.createWorkflowArtefact()
        workflowStep.workflowRun.workflow.beanName = "workflow bean"
        workflowStep.workflowRun.outputArtefacts = [abc: workflowArtefact]
        workflowStep.workflowRun.workflow.save(flush: true)
        service.applicationContext = Mock(ApplicationContext) {
            getBean("workflow bean", OtpWorkflow) >> { [getJobBeanNames: { ["1st job bean", "2nd job bean"] }] as OtpWorkflow }
        }

        when:
        service.changeStateToSuccess(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.SUCCESS
        workflowStep.workflowRun.state != WorkflowRun.State.SUCCESS
        workflowStep.workflowRun.outputArtefacts.every { String s, WorkflowArtefact wa ->
            wa.state != WorkflowArtefact.State.SUCCESS
        }
    }

    void "test changeStateToSuccess, is last step"() {
        given:
        WorkflowStep workflowStep = DomainFactory.createWorkflowStep(beanName: "2nd job bean")
        WorkflowArtefact workflowArtefact = DomainFactory.createWorkflowArtefact()
        workflowStep.workflowRun.workflow.beanName = "workflow bean"
        workflowStep.workflowRun.outputArtefacts = [abc: workflowArtefact]
        workflowStep.workflowRun.workflow.save(flush: true)
        service.applicationContext = Mock(ApplicationContext) {
            getBean("workflow bean", OtpWorkflow) >> { [getJobBeanNames: { ["1st job bean", "2nd job bean"] }] as OtpWorkflow }
        }

        when:
        service.changeStateToSuccess(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.SUCCESS
        workflowStep.workflowRun.state == WorkflowRun.State.SUCCESS
        workflowStep.workflowRun.outputArtefacts.every { String s, WorkflowArtefact wa ->
            wa.state == WorkflowArtefact.State.SUCCESS
        }
    }

    void "test changeStateToRunning"() {
        given:
        WorkflowStep workflowStep = DomainFactory.createWorkflowStep()

        when:
        service.changeStateToRunning(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.RUNNING
        workflowStep.workflowRun.state == WorkflowRun.State.RUNNING
    }
}