/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.scheduler

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.JobMailService
import de.dkfz.tbi.otp.job.plan.ValidatingJobDefinition
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.restarting.RestartHandlerService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.ExceptionUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import java.security.SecureRandom

import static org.springframework.util.Assert.notNull

/**
 * Class handling the scheduling of Jobs.
 *
 * This class controls the execute method of a {@link Job}. It ensures that a {@link ProcessingStepUpdate} is
 * created when the execute method is entered and leaved. As well it ensures that the {@link ProcessingStep} is
 * in the correct state when it is called.
 *
 * In case the execute method throws an exception this is intercepted and the error handling is invoked. After
 * a Job finished this class takes care of invoking the next to be executed Job and triggers it.
 *
 * @deprecated old job system
 * @see Job
 * @see ProcessingStep
 * @see ProcessingStepUpdate
 */
@Deprecated
@Component("scheduler")
@Slf4j
class Scheduler {

    @Autowired
    SchedulerService schedulerService

    @Autowired
    ErrorLogService errorLogService

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    JobMailService jobMailService

    @Autowired
    ProcessService processService

    @Autowired
    RestartHandlerService restartHandlerService

    /**
     * Calls the job's {@link Job#execute()} method in the context of the job and with error handling.
     */
    void executeJob(final Job job) {
        notNull job
        doCreateCheck(job)
        doWithErrorHandling(job, {
            doInJobContext(job, { job.execute() })
            doEndCheck(job)
        })
    }

    /**
     * Executes part of a {@link Job} with error handling.
     *
     * <p>Note that job code execution should also be wrapped in {@link #doInJobContext(Job, Closure)}.</p>
     *
     * @param closure The part of the job that shall be executed.
     * @param rethrow Whether an exception shall be rethrown by this method after it has been handled. If false, the
     * exception will be logged.
     */
    void doWithErrorHandling(final Job job, final Closure closure, final boolean rethrow = true) {
        notNull job
        notNull closure
        try {
            closure()
        } catch (final Throwable e) {
            doErrorHandling(job, e)
            if (rethrow) {
                throw e
            } else {
                log.error "Exception occurred in ${job} with processing step ID ${job.processingStep?.id}.", e
            }
        }
    }

    /**
     * Executes part of a {@link Job} in the context of the job.
     *
     * <p>Note that job code execution should also be wrapped in {@link #doWithErrorHandling(Job, Closure)}.</p>
     *
     * @param closure The part of the job that shall be executed.
     */
    void doInJobContext(final Job job, final Closure closure) {
        notNull job
        notNull closure
        schedulerService.startingJobExecutionOnCurrentThread(job)
        try {
            closure()
        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(job)
        }
    }

    /**
     * Method for before execution of a Job.
     *
     * This method takes care of creating the ProcessingStepUpdate for state STARTED when the Job
     * gets executed. It will throw a RuntimeException in case that there are severe errors such as
     * the Job does not have a ProcessingStep or there is not at least the CREATED ProcessingStepUpdate
     * available.
     *
     * This method is also responsible for persisting the input parameters passed to the Job at the time
     * of execution.
     */
    private void doCreateCheck(final Job job) {
        try {
            // verify that the Job has a processing Step
            if (!job.processingStep) {
                log.error("Job of type ${job.class} executed without a ProcessingStep being set")
                throw new ProcessingException("Job executed without a ProcessingStep being set")
            }
            SessionUtils.withTransaction {
                ProcessingStep step = ProcessingStep.getInstance(job.processingStep.id)
                // get the last ProcessingStepUpdate
                List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(step)
                if (existingUpdates.isEmpty()) {
                    log.error("Job of type ${job.class} executed before entering the CREATED state")
                    throw new ProcessingException("Job executed before entering the CREATED state")
                } else if (existingUpdates.size() > 1) {
                    if (existingUpdates.sort { it.id }.last().state == ExecutionState.FAILURE ||
                            existingUpdates.sort { it.id }.last().state == ExecutionState.RESTARTED) {
                        // scheduler is already in failed state - no reason to process
                        throw new ProcessingException("Job already in failed condition before execution")
                    }
                }
                if (job instanceof ValidatingJob) {
                    ValidatingJobDefinition validator = step.jobDefinition as ValidatingJobDefinition
                    ProcessingStep validatedStep = CollectionUtils.atMostOneElement(
                            ProcessingStep.findAllByProcessAndJobDefinition(step.process, validator.validatorFor))
                    (job as ValidatingJob).validatorFor = validatedStep
                }
            }
            // add a ProcessingStepUpdate to the ProcessingStep
            SessionUtils.withTransaction {
                ProcessingStep step = ProcessingStep.getInstance(job.processingStep.id)
                processService.setOperatorIsAwareOfFailure(step.process, false)
            }
            SessionUtils.withTransaction {
                ProcessingStep step = ProcessingStep.getInstance(job.processingStep.id)
                List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(step)
                ProcessingStepUpdate update = new ProcessingStepUpdate(
                        date: new Date(),
                        state: ExecutionState.STARTED,
                        previous: existingUpdates.sort { it.date }.last(),
                        processingStep: step
                )
                if (!update.save(flush: true)) {
                    log.error("Could not create a STARTED Update for Job of type ${job.class}")
                    throw new ProcessingException("Could not create a STARTED Update for Job")
                }
                log.debug("doCreateCheck performed for ${job} with ProcessingStep ${job.processingStep.id}")
            }
            job.start()
        } catch (RuntimeException e) {
            jobMailService.sendErrorNotification(job, e?.message ?: "No Exception message")
            // removing Job from running
            schedulerService.removeRunningJob(job)
            throw new SchedulerException("doCreateCheck failed for Job of type ${job.class}", e)
        }
    }

