/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.administration

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.springframework.http.*
import spock.lang.Specification

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError

class CrashRepairControllerSpec extends Specification implements ControllerUnitTest<CrashRepairController>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowRun,
                WorkflowError,
                Project,
        ]
    }

    void setup() {
        controller.workflowStateChangeService = Mock(WorkflowStateChangeService)
        controller.workflowService = Mock(WorkflowService)
        controller.jobService = Mock(JobService)
    }

    void "test restartWorkflowStep_ShouldReturnStatusOkAndContentTypeJSON"() {
        given:
        WorkflowRun run = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
                jobCanBeRestarted: true,
        ])

        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
                state: WorkflowStep.State.RUNNING,
        ])

        WorkflowStepCommand cmd = new WorkflowStepCommand(workflowStep: step)

        when:
        request.method = HttpMethod.POST.toString()
        controller.restartWorkflowStep(cmd)

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == MediaType.APPLICATION_JSON_UTF8_VALUE
    }

    void "test restartWorkflowStep_shouldSendBadRequest_whenServiceFails"() {
        given:
        controller.jobService  = Mock(JobService) {
            _ * createRestartedJobAfterSystemRestart(_) >> { throw new WorkflowException() }
        }

        WorkflowRun run = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
                jobCanBeRestarted: false,
        ])

        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
                state: WorkflowStep.State.RUNNING,
        ])

        WorkflowStepCommand cmd = new WorkflowStepCommand(workflowStep: step)

        when:
        request.method = HttpMethod.POST.toString()
        controller.restartWorkflowStep(cmd)

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
    }

    void "test restartWorkflowRun_ShouldReturnStatusOkAndContentTypeJSON"() {
        given:
        WorkflowRun run = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
                jobCanBeRestarted: true,
        ])

        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
                state: WorkflowStep.State.RUNNING,
        ])

        WorkflowStepCommand cmd = new WorkflowStepCommand(workflowStep: step)

        when:
        request.method = HttpMethod.POST.toString()
        controller.restartWorkflowRun(cmd)

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == MediaType.APPLICATION_JSON_UTF8_VALUE
    }

    void "test restartWorkflowRun_ShouldReturnBadRequest_whenServiceFails"() {
        given:
        controller.workflowService = Mock(WorkflowService) {
            _ * createRestartedWorkflow(_) >> { throw new WorkflowException() }
        }

        WorkflowRun run = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
                jobCanBeRestarted: false,
        ])

        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
                state: WorkflowStep.State.RUNNING,
        ])

        WorkflowStepCommand cmd = new WorkflowStepCommand(workflowStep: step)

        when:
        request.method = HttpMethod.POST.toString()
        controller.restartWorkflowRun(cmd)

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
    }

    void "test markWorkflowStepAsFailed_ShouldReturnStatusOkAndContentTypeJSON"() {
        given:
        WorkflowRun run = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
                jobCanBeRestarted: true,
        ])

        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
                state: WorkflowStep.State.RUNNING,
        ])

        WorkflowStepCommand cmd = new WorkflowStepCommand(workflowStep: step)

        when:
        request.method = HttpMethod.POST.toString()
        controller.markWorkflowStepAsFailed(cmd)

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == MediaType.APPLICATION_JSON_UTF8_VALUE
    }

    void "test markWorkflowStepAsFailed_ShouldReturnBadRequest_whenServiceFails"() {
        given:
        controller.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            _ * changeStateToFailedWithManualChangedError(_) >> { throw new OtpRuntimeException() }
        }

        WorkflowRun run = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
                jobCanBeRestarted: true,
        ])

        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
                state: WorkflowStep.State.RUNNING,
        ])

        WorkflowStepCommand cmd = new WorkflowStepCommand(workflowStep: step)

        when:
        request.method = HttpMethod.POST.toString()
        controller.markWorkflowStepAsFailed(cmd)

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
    }

    void "test markWorkflowRunAsFinalFailed_ShouldReturnStatusOkAndContentTypeJSON"() {
        given:
        WorkflowRun run = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
                jobCanBeRestarted: true,
        ])

        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
                state: WorkflowStep.State.RUNNING,
        ])

        WorkflowStepCommand cmd = new WorkflowStepCommand(workflowStep: step)

        when:
        request.method = HttpMethod.POST.toString()
        controller.markWorkflowRunAsFinalFailed(cmd)

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == MediaType.APPLICATION_JSON_UTF8_VALUE
    }

    void "test markWorkflowRunAsFinalFailed_ShouldReturnBadRequest_whenServiceFails"() {
        given:
        controller.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            _ * changeStateToFinalFailed(_) >> { throw new OtpRuntimeException() }
        }

        WorkflowRun run = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
                jobCanBeRestarted: true,
        ])

        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
                state: WorkflowStep.State.RUNNING,
        ])

        WorkflowStepCommand cmd = new WorkflowStepCommand(workflowStep: step)

        when:
        request.method = HttpMethod.POST.toString()
        controller.markWorkflowRunAsFinalFailed(cmd)

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
    }
}
