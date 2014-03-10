package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.job.plan.ValidatingJobDefinition
import de.dkfz.tbi.otp.job.processing.DecisionJob
import de.dkfz.tbi.otp.job.processing.DecisionProcessingStep
import de.dkfz.tbi.otp.job.processing.EndStateAwareJob
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.JobExcecutionException
import de.dkfz.tbi.otp.job.processing.LoggingException;
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.PbsJob
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.ValidatingJob
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.notification.NotificationEvent
import de.dkfz.tbi.otp.notification.NotificationType
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.apache.commons.logging.LogFactory
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.JoinPoint
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Aspect handling the scheduling of Jobs.
 * 
 * This Aspect controls the execute Method of a {@link Job}. It ensures that a {@link ProcessingStepUpdate} is
 * created when the execute Method is entered and leaved. As well it ensures that the {@link ProcessingStep} is
 * in the correct state when it is called.
 *
 * For technical reasons the Aspect uses the {@link JobExecution} annotation for the pointcut.
 * 
 * In case the execute method throws an exception this is intercepted and the error handling is invoked. After
 * a Job finished the Aspect takes care of invoking the next to be executed Job and triggers it.
 *
 * @see Job
 * @see JobExecution
 * @see ProcessingStep
 * @see ProcessingStepUpdate
 */
@Component("scheduler")
@Aspect
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

    /**
     * Aspect for before execution of a Job.
     *
     * This Aspect takes care of creating the ProcessingStepUpdate for state STARTED when the Job
     * gets executed. It will throw a RuntimeException in case that there are severe errors such as
     * the Job does not have a ProcessingStep or there is not at least the CREATED ProcessingStepUpdate
     * available.
     * 
     * The Aspect is also responsible for persisting the input parameters passed to the Job at the time
     * of execution.
     *
     * @param joinPoint The JoinPoint describing the intercepted method call on the Job.
     */
    @Before("@annotation(de.dkfz.tbi.otp.job.scheduler.JobExecution) && this(de.dkfz.tbi.otp.job.processing.Job)")
    public void doCreateCheck(JoinPoint joinPoint) {
        Job job = joinPoint.target as Job
        LogThreadLocal.setJobLog(job.log)
        try {
            // verify that the Job has a processing Step
            if (!job.processingStep) {
                log.fatal("Job of type ${joinPoint.target.class} executed without a ProcessingStep being set")
                throw new ProcessingException("Job executed without a ProcessingStep being set")
            }
            ProcessingStep step = ProcessingStep.get(job.processingStep.id)
            // get the last ProcessingStepUpdate
            List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(step)
            if (existingUpdates.isEmpty()) {
                log.fatal("Job of type ${joinPoint.target.class} executed before entering the CREATED state")
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
                log.fatal("Could not create a STARTED Update for Job of type ${joinPoint.target.class}")
                throw new ProcessingException("Could not create a STARTED Update for Job")
            }
            log.debug("doCreateCheck performed for ${joinPoint.getTarget().class} with ProcessingStep ${job.processingStep.id}")
            job.start()
            // send notification
            NotificationEvent event = new NotificationEvent(this, step, NotificationType.PROCESS_STEP_STARTED)
            grailsApplication.mainContext.publishEvent(event)
        } catch (RuntimeException e) {
            LogThreadLocal.removeJobLog()
            // removing Job from running
            schedulerService.removeRunningJob(job)
            throw new SchedulerException("Could not create @Before annotation for Job of type ${joinPoint.target.class}", e.cause)
        }
    }

    /**
     * Aspect for after execution of a Job.
     *
     * This Aspect takes care of creating the ProcessingStepUpdate for state FINISHED after a Job
     * execution ended. If the Job is an EndStateAwareJob it will also create the Update for the state
     * provided by the Job. In case of a failure the error handling is invoked.
     *
     * The Aspect takes also care of persisting the output parameters provided by the Job and will invoke
     * the next Job of the JobExecutionPlan.
     *
     * @param joinPoint The JoinPoint describing the intercepted method call on the Job.
     */
    @AfterReturning("@annotation(de.dkfz.tbi.otp.job.scheduler.JobExecution) && this(de.dkfz.tbi.otp.job.processing.Job)")
    public void doEndCheck(JoinPoint joinPoint) {
        LogThreadLocal.removeJobLog()
        Job job = joinPoint.target as Job
        if (job instanceof MonitoringJob) {
            // These kind of jobs are allowed to finish the execute method before their processing is finished
            // They will invoke the doEndCheck in the SchedulerService by themselves.
            return
        }
        schedulerService.doEndCheck(job)
    }

    /**
     * Aspect for after exception got thrown during Job execution.
     *
     * It is important to know that the aspect is not able to eat the exception. The exception will propagate to
     * the calling class. That is the service method which triggers the Job execution has to wrap it in a try/catch block
     * and eat the exception itself.
     *
     * The aspect logs the exception, and stores a failure update for the ProcessingStep. As well it triggers the error
     * handling process for the Job's JobExecutionPlan.
     *
     * @param joinPoint The JoinPoint we intercept
     * @param e The intercepted Exception
     */
    @AfterThrowing(pointcut="@annotation(de.dkfz.tbi.otp.job.scheduler.JobExecution) && this(de.dkfz.tbi.otp.job.processing.Job)", throwing="e")
    public void doErrorHandling(JoinPoint joinPoint, Exception e) {
        LogThreadLocal.removeJobLog()
        Job job = joinPoint.target as Job
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
            log.fatal("Could not create a FAILURE Update for Job of type ${joinPoint.target.class}")
            throw new ProcessingException("Could not create a FAILURE Update for Job")
        }
        markProcessAsFailed(step, error.errorMessage)
        log.debug("doErrorHandling performed for ${joinPoint.getTarget().class} with ProcessingStep ${step.id}")
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
