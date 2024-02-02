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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

class WorkflowStateChangeServiceSpec extends Specification implements ServiceUnitTest<WorkflowStateChangeService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
                WorkflowRun,
                WorkflowRunInputArtefact,
        ]
    }

    void "test changeStateToSkipped"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        OmittedMessage skippedMessage = new OmittedMessage(message: "asdf", category: OmittedMessage.Category.WORKFLOW_COVERAGE_REJECTION)
        WorkflowArtefact wa1 = createWorkflowArtefact(producedBy: workflowStep.workflowRun, outputRole: "asdf")

        WorkflowRun wr2 = createWorkflowRun(state: WorkflowRun.State.FAILED)
        WorkflowArtefact wa2 = createWorkflowArtefact(state: WorkflowArtefact.State.FAILED, producedBy: wr2, outputRole: "asdf")
        createWorkflowRunInputArtefact(workflowRun: wr2, workflowArtefact: wa1)

        WorkflowRun wr3 = createWorkflowRun(state: WorkflowRun.State.PENDING)
        WorkflowArtefact wa3 = createWorkflowArtefact(state: WorkflowArtefact.State.PLANNED_OR_RUNNING, producedBy: wr3, outputRole: "asdf")
        createWorkflowRunInputArtefact(workflowRun: wr3, workflowArtefact: wa2)

        when:
        service.changeStateToOmitted(workflowStep, skippedMessage)

        then:
        workflowStep.state == WorkflowStep.State.OMITTED
        workflowStep.workflowRun.state == WorkflowRun.State.OMITTED_MISSING_PRECONDITION
        workflowStep.workflowRun.omittedMessage == skippedMessage
        wa1.state == WorkflowArtefact.State.OMITTED

        wa2.state == WorkflowArtefact.State.FAILED
        wr2.state == WorkflowRun.State.FAILED

        wa3.state == WorkflowArtefact.State.OMITTED
        wr3.state == WorkflowRun.State.OMITTED_MISSING_PRECONDITION
        wr3.omittedMessage == skippedMessage
    }

    void "test changeStateToWaitingOnUser"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.changeStateToWaitingOnUser(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.SUCCESS
        workflowStep.workflowRun.state == WorkflowRun.State.WAITING_FOR_USER
    }

    void "test changeStateToWaitingOnSystem"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.changeStateToWaitingOnSystem(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.SUCCESS
        workflowStep.workflowRun.state == WorkflowRun.State.RUNNING_WES
    }

    void "test changeStateToFinalFailed"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowArtefact wa1 = createWorkflowArtefact(producedBy: workflowStep.workflowRun, outputRole: "asdf")
        workflowStep.workflowRun.save(flush: true)

        WorkflowRun wr2 = createWorkflowRun(state: WorkflowRun.State.FAILED)
        WorkflowArtefact wa2 = createWorkflowArtefact(state: WorkflowArtefact.State.FAILED, producedBy: wr2, outputRole: "asdf")
        createWorkflowRunInputArtefact(workflowRun: wr2, workflowArtefact: wa1)

        WorkflowRun wr3 = createWorkflowRun(state: WorkflowRun.State.PENDING)
        WorkflowArtefact wa3 = createWorkflowArtefact(state: WorkflowArtefact.State.PLANNED_OR_RUNNING, producedBy: wr3, outputRole: "asdf")
        createWorkflowRunInputArtefact(workflowRun: wr3, workflowArtefact: wa2)

        when:
        service.changeStateToFinalFailed(workflowStep)

        then:
        workflowStep.workflowRun.state == WorkflowRun.State.FAILED_FINAL
        wa1.state == WorkflowArtefact.State.FAILED

        wa2.state == WorkflowArtefact.State.FAILED
        wr2.state == WorkflowRun.State.FAILED

        wa3.state == WorkflowArtefact.State.OMITTED
        wr3.state == WorkflowRun.State.OMITTED_MISSING_PRECONDITION
        wr3.omittedMessage.category == OmittedMessage.Category.PREREQUISITE_WORKFLOW_RUN_NOT_SUCCESSFUL
        wr3.omittedMessage.message == "Previous run failed"
    }

    void "test changeStateToFailed"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        Throwable throwable = new IOException("test")

        when:
        service.changeStateToFailed(workflowStep, throwable)

        then:
        workflowStep.state == WorkflowStep.State.FAILED
        workflowStep.workflowError.message == throwable.message
        workflowStep.workflowRun.state == WorkflowRun.State.FAILED
    }

    void "test changeStateToSuccess, is not last step"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(beanName: "1st job bean")
        createWorkflowArtefact(producedBy: workflowStep.workflowRun, outputRole: "abc")
        workflowStep.workflowRun.workflow.beanName = "workflow bean"
        workflowStep.workflowRun.workflow.save(flush: true)
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(workflowStep.workflowRun) >> Mock(OtpWorkflow) {
                1 * getJobBeanNames() >> ["1st job bean", "2nd job bean"]
            }
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
        WorkflowStep workflowStep = createWorkflowStep(beanName: "2nd job bean")
        createWorkflowArtefact(producedBy: workflowStep.workflowRun, outputRole: "abc")
        workflowStep.workflowRun.workflow.beanName = "workflow bean"
        workflowStep.workflowRun.workflow.save(flush: true)
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(workflowStep.workflowRun) >> Mock(OtpWorkflow) {
                1 * getJobBeanNames() >> ["1st job bean", "2nd job bean"]
            }
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
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.changeStateToRunning(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.RUNNING
        workflowStep.workflowRun.state == WorkflowRun.State.RUNNING_OTP
    }
}
