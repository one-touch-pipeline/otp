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

import asset.pipeline.grails.LinkGenerator
import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.restartHandler.BeanToRestartNotFoundInWorkflowRunException
import de.dkfz.tbi.otp.workflow.shared.WorkflowJobIsNotRestartableException
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog

class JobServiceSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    static private final List<WorkflowRun.State> ALLOWED_STATE = [
            WorkflowRun.State.PENDING,
            WorkflowRun.State.RUNNING_OTP,
            WorkflowRun.State.RUNNING_WES,
    ].asImmutable()

    static private final List<WorkflowRun.State> NOT_ALLOWED_STATE = (WorkflowRun.State.values().toList() - ALLOWED_STATE).asImmutable()

    private JobService service

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
                WorkflowMessageLog,
                WorkflowRun,
        ]
    }

    void setup() {
        service = new JobService()
        service.logService = Mock(LogService) {
            _ * addSimpleLogEntry(_, _)
        }
    }

    void "test createNextJob, when first step, then create first job"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(workflowRun) >> Mock(OtpWorkflow) {
                _ * getFirstJobBeanName(workflowRun) >> "1st job bean"
            }
        }

        when:
        service.createNextJob(workflowRun)

        then:
        workflowRun.state == WorkflowRun.State.RUNNING_OTP
        WorkflowStep.all.size() == 1
        WorkflowStep.first() == workflowRun.workflowSteps.last()
        WorkflowStep.first().state == WorkflowStep.State.CREATED
        WorkflowStep.first().previous == null
        WorkflowStep.first().workflowRun == workflowRun
        WorkflowStep.first().beanName == "1st job bean"
        workflowRun.workflowSteps == WorkflowStep.all
    }

    @Unroll
    void "test createNextJob, existing step, when run is in state #state, then create new job"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun(state: state)
        WorkflowStep existingStep = createWorkflowStep(workflowRun: workflowRun, beanName: "1st job bean", state: WorkflowStep.State.SUCCESS)
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(workflowRun) >> Mock(OtpWorkflow) {
                _ * getFirstJobBeanName(workflowRun) >> "1st job bean"
                _ * getNextJobBeanName(existingStep) >> "2nd job bean"
            }
        }

        when:
        service.createNextJob(workflowRun)

        then:
        workflowRun.state == WorkflowRun.State.RUNNING_OTP
        WorkflowStep.all.size() == 2
        WorkflowStep newStep = (WorkflowStep.all - existingStep).find()
        existingStep == workflowRun.workflowSteps.first()
        newStep == workflowRun.workflowSteps.last()
        newStep.state == WorkflowStep.State.CREATED
        newStep.previous == existingStep
        newStep.workflowRun == workflowRun
        newStep.beanName == "2nd job bean"
        workflowRun.workflowSteps == WorkflowStep.all

        where:
        state << ALLOWED_STATE
    }

    @Unroll
    void "test createNextJob, existing step, when run is in state #state, then throw assertion"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun(state: state)
        createWorkflowStep(workflowRun: workflowRun, beanName: "1st job bean", state: WorkflowStep.State.SUCCESS)

        when:
        service.createNextJob(workflowRun)

        then:
        thrown(AssertionError)

        where:
        state << NOT_ALLOWED_STATE
    }

    @Unroll
    void "test createNextJob, existing step, when step is in state #state, then throw assertion"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        createWorkflowStep(workflowRun: workflowRun, beanName: "1st job bean", state: state)

        when:
        service.createNextJob(workflowRun)

        then:
        thrown(AssertionError)

        where:
        state << WorkflowStep.State.values().toList() - WorkflowStep.State.SUCCESS
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

    void "test createRestartedJobAfterJobFailure, last job should restarted"() {
        WorkflowRun workflowRun = createWorkflowRun([
                state: WorkflowRun.State.FAILED,
        ])
        WorkflowStep step1 = createWorkflowStep(workflowRun: workflowRun)
        WorkflowStep stepToRestart = createWorkflowStep([
                workflowRun: workflowRun,
                state      : WorkflowStep.State.FAILED,
        ])
        workflowRun.save(flush: true)
        service.workflowRunService = Mock(WorkflowRunService) {
            1 * it.lockAndRefreshWorkflowRunWithSteps(_)
        }

        when:
        service.createRestartedJobAfterJobFailure(stepToRestart)

        then:
        !step1.obsolete
        stepToRestart.obsolete
        !workflowRun.workflowSteps.last().obsolete
        workflowRun.workflowSteps.last().beanName == stepToRestart.beanName
        workflowRun.workflowSteps.last().state == WorkflowStep.State.CREATED
        workflowRun.workflowSteps.last().previous == stepToRestart
        workflowRun.workflowSteps.last().restartedFrom == stepToRestart
    }

    void "test createRestartedJobAfterJobFailure, job in the middle should restarted"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([
                state: WorkflowRun.State.FAILED,
        ])
        WorkflowStep step1 = createWorkflowStep(workflowRun: workflowRun)
        WorkflowStep stepToRestart = createWorkflowStep(workflowRun: workflowRun)
        WorkflowStep step2 = createWorkflowStep([
                workflowRun: workflowRun,
                state      : WorkflowStep.State.FAILED,
        ])
        workflowRun.save(flush: true)
        service.workflowRunService = Mock(WorkflowRunService) {
            1 * it.lockAndRefreshWorkflowRunWithSteps(_)
        }

        when:
        service.createRestartedJobAfterJobFailure(stepToRestart)

        then:
        !step1.obsolete
        [stepToRestart, step2].every { it.obsolete }
        !workflowRun.workflowSteps.last().obsolete
        workflowRun.workflowSteps.last().beanName == stepToRestart.beanName
        workflowRun.workflowSteps.last().state == WorkflowStep.State.CREATED
        workflowRun.workflowSteps.last().previous == step2
        workflowRun.workflowSteps.last().restartedFrom == stepToRestart
    }

    @Unroll
    void "test createRestartedJobAfterJobFailure, when stepState is #stepState and runState is #runState, then throw assert"() {
        given:
        WorkflowStep failedStep = createWorkflowStep(state: stepState)
        failedStep.workflowRun.state = runState
        failedStep.workflowRun.save(flush: true)

        service.workflowRunService = Mock(WorkflowRunService) {
            1 * it.lockAndRefreshWorkflowRunWithSteps(_)
            0 * _
        }

        when:
        service.createRestartedJobAfterJobFailure(failedStep)

        then:
        thrown(AssertionError)

        where:
        stepState                  | runState
        WorkflowStep.State.FAILED  | WorkflowRun.State.SUCCESS
        WorkflowStep.State.SUCCESS | WorkflowRun.State.FAILED
        WorkflowStep.State.SUCCESS | WorkflowRun.State.SUCCESS
    }

    void "test createRestartedJobAfterJobFailure, when WorkflowStep is in not restartable state, then throw exception"() {
        WorkflowRun workflowRun = createWorkflowRun([
                state            : WorkflowRun.State.FAILED,
                jobCanBeRestarted: false,
        ])
        createWorkflowStep(workflowRun: workflowRun)
        WorkflowStep stepToRestart = createWorkflowStep([
                workflowRun: workflowRun,
                state      : WorkflowStep.State.FAILED,
        ])
        workflowRun.save(flush: true)

        service.linkGenerator = Mock(LinkGenerator) {
            1 * link(_) >> "link"
        }

        service.workflowRunService = Mock(WorkflowRunService) {
            1 * it.lockAndRefreshWorkflowRunWithSteps(_)
            0 * _
        }

        when:
        service.createRestartedJobAfterJobFailure(stepToRestart)

        then:
        thrown(WorkflowJobIsNotRestartableException)
    }

    void "test createRestartedJobAfterSystemRestart"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([
                state: WorkflowRun.State.RUNNING_OTP,
        ])
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
                state      : WorkflowStep.State.RUNNING,
        ])
        service.workflowStateChangeService = new WorkflowStateChangeService()
        service.workflowRunService = Mock(WorkflowRunService) {
            1 * it.lockAndRefreshWorkflowRunWithSteps(_)
        }

        when:
        service.createRestartedJobAfterSystemRestart(workflowStep)

        then:
        workflowStep.state == WorkflowStep.State.FAILED
        workflowRun.workflowSteps.last().beanName == workflowStep.beanName
        workflowRun.workflowSteps.last().state == WorkflowStep.State.CREATED
        workflowRun.workflowSteps.last().previous == workflowStep
        workflowRun.workflowSteps.last().restartedFrom == workflowStep
    }

    void "test createRestartedJobAfterSystemRestart should fail when workflowStep state is not RUNNING"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([
                state: WorkflowRun.State.FAILED,
        ])
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
                state      : WorkflowStep.State.RUNNING,
        ])

        when:
        service.createRestartedJobAfterSystemRestart(workflowStep)

        then:
        thrown(WorkflowJobIsNotRestartableException)
    }

    @Unroll
    void "test createRestartedJobAfterJobFailure, when workflow system not running, then throw assertion"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: stepState)
        workflowStep.workflowRun.state = runState
        workflowStep.workflowRun.save(flush: true)

        service.workflowRunService = Mock(WorkflowRunService) {
            1 * it.lockAndRefreshWorkflowRunWithSteps(_)
            0 * _
        }

        when:
        service.createRestartedJobAfterJobFailure(workflowStep)

        then:
        thrown(AssertionError)

        where:
        stepState                  | runState
        WorkflowStep.State.RUNNING | WorkflowRun.State.SUCCESS
        WorkflowStep.State.SUCCESS | WorkflowRun.State.RUNNING_OTP
        WorkflowStep.State.SUCCESS | WorkflowRun.State.SUCCESS
    }

    @Unroll
    void "searchForJobToRestart, when bean is #beanToRestart, then return step on index #index"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        List<WorkflowStep> workflowSteps = (0..5).collect {
            createWorkflowStep([
                    workflowRun: workflowRun,
                    beanName   : "bean_${it}",
            ])
        }

        when:
        WorkflowStep result = service.searchForJobToRestart(workflowSteps.last(), beanToRestart)

        then:
        result == workflowSteps[index]

        where:
        beanToRestart | index
        'bean_5'      | 5
        'bean_3'      | 3
        'bean_1'      | 1
    }

    void "searchForJobToRestart, when bean is unknown, then throw BeanToRestartNotFoundInWorkflowRunException"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.searchForJobToRestart(workflowStep, "unknown")

        then:
        thrown(BeanToRestartNotFoundInWorkflowRunException)
    }

    @Unroll
    void "searchForJobToRestart, when bean is #beanToRestart, then ignore obsolete and return step on index #index"() {
        given:
        List<WorkflowStep> workflowSteps = createPartlyObsoleteSteps()

        when:
        WorkflowStep result = service.searchForJobToRestart(workflowSteps.last(), beanToRestart)

        then:
        result == workflowSteps[index]

        where:
        beanToRestart | index
        'bean_4'      | 9
        'bean_3'      | 6
        'bean_1'      | 0
    }

    void "searchForJobToRestart, when bean is only obsolete in list, then throw BeanToRestartNotFoundInWorkflowRunException"() {
        given:
        WorkflowStep workflowStep = createPartlyObsoleteSteps().last()

        when:
        service.searchForJobToRestart(workflowStep, "bean_5")

        then:
        thrown(BeanToRestartNotFoundInWorkflowRunException)
    }

    /**
     * Helper to create a run with partly obsolete steps to simulate some restarts
     */
    private List<WorkflowStep> createPartlyObsoleteSteps() {
        WorkflowRun workflowRun = createWorkflowRun()
        return [
                [1, false,],
                [2, false,],
                [3, true,],
                [4, true,],
                [5, true,],
                [4, true,],
                [3, false,],
                [4, true,],
                [5, true],
                [4, false,],
        ].collect {
            createWorkflowStep([
                    workflowRun: workflowRun,
                    beanName   : "bean_${it[0]}",
                    obsolete   : it[1],
            ])
        }
    }
}