    /**
     * Method for after execution of a Job.
     *
     * This method takes care of creating the ProcessingStepUpdate for state FINISHED after a Job
     * execution ended. If the Job is an EndStateAwareJob it will also create the Update for the state
     * provided by the Job. In case of a failure the error handling is invoked.
     *
     * This method takes also care of persisting the output parameters provided by the Job and will invoke
     * the next Job of the JobExecutionPlan.
     */
    private void doEndCheck(final Job job) {
        if (job instanceof MonitoringJob) {
            // These kind of jobs are allowed to finish the execute method before their processing is finished
            // They will invoke the doEndCheck in the SchedulerService by themselves.
            return
        }
        schedulerService.doEndCheck(job)
    }

    /**
     * Method for after exception got thrown during Job execution.
     *
     * This method logs the exception, and stores a failure update for the ProcessingStep. As well it triggers the error
     * handling process for the Job's JobExecutionPlan.
     */
    private void doErrorHandling(Job job, Throwable exceptionToBeHandled) {
        try {
            doUnsafeErrorHandling(job, exceptionToBeHandled)
        } catch (final Throwable exceptionDuringExceptionHandling) {
            final String identifier = System.currentTimeMillis() + "-" + sprintf('%016X', new SecureRandom().nextLong())
            job.log.error "An exception was thrown during exception handling. The original exception (ID ${identifier}), " +
                    "which triggered the exception handling, is:\n" +
                    "${ExceptionUtils.getStackTrace(exceptionToBeHandled)}\n" +
                    "And the exception which was thrown during exception handling is:\n" +
                    "${ExceptionUtils.getStackTrace(exceptionDuringExceptionHandling)}"
            throw new RuntimeException(
                    "An exception was thrown during exception handling. See the log for the original exception (ID ${identifier}).",
                    exceptionDuringExceptionHandling)
        }
    }

    /**
     * The error handling done in this method may fail. If you do not expect exceptions to be thrown from error
     * handling, use {@link #doErrorHandling(Job, Throwable)} instead.
     */
    private void doUnsafeErrorHandling(Job job, Throwable exceptionToBeHandled) {
        ProcessingStep step = ProcessingStep.getInstance(job.processingStep.id)
        try {
            schedulerService.removeRunningJob(job)
            // add a ProcessingStepUpdate to the ProcessingStep
            processService.setOperatorIsAwareOfFailure(step.process, false)
            ProcessingStepUpdate update = new ProcessingStepUpdate(
                    date: new Date(),
                    state: ExecutionState.FAILURE,
                    previous: step.latestProcessingStepUpdate,
                    processingStep: step
            )
            update.save(flush: true)
            String errorHash = null
            try {
                errorHash = errorLogService.log(exceptionToBeHandled)
            } catch (Exception exceptionDuringLogging) {
                log.error "Another exception occured trying to log an exception for ProcessingStepUpdate ${update.id}\n" +
                        "original exception:\n" +
                        "${ExceptionUtils.getStackTrace(exceptionToBeHandled)}\n" +
                        "exception during exception logging:\n" +
                        "${ExceptionUtils.getStackTrace(exceptionDuringLogging)}", exceptionDuringLogging
            }
            ProcessingError error = new ProcessingError(
                    errorMessage: exceptionToBeHandled.message ?: "No Exception message",
                    processingStepUpdate: update,
                    stackTraceIdentifier: errorHash
            )
            error.save(flush: true)
            update.error = error
            if (!update.save(flush: true)) {
                log.error("Could not create a FAILURE Update for Job of type ${job.class}")
                throw new ProcessingException("Could not create a FAILURE Update for Job")
            }
            markProcessAsFailed(step)
            log.debug("doErrorHandling performed for ${job.class} with ProcessingStep ${step.id}")
            restartHandlerService.handleRestart(job)
        } finally {
            jobMailService.sendErrorNotification(job, exceptionToBeHandled?.message ?: "No Exception message")
        }
    }

    /**
     * Helper function to set a Process as failed and send out an error notification.
     * @param step The ProcessingStep which failed
     * @param error The error message why this step failed.
     */
    private void markProcessAsFailed(ProcessingStep step) {
        schedulerService.markProcessAsFailed(step)
    }
}
