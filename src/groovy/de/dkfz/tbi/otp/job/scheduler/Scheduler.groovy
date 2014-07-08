package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.job.plan.ValidatingJobDefinition
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.ValidatingJob
import de.dkfz.tbi.otp.notification.NotificationEvent
import de.dkfz.tbi.otp.notification.NotificationType
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.infrastructure.ProcessingStepThreadLocal
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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
 * @see Job
 * @see ProcessingStep
 * @see ProcessingStepUpdate
 */
@Component("scheduler")
@SuppressWarnings(["CatchException", "CatchRuntimeException"])
class Scheduler {
    /**
     * Dependency Injection of Scheduler Service
     */
    @Autowired
    SchedulerService schedulerService
    /**
     * Dependency Injection of Error Log Service
     */
    @Autowired
    ErrorLogService errorLogService
    /**
     * Dependency Injection of grailsApplication
     */
    @Autowired
    GrailsApplication grailsApplication
    /**
     * Log for this class.
     */
    private static final log = LogFactory.getLog(this)

    public void executeJob(final Job job) {
        doCreateCheck(job)
        try {
            job.execute()
            doEndCheck(job)
        } catch (final Throwable e) {
            doErrorHandlingForExecute(job, e)
            throw e
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
    public void doCreateCheck(final Job job) {
        LogThreadLocal.setThreadLog(job.log)
        try {
            // verify that the Job has a processing Step
            if (!job.processingStep) {
                log.fatal("Job of type ${job.class} executed without a ProcessingStep being set")
                throw new ProcessingException("Job executed without a ProcessingStep being set")
            }
            ProcessingStep step = ProcessingStep.get(job.processingStep.id)
            ProcessingStepThreadLocal.setProcessingStep(step)
            // get the last ProcessingStepUpdate
            List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(step)
            if (existingUpdates.isEmpty()) {
                log.fatal("Job of type ${job.class} executed before entering the CREATED state")
                throw new ProcessingException("Job executed before entering the CREATED state")
            } else if (existingUpdates.size() > 1) {
                if (existingUpdates.sort{ it.id }.last().state == ExecutionState.FAILURE ||
                    existingUpdates.sort{ it.id }.last().state == ExecutionState.RESTARTED) {
                    // scheduler is already in failed state - no reason to process
                    throw new ProcessingException("Job already in failed condition before execution")
                }
            }
            if (job instanceof ValidatingJob) {
                ValidatingJobDefinition validator = step.jobDefinition as ValidatingJobDefinition
                ProcessingStep validatedStep = ProcessingStep.findByProcessAndJobDefinition(step.process, validator.validatorFor)
                (job as ValidatingJob).setValidatorFor(validatedStep)
            }
            // add a ProcessingStepUpdate to the ProcessingStep
            ProcessingStepUpdate update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.STARTED,
                previous: existingUpdates.sort { it.date }.last(),
                processingStep: step
                )
            if (!update.save(flush: true)) {
                log.fatal("Could not create a STARTED Update for Job of type ${job.class}")
                throw new ProcessingException("Could not create a STARTED Update for Job")
            }
            log.debug("doCreateCheck performed for ${job} with ProcessingStep ${job.processingStep.id}")
            job.start()
            // send notification
            NotificationEvent event = new NotificationEvent(this, step, NotificationType.PROCESS_STEP_STARTED)
            grailsApplication.mainContext.publishEvent(event)
        } catch (RuntimeException e) {
            LogThreadLocal.removeThreadLog()
            ProcessingStepThreadLocal.removeProcessingStep()
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
    public void doEndCheck(final Job job) {
        LogThreadLocal.removeThreadLog()
        ProcessingStepThreadLocal.removeProcessingStep()
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
    public void doErrorHandlingForExecute(final Job job, final Throwable e) {
        LogThreadLocal.removeThreadLog()
        ProcessingStepThreadLocal.removeProcessingStep()
        doErrorHandling(job, e)
    }

    public void doErrorHandling(Job job, Throwable e) {
        schedulerService.removeRunningJob(job)
        ProcessingStep step = ProcessingStep.get(job.processingStep.id)
        // get the last ProcessingStepUpdate
        List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(step)
        // add a ProcessingStepUpdate to the ProcessingStep
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FAILURE,
            previous: step.latestProcessingStepUpdate,
            processingStep: step
            )
        update.save()
        String errorHash = null
        try {
            errorHash = errorLogService.log(e)
        } catch (Exception ex) {
            // do nothing
            //throw new LoggingException("Could not write error log file properly", ex.cause)
        }
        ProcessingError error = new ProcessingError(
            errorMessage: e.message ? e.message.substring(0, Math.min(e.message.length(), 255)) : "No Exception message",
            processingStepUpdate: update,
            stackTraceIdentifier: errorHash
        )
        error.save()
        update.error = error
        if (!update.save(flush: true)) {
            // TODO: trigger error handling
            log.fatal("Could not create a FAILURE Update for Job of type ${job.class}")
            throw new ProcessingException("Could not create a FAILURE Update for Job")
        }
        markProcessAsFailed(step, error.errorMessage)
        log.debug("doErrorHandling performed for ${job.class} with ProcessingStep ${step.id}")
    }

    /**
     * Helper function to set a Process as failed and send out an error notification.
     * @param step The ProcessignStep which failed
     * @param error The error message why this step failed.
     */
    private void markProcessAsFailed(ProcessingStep step, String error) {
        schedulerService.markProcessAsFailed(step, error)
    }

    /**
     * Helper method to create a ProcessingError and add it to given ProcessingStep.
     * Includes creating the Failure ProcessingStepUpdate.
     * @param step The ProcessingStep for which the Error needs to be created.
     * @param previous The ProcessingStepUpdate which should be used as the previous update
     * @param errorMessage The message to be stored for the Error
     * @param jobClass The Job Class to use in logging in case of severe error
     * @throws ProcessingException In case the ProcessingError cannot be saved.
     **/
    private void createError(ProcessingStep step, ProcessingStepUpdate previous, String errorMessage, Class jobClass) {
        schedulerService.createError(step, previous, errorMessage, jobClass)
    }
}
