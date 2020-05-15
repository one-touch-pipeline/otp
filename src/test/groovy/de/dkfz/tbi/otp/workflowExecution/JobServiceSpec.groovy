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
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog

class JobServiceSpec extends Specification implements ServiceUnitTest<JobService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Workflow,
                WorkflowMessageLog,
                WorkflowRun,
        ]
    }

    void "test createNextJob, first step"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        workflowRun.workflow.beanName = "workflow bean"
        workflowRun.workflow.save(flush: true)
        service.applicationContext = Mock(ApplicationContext) {
            getBean("workflow bean", OtpWorkflow) >> { [getJobBeanNames: { ["1st job bean", "2nd job bean"] }] as OtpWorkflow }
        }

        when:
        service.createNextJob(workflowRun)

        then:
        workflowRun.state == WorkflowRun.State.RUNNING
        WorkflowStep.all.size() == 1
        WorkflowStep.first() == workflowRun.workflowSteps.last()
        WorkflowStep.first().state == WorkflowStep.State.CREATED
        WorkflowStep.first().previous == null
        WorkflowStep.first().workflowRun == workflowRun
        WorkflowStep.first().beanName == "1st job bean"
        workflowRun.workflowSteps == WorkflowStep.all
    }

    void "test createNextJob, existing step"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        WorkflowStep existingStep = createWorkflowStep(workflowRun: workflowRun, beanName: "1st job bean")
        workflowRun.workflow.beanName = "workflow bean"
        workflowRun.workflow.save(flush: true)
        service.applicationContext = Mock(ApplicationContext) {
            getBean("workflow bean", OtpWorkflow) >> { [getJobBeanNames: { ["1st job bean", "2nd job bean"] }] as OtpWorkflow }
        }

        when:
        service.createNextJob(workflowRun)

        then:
        workflowRun.state == WorkflowRun.State.RUNNING
        WorkflowStep.all.size() == 2
        WorkflowStep newStep = (WorkflowStep.all - existingStep).find()
        existingStep == workflowRun.workflowSteps.first()
        newStep == workflowRun.workflowSteps.last()
        newStep.state == WorkflowStep.State.CREATED
        newStep.previous == existingStep
        newStep.workflowRun == workflowRun
        newStep.beanName == "2nd job bean"
        workflowRun.workflowSteps == WorkflowStep.all
    }

    void "test createNextJob, workflow.beanName not set"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        workflowRun.workflow.beanName = null
        workflowRun.workflow.save(flush: true)

        when:
        service.createNextJob(workflowRun)

        then:
        thrown(AssertionError)
    }

    void "test createRestartedJob"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        WorkflowStep step1 = createWorkflowStep(workflowRun: workflowRun)
        WorkflowStep stepToRestart = createWorkflowStep(workflowRun: workflowRun)
        WorkflowStep step2 = createWorkflowStep(workflowRun: workflowRun)
        workflowRun.save(flush: true)

        when:
        service.createRestartedJob(stepToRestart)

        then:
        !step1.obsolete
        [stepToRestart, step2].every { it.obsolete }
        !workflowRun.workflowSteps.last().obsolete
        workflowRun.workflowSteps.last().beanName == stepToRestart.beanName
        workflowRun.workflowSteps.last().state == WorkflowStep.State.CREATED
        workflowRun.workflowSteps.last().previous == step2
        workflowRun.workflowSteps.last().restartedFrom == stepToRestart
    }

    void "test createRestartedJobAfterJobFailure"() {
        given:
        WorkflowStep failedStep = createWorkflowStep(state: WorkflowStep.State.FAILED)
        failedStep.workflowRun.state = WorkflowRun.State.FAILED
        failedStep.workflowRun.save(flush: true)
        JobService service = Spy(JobService) {
            1 * createRestartedJob(failedStep) >> { }
        }

        when:
        service.createRestartedJobAfterJobFailure(failedStep)

        then:
        notThrown()
    }

    @Unroll
    void "test createRestartedJobAfterJobFailure, not failed"() {
        given:
        WorkflowStep failedStep = createWorkflowStep(state: stepState)
        failedStep.workflowRun.state = runState
        failedStep.workflowRun.save(flush: true)

        when:
        service.createRestartedJobAfterJobFailure(failedStep)

        then:
        thrown(AssertionError)

        where:
        stepState                  | runState                  || _
        WorkflowStep.State.FAILED  | WorkflowRun.State.SUCCESS || _
        WorkflowStep.State.SUCCESS | WorkflowRun.State.FAILED  || _
        WorkflowStep.State.SUCCESS | WorkflowRun.State.SUCCESS || _
    }

    void "test createRestartedJobAfterSystemRestart"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.RUNNING)
        workflowStep.workflowRun.state = WorkflowRun.State.RUNNING
        workflowStep.workflowRun.save(flush: true)
        JobService service = Spy(JobService) {
            1 * createRestartedJob(workflowStep) >> { }
        }

        when:
        service.createRestartedJobAfterSystemRestart(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.FAILED
        CollectionUtils.exactlyOneElement(workflowStep.logs).displayLog().contains("OTP restarted")
    }

    @Unroll
    void "test createRestartedJobAfterSystemRestart, not running"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: stepState)
        workflowStep.workflowRun.state = runState
        workflowStep.workflowRun.save(flush: true)

        when:
        service.createRestartedJobAfterJobFailure(workflowStep)

        then:
        thrown(AssertionError)

        where:
        stepState                  | runState                  || _
        WorkflowStep.State.RUNNING | WorkflowRun.State.SUCCESS || _
        WorkflowStep.State.SUCCESS | WorkflowRun.State.RUNNING || _
        WorkflowStep.State.SUCCESS | WorkflowRun.State.SUCCESS || _
    }
}
