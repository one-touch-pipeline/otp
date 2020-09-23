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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled

import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.TransactionUtils
import de.dkfz.tbi.otp.workflow.jobs.Job
import de.dkfz.tbi.otp.workflow.restartHandler.AutoRestartHandlerService
import de.dkfz.tbi.otp.workflow.restartHandler.ErrorNotificationService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException

import static grails.async.Promises.task

class JobScheduler {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ErrorNotificationService errorNotificationService

    @Autowired
    JobService jobService

    @Autowired
    NotificationCreator notificationCreator

    @Autowired
    AutoRestartHandlerService autoRestartHandlerService

    @Autowired
    WorkflowStateChangeService workflowStateChangeService

    @Autowired
    WorkflowSystemService workflowSystemService

    @Scheduled(fixedDelay = 1000L)
    void scheduleJob() {
        if (workflowSystemService.enabled) {
            WorkflowStep step = CollectionUtils.atMostOneElement(
                    WorkflowStep.findAllByState(WorkflowStep.State.CREATED, [sort: 'id', order: 'asc', max: 1])
            )
            if (step) {
                workflowStateChangeService.changeStateToRunning(step)
                task {
                    executeAndCheckJob(step)
                }
            }
        }
    }

    @SuppressWarnings("CatchThrowable")
    protected void executeAndCheckJob(WorkflowStep workflowStep) {
        assert workflowStep
        try {
            Job job = applicationContext.getBean(workflowStep.beanName, Job)
            TransactionUtils.withNewTransaction {
                job.execute(workflowStep)
            }
            if (workflowStep.state == WorkflowStep.State.SUCCESS && workflowStep.workflowRun.state == WorkflowRun.State.RUNNING) {
                jobService.createNextJob(workflowStep.workflowRun)
            } else if (workflowStep.state == WorkflowStep.State.SUCCESS && workflowStep.workflowRun.state == WorkflowRun.State.SUCCESS) {
                notifyUsers(workflowStep)
            } else if (workflowStep.state == WorkflowStep.State.FAILED && workflowStep.workflowRun.state != WorkflowRun.State.FAILED) {
                throw new JobSchedulerException("Workflow step is in state `FAILED`, but the run is in state `${workflowStep.workflowRun.state}")
            } else if (workflowStep.state == WorkflowStep.State.RUNNING) {
                throw new JobSchedulerException("Workflow step is still in state `RUNNING` after the job finished")
            } else if (workflowStep.state == WorkflowStep.State.FAILED) {
                autoRestartHandlerService.handleRestarts(workflowStep)
            }
        } catch (Throwable t) {
            try {
                workflowStateChangeService.changeStateToFailed(workflowStep, t)
                autoRestartHandlerService.handleRestarts(workflowStep)
            } catch (Throwable t2) {
                errorNotificationService.sendMaintainer(workflowStep, t2)
            }
        }
    }

    protected void notifyUsers(WorkflowStep workflowStep) {
        assert workflowStep
        notificationCreator.processFinished(errorNotificationService.getSeqTracks(workflowStep))
    }
}

class JobSchedulerException extends WorkflowException {
    JobSchedulerException(String message) {
        super(message)
    }
}
