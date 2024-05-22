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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.jobs.Job
import de.dkfz.tbi.otp.workflow.restartHandler.AutoRestartHandlerService
import de.dkfz.tbi.otp.workflow.restartHandler.ErrorNotificationService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException

import static grails.async.Promises.task

@CompileDynamic
@Slf4j
@Component
class JobScheduler {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ErrorNotificationService errorNotificationService

    @Autowired
    LogService logService

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
            WorkflowStep step = SessionUtils.withTransaction {
                CollectionUtils.atMostOneElement(
                        WorkflowStep.findAllByState(WorkflowStep.State.CREATED, [sort: 'id', order: 'asc', max: 1])
                )
            }
            if (step) {
                SessionUtils.withTransaction {
                    step.refresh()
                    log.debug("Found job to starting asyncron: ${step.displayInfo()}")
                    workflowStateChangeService.changeStateToRunning(step)
                }
                task {
                    executeAndCheckJob(step)
                }
            }
        }
    }

    @SuppressWarnings("CatchThrowable")
    protected void executeAndCheckJob(WorkflowStep workflowStep) {
        try {
            assert workflowStep
            executeJob(workflowStep)
            checkResult(workflowStep)
        } catch (Throwable exceptionInJob) {
            handleException(workflowStep, exceptionInJob)
        }
    }

    @Transactional
    private void executeJob(WorkflowStep workflowStep) {
        workflowStep.refresh()
        log.debug("Start job: ${workflowStep.displayInfo()}")
        logService.addSimpleLogEntry(workflowStep, "Start")
        Job job = applicationContext.getBean(workflowStep.beanName, Job)
        ExecutedCommandLogCallbackThreadLocalHolder.withCommandLogCallback(new WorkflowStepCommandCallback(logService, workflowStep)) {
            job.execute(workflowStep)
        }
        log.debug("Finish job: ${workflowStep.displayInfo()}")
        logService.addSimpleLogEntry(workflowStep, "End")
    }

    @Transactional
    private void checkResult(WorkflowStep workflowStep) {
        workflowStep.refresh()
        if (workflowStep.state == WorkflowStep.State.SUCCESS && workflowStep.workflowRun.state == WorkflowRun.State.RUNNING_OTP) {
            jobService.createNextJob(workflowStep.workflowRun)
        } else if (workflowStep.state == WorkflowStep.State.SUCCESS && workflowStep.workflowRun.state == WorkflowRun.State.SUCCESS) {
            notifyUsers(workflowStep)
        } else if (workflowStep.state == WorkflowStep.State.FAILED && workflowStep.workflowRun.state != WorkflowRun.State.FAILED) {
            throw new JobSchedulerException("Workflow step is in state `FAILED`, but the run is in state `${workflowStep.workflowRun.state}")
        } else if (workflowStep.state == WorkflowStep.State.RUNNING) {
            throw new JobSchedulerException("Workflow step is still in state `RUNNING_OTP` after the job finished")
        } else if (workflowStep.state == WorkflowStep.State.FAILED) {
            autoRestartHandlerService.handleRestarts(workflowStep)
        } else if (workflowStep.state == WorkflowStep.State.SKIPPED && workflowStep.workflowRun.state == WorkflowRun.State.SKIPPED_MISSING_PRECONDITION) {
            notifyUsers(workflowStep)
        }
    }

    @SuppressWarnings("CatchThrowable")
    private void handleException(WorkflowStep workflowStep, Throwable exceptionInJob) {
        try {
            WorkflowStep.withTransaction {
                workflowStep.refresh()
                workflowStateChangeService.changeStateToFailedWithManualChangedError(workflowStep, exceptionInJob)
                logService.addSimpleLogEntry(workflowStep, "Failed")
                log.debug("Failed job: ${workflowStep.displayInfo()}")
            }
            WorkflowStep.withTransaction {
                workflowStep.refresh()
                autoRestartHandlerService.handleRestarts(workflowStep)
            }
        } catch (Throwable exceptionInExceptionHandling) {
            try {
                WorkflowStep.withTransaction {
                    workflowStep.refresh()
                    errorNotificationService.sendMaintainer(workflowStep, exceptionInJob, exceptionInExceptionHandling)
                }
            } catch (Throwable exceptionInSendingSimpleMail) {
                String messageToLog = [
                        "Fail to send simple mail to maintainer for job: ${workflowStep}",
                        "Exception in job:",
                        StackTraceUtils.getStackTrace(exceptionInJob),
                        "Exception in exception handling:",
                        StackTraceUtils.getStackTrace(exceptionInExceptionHandling),
                        "Exception in simple mail sending:",
                        StackTraceUtils.getStackTrace(exceptionInSendingSimpleMail),
                ].join('\n')
                log.error(messageToLog)
            }
        }
    }

    protected void notifyUsers(WorkflowStep workflowStep) {
        assert workflowStep
        notificationCreator.processFinished(errorNotificationService.getSeqTracks(workflowStep))
    }
}

@InheritConstructors
class JobSchedulerException extends WorkflowException {
}
