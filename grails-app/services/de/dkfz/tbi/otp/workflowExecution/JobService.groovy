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

import grails.gorm.transactions.Transactional

@Transactional
class JobService {

    LogService logService

    void createNextJob(WorkflowRun workflowRun) {
        assert workflowRun
        assert workflowRun.workflow.beanName

        OtpWorkflow otpWorkflow = applicationContext.getBean(workflowRun.workflow.beanName, OtpWorkflow)
        String beanName = workflowRun.workflowSteps ?
                    otpWorkflow.jobBeanNames[otpWorkflow.jobBeanNames.indexOf(workflowRun.workflowSteps.last().beanName) + 1] :
                    otpWorkflow.jobBeanNames.first()

        new WorkflowStep(
                workflowRun: workflowRun,
                beanName: beanName,
                state: WorkflowStep.State.CREATED,
                previous: workflowRun.workflowSteps ? workflowRun.workflowSteps.last() : null,
        ).save(flush: true)
        workflowRun.state = WorkflowRun.State.RUNNING
        workflowRun.save(flush: true)
    }

    private void createRestartedJob(WorkflowStep stepToRestart) {
        assert stepToRestart
        WorkflowStep workflowStep = new WorkflowStep(
                beanName: stepToRestart.beanName,
                state: WorkflowStep.State.CREATED,
                previous: stepToRestart.workflowRun.workflowSteps.last(),
                restartedFrom: stepToRestart,
        ).save(flush: true)

        stepToRestart.workflowRun.workflowSteps[stepToRestart.workflowRun.workflowSteps.indexOf(stepToRestart)..(stepToRestart.workflowRun.workflowSteps.size() - 1)].each { WorkflowStep step ->
            step.obsolete = true
            step.save(flush: true)
        }

        stepToRestart.workflowRun.workflowSteps.add(workflowStep)
        stepToRestart.workflowRun.save(flush: true)
    }

    void createRestartedJobAfterJobFailure(WorkflowStep stepToRestart) {
        assert stepToRestart

        WorkflowRun workflowRun = stepToRestart.workflowRun
        assert workflowRun.state == WorkflowRun.State.FAILED

        WorkflowStep failedStep = workflowRun.workflowSteps.last()
        assert failedStep.state == WorkflowStep.State.FAILED

        createRestartedJob(stepToRestart)
    }

    void createRestartedJobAfterSystemRestart(WorkflowStep workflowStep) {
        assert workflowStep
        assert workflowStep.state == WorkflowStep.State.RUNNING && workflowStep.workflowRun.state == WorkflowRun.State.RUNNING
        workflowStep.state = WorkflowStep.State.FAILED
        logService.addSimpleLogEntry(workflowStep, "OTP restarted")
        createRestartedJob(workflowStep)
    }
}
