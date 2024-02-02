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
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.shared.JobFailedException
import de.dkfz.tbi.otp.workflowExecution.*

class AutoRestartActionServiceSpec extends Specification implements ServiceUnitTest<AutoRestartActionService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowJobErrorDefinition,
        ]
    }

    void "handleActionAndSendMail, when action is RESTART_WORKFLOW, then restart workflow and send mail"() {
        given:
        service.workflowService = Mock(WorkflowService)
        service.errorNotificationService = Mock(ErrorNotificationService)
        service.logService = Mock(LogService)

        WorkflowJobErrorDefinition.Action action = WorkflowJobErrorDefinition.Action.RESTART_WORKFLOW
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowJobErrorDefinition errorDefinition = createWorkflowJobErrorDefinition([
                action: action,
        ])

        when:
        service.handleActionAndSendMail(workflowStep, [errorDefinition], action, null)

        then:
        1 * service.workflowService.createRestartedWorkflow(workflowStep, true)
        1 * service.errorNotificationService.send(workflowStep, action, _, [errorDefinition])
    }

    void "handleActionAndSendMail, when action is RESTART_WORKFLOW but restart workflow fail, then send mail with action stop"() {
        given:
        service.workflowService = Mock(WorkflowService)
        service.errorNotificationService = Mock(ErrorNotificationService)
        service.logService = Mock(LogService)

        WorkflowJobErrorDefinition.Action action = WorkflowJobErrorDefinition.Action.RESTART_WORKFLOW
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowJobErrorDefinition errorDefinition = createWorkflowJobErrorDefinition([
                action: action,
        ])

        when:
        service.handleActionAndSendMail(workflowStep, [errorDefinition], action, null)

        then:
        1 * service.workflowService.createRestartedWorkflow(workflowStep, true) >> { throw new JobFailedException('Fail') }
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, [errorDefinition])
    }

    void "handleActionAndSendMail, when action is RESTART_JOB, then restart the job and send mail"() {
        given:
        service.errorNotificationService = Mock(ErrorNotificationService)
        service.jobService = Mock(JobService)
        service.logService = Mock(LogService)

        WorkflowJobErrorDefinition.Action action = WorkflowJobErrorDefinition.Action.RESTART_JOB
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowStep workflowStepToRestart = createWorkflowStep()
        WorkflowJobErrorDefinition errorDefinition = createWorkflowJobErrorDefinition([
                action: action,
        ])

        when:
        service.handleActionAndSendMail(workflowStep, [errorDefinition], action, errorDefinition.beanToRestart)

        then:
        1 * service.jobService.searchForJobToRestart(workflowStep, errorDefinition.beanToRestart) >> workflowStepToRestart
        1 * service.jobService.createRestartedJobAfterJobFailure(workflowStepToRestart)
        1 * service.errorNotificationService.send(workflowStep, action, _, [errorDefinition])
    }

    void "handleActionAndSendMail, when action is RESTART_JOB but job restart fail, then send mail with action STOP"() {
        given:
        service.errorNotificationService = Mock(ErrorNotificationService)
        service.jobService = Mock(JobService)
        service.logService = Mock(LogService)

        WorkflowJobErrorDefinition.Action action = WorkflowJobErrorDefinition.Action.RESTART_JOB
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowStep workflowStepToRestart = createWorkflowStep()
        WorkflowJobErrorDefinition errorDefinition = createWorkflowJobErrorDefinition([
                action: action,
        ])

        when:
        service.handleActionAndSendMail(workflowStep, [errorDefinition], action, errorDefinition.beanToRestart)

        then:
        1 * service.jobService.searchForJobToRestart(workflowStep, errorDefinition.beanToRestart) >> workflowStepToRestart
        1 * service.jobService.createRestartedJobAfterJobFailure(workflowStepToRestart) >> { throw new JobFailedException('Fail') }
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, [errorDefinition])
    }

    void "handleActionAndSendMail, when action is STOP, then restart the job and send mail"() {
        given:
        service.errorNotificationService = Mock(ErrorNotificationService)
        service.logService = Mock(LogService)

        WorkflowJobErrorDefinition.Action action = WorkflowJobErrorDefinition.Action.STOP
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowJobErrorDefinition errorDefinition = createWorkflowJobErrorDefinition([
                action: action,
        ])

        when:
        service.handleActionAndSendMail(workflowStep, [errorDefinition], action, null)

        then:
        1 * service.errorNotificationService.send(workflowStep, action, _, [errorDefinition])
    }

    void "handleActionAndSendMail, when action is unknown, then restart the job and send mail"() {
        given:
        service.errorNotificationService = Mock(ErrorNotificationService)
        service.logService = Mock(LogService)

        WorkflowJobErrorDefinition.Action action = GroovyMock(WorkflowJobErrorDefinition.Action)
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowJobErrorDefinition errorDefinition = createWorkflowJobErrorDefinition([
                action: action,
        ])

        when:
        service.handleActionAndSendMail(workflowStep, [errorDefinition], action, null)

        then:
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, [errorDefinition])
    }
}
