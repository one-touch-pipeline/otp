/*
 * Copyright 2011-2026 The OTP authors
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
import spock.lang.Unroll

import java.time.ZonedDateTime

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

    void "test changeStateToFailedWithManualChangedError"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        Throwable throwable = new IOException("test")

        when:
        service.changeStateToFailedWithManualChangedError(workflowStep, throwable)

        then:
        workflowStep.state == WorkflowStep.State.FAILED
        workflowStep.jobFinished != null
        workflowStep.workflowError.message == throwable.message
        workflowStep.workflowRun.state == WorkflowRun.State.FAILED
    }

    void "test changeStateToFailedAfterRestart"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.changeStateToFailedAfterRestart(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.FAILED
        workflowStep.jobFinished != null
        workflowStep.workflowRun.state == WorkflowRun.State.FAILED
    }

    void "test changeStateToFinalFailed"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.changeStateToFinalFailed(workflowStep)

        then:
        workflowStep.workflowRun.state == WorkflowRun.State.FAILED_FINAL
        workflowStep.workflowRun.lastJobFinished != null
    }

    @Unroll
    void "test toggleFailedWaitingState"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun(state: state)

        when:
        service.toggleFailedWaitingState(workflowRun)

        then:
        workflowRun.state == expected

        where:
        state                            | expected
        WorkflowRun.State.FAILED         | WorkflowRun.State.FAILED_WAITING
        WorkflowRun.State.FAILED_WAITING | WorkflowRun.State.FAILED
    }

    void "toggleFailedWaitingState, when called for workflow state other than FAILED or FAILED_WAITING, then throw assertion"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun(state: WorkflowRun.State.SUCCESS)

        when:
        service.toggleFailedWaitingState(workflowRun)

        then:
        thrown(AssertionError)
    }

    void "test changeStateToSuccess, is not last step"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(beanName: "1st job bean")
        createWorkflowArtefact(producedBy: workflowStep.workflowRun, outputRole: "abc")
        workflowStep.workflowRun.workflow.beanName = "workflow bean"
        workflowStep.workflowRun.workflow.save(flush: true)
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(workflowStep.workflowRun) >> Mock(OtpWorkflow) {
                _ * getFirstJobBeanName(workflowStep.workflowRun) >> "1st job bean"
                _ * getNextJobBeanName(workflowStep) >> "2nd job bean"
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
                _ * getNextJobBeanName(workflowStep) >> null
            }
        }

        when:
        service.changeStateToSuccess(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.SUCCESS
        workflowStep.jobFinished != null
        workflowStep.workflowRun.state == WorkflowRun.State.SUCCESS
        workflowStep.workflowRun.lastJobFinished != null
        workflowStep.workflowRun.outputArtefacts.every { String s, WorkflowArtefact wa ->
            wa.state == WorkflowArtefact.State.SUCCESS
        }
    }

    void "test changeStateToSuccess, is not last step, does not set finish timestamps"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(beanName: "1st job bean")
        createWorkflowArtefact(producedBy: workflowStep.workflowRun, outputRole: "abc")
        workflowStep.workflowRun.workflow.beanName = "workflow bean"
        workflowStep.workflowRun.workflow.save(flush: true)
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(workflowStep.workflowRun) >> Mock(OtpWorkflow) {
                _ * getNextJobBeanName(workflowStep) >> "2nd job bean"
            }
        }

        when:
        service.changeStateToSuccess(workflowStep)

        then:
        workflowStep.jobFinished != null
        workflowStep.workflowRun.lastJobFinished == null
    }

    void "test changeStateToRunning"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.changeStateToRunning(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.RUNNING
        workflowStep.jobStarted != null
        workflowStep.workflowRun.state == WorkflowRun.State.RUNNING_OTP
        workflowStep.workflowRun.firstJobStarted != null
    }

    void "test changeStateToRunning, does not overwrite existing start timestamps"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        ZonedDateTime existingStepStart = ZonedDateTime.now().minusHours(1)
        ZonedDateTime existingRunStart = ZonedDateTime.now().minusHours(2)
        workflowStep.jobStarted = existingStepStart
        workflowStep.workflowRun.firstJobStarted = existingRunStart

        when:
        service.changeStateToRunning(workflowStep)

        then:
        workflowStep.jobStarted == existingStepStart
        workflowStep.workflowRun.firstJobStarted == existingRunStart
    }
}
