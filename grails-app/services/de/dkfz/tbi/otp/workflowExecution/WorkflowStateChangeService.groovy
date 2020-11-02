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

import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.utils.StackTraceUtils

@Transactional
class WorkflowStateChangeService {
    void changeStateToSkipped(WorkflowStep step, SkippedMessage message) {
        assert step
        assert message
        step.state = WorkflowStep.State.SKIPPED
        step.save(flush: true)

        step.workflowRun.state = WorkflowRun.State.SKIPPED
        step.workflowRun.skippedMessage = message
        step.workflowRun.save(flush: true)

        step.workflowRun.outputArtefacts.each { String role, WorkflowArtefact workflowArtefact ->
            workflowArtefact.state = WorkflowArtefact.State.SKIPPED
            workflowArtefact.save(flush: true)
        }

        getDependingWorkflowRuns(step.workflowRun).each { WorkflowRun workflowRun ->
            if (workflowRun.state == WorkflowRun.State.PENDING) {
                workflowRun.state = WorkflowRun.State.SKIPPED
                workflowRun.skippedMessage = message
                workflowRun.save(flush: true)
                workflowRun.outputArtefacts.each { String role, WorkflowArtefact workflowArtefact ->
                    if (workflowArtefact.state == WorkflowArtefact.State.PLANNED_OR_RUNNING) {
                        workflowArtefact.state = WorkflowArtefact.State.SKIPPED
                        workflowArtefact.save(flush: true)
                    }
                }
            }
        }
    }

    void changeStateToWaitingOnUser(WorkflowStep step) {
        assert step
        step.workflowRun.state = WorkflowRun.State.WAITING_ON_USER
        step.workflowRun.save(flush: true)

        step.state = WorkflowStep.State.SUCCESS
        step.save(flush: true)
    }

    void changeStateToWaitingOnSystem(WorkflowStep step) {
        assert step
        step.workflowRun.state = WorkflowRun.State.WAITING_ON_SYSTEM
        step.workflowRun.save(flush: true)

        step.state = WorkflowStep.State.SUCCESS
        step.save(flush: true)
    }

    void changeStateToFinalFailed(WorkflowStep step) {
        assert step
        step.workflowRun.state = WorkflowRun.State.FAILED_FINAL
        step.workflowRun.save(flush: true)

        step.workflowRun.outputArtefacts.each { String role, WorkflowArtefact workflowArtefact ->
            workflowArtefact.state = WorkflowArtefact.State.FAILED
            workflowArtefact.save(flush: true)
        }

        getDependingWorkflowRuns(step.workflowRun).each { WorkflowRun workflowRun ->
            if (workflowRun.state == WorkflowRun.State.PENDING) {
                workflowRun.state = WorkflowRun.State.SKIPPED
                workflowRun.skippedMessage = new SkippedMessage(
                        message: "Previous run failed",
                        category: SkippedMessage.Category.PREREQUISITE_WORKFLOW_RUN_NOT_SUCCESSFUL,
                ).save(flush: true)
                workflowRun.save(flush: true)

                workflowRun.outputArtefacts.each { String role, WorkflowArtefact workflowArtefact ->
                    if (workflowArtefact.state == WorkflowArtefact.State.PLANNED_OR_RUNNING) {
                        workflowArtefact.state = WorkflowArtefact.State.SKIPPED
                        workflowArtefact.save(flush: true)
                    }
                }
            }
        }
    }

    void changeStateToFailed(WorkflowStep step, Throwable throwable) {
        assert step
        step.workflowRun.state = WorkflowRun.State.FAILED
        step.workflowRun.save(flush: true)

        step.workflowError = new WorkflowError(message: throwable.message, stacktrace: StackTraceUtils.getStackTrace(throwable))
        step.state = WorkflowStep.State.FAILED
        step.save(flush: true)
    }

    void changeStateToSuccess(WorkflowStep step) {
        assert step
        step.state = WorkflowStep.State.SUCCESS
        step.save(flush: true)

        String lastBeanName = null
        if (step.workflowRun.workflow.beanName) {
            OtpWorkflow otpWorkflow = applicationContext.getBean(step.workflowRun.workflow.beanName, OtpWorkflow)
            lastBeanName = otpWorkflow.jobBeanNames.last()
        }

        if (step.beanName == lastBeanName) {
            step.workflowRun.state = WorkflowRun.State.SUCCESS
            step.workflowRun.save(flush: true)
            step.workflowRun.outputArtefacts.each { String role, WorkflowArtefact workflowArtefact ->
                workflowArtefact.state = WorkflowArtefact.State.SUCCESS
                workflowArtefact.save(flush: true)
            }
        }
    }

    void changeStateToRunning(WorkflowStep step) {
        assert step
        step.workflowRun.state = WorkflowRun.State.RUNNING
        step.workflowRun.save(flush: true)

        step.state = WorkflowStep.State.RUNNING
        step.save(flush: true)
    }

    private Collection<WorkflowRun> getDependingWorkflowRuns(WorkflowRun run) {
        assert run
        List<WorkflowRun> runs = WorkflowRunInputArtefact.findAllByWorkflowArtefactInList(run.outputArtefacts*.value)*.workflowRun
        return runs + runs.collectMany { getDependingWorkflowRuns(it) }
    }
}
