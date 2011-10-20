package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.job.processing.EndStateAwareJob
import de.dkfz.tbi.otp.job.processing.InvalidStateException;
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.ExecutionState
import org.apache.commons.logging.LogFactory
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.JoinPoint
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
class Scheduler {
    /**
     * Dependency Injection of Scheduler Service
     */
    @Autowired
    SchedulerService schedulerService
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
        try {
            // verify that the Job has a processing Step
            if (!job.processingStep) {
                log.fatal("Job of type ${joinPoint.target.class} executed without a ProcessingStep being set")
                throw new RuntimeException("Job executed without a ProcessingStep being set")
            }
            // get the last ProcessingStepUpdate
            List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(job.processingStep)
            if (existingUpdates.isEmpty()) {
                log.fatal("Job of type ${joinPoint.target.class} executed before entering the CREATED state")
                throw new RuntimeException("Job executed before entering the CREATED state")
            } else if (existingUpdates.size() > 1) {
                if (existingUpdates.sort{ it.id }.last().state == ExecutionState.FAILURE) {
                    // scheduler is already in failed state - no reason to process
                    throw new RuntimeException("Job already in failed condition before execution")
                }
            }
            // add a ProcessingStepUpdate to the ProcessingStep
            ProcessingStepUpdate update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.STARTED,
                previous: existingUpdates.sort { it.date }.last(),
                processingStep: job.processingStep
                )
            job.processingStep.addToUpdates(update)
            if (!job.processingStep.save(flush: true)) {
                log.fatal("Could not create a STARTED Update for Job of type ${joinPoint.target.class}")
                throw new RuntimeException("Could not create a STARTED Update for Job")
            }
            // TODO: persist input parameters
            log.debug("doCreateCheck performed for ${joinPoint.getTarget().class} with ProcessingStep ${job.processingStep.id}")
            job.start()
        } catch (RuntimeException e) {
            // TODO: trigger error handling
            // removing Job from running
            schedulerService.removeRunningJob(job)
            // pass along the exception
            throw e
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
        Job job = joinPoint.target as Job
        job.end()
        schedulerService.removeRunningJob(job)
        // get the last ProcessingStepUpdate
        List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(job.processingStep)
        // add a ProcessingStepUpdate to the ProcessingStep
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FINISHED,
            previous: existingUpdates.sort { it.date }.last()
            )
        job.processingStep.addToUpdates(update)
        job.getOutputParameters().each { Parameter param ->
            job.processingStep.addToOutput(param)
        }
        if (!job.processingStep.save(flush: true)) {
            log.fatal("Could not create a FINISHED Update for Job of type ${joinPoint.target.class}")
            throw new RuntimeException("Could not create a FINISHED Update for Job")
        }

        // test whether the Job knows if it ended
        if (job instanceof EndStateAwareJob) {
            EndStateAwareJob endStateAwareJob = job as EndStateAwareJob
            ProcessingStepUpdate endStateUpdate = new ProcessingStepUpdate(
                date: new Date(),
                state: endStateAwareJob.getEndState(),
                previous: update
                )
            job.processingStep.addToUpdates(endStateUpdate)
            if (!job.processingStep.save(flush: true)) {
                log.fatal("Could not create a ERROR/SUCCESS Update for Job of type ${joinPoint.target.class}")
                throw new RuntimeException("Could not create a ERROR/SUCCESS Update for Job")
            }
            if (endStateAwareJob.getEndState() == ExecutionState.FAILURE) {
                // TODO: do error handling
                // we failed, so don't start next Job
                return
            }
        }
        schedulerService.createNextProcessingStep(job.processingStep)
        log.debug("doEndCheck performed for ${joinPoint.getTarget().class} with ProcessingStep ${job.processingStep.id}")
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
        Job job = joinPoint.target as Job
        schedulerService.removeRunningJob(job)
        // get the last ProcessingStepUpdate
        List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(job.processingStep)
        // add a ProcessingStepUpdate to the ProcessingStep
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FAILURE,
            previous: existingUpdates.sort { it.date }.last()
            )
        // TODO: add the stacktrace identifier
        ProcessingError error = new ProcessingError(errorMessage: e.message, processingStepUpdate: update)
        update.error = error
        error.save()
        job.processingStep.addToUpdates(update)
        if (!job.processingStep.save(flush: true)) {
            // TODO: trigger error handling
            log.fatal("Could not create a FAILURE Update for Job of type ${joinPoint.target.class}")
            throw new RuntimeException("Could not create a FAILURE Update for Job")
        }
        // TODO: trigger error handling
        log.debug("doErrorHandling performed for ${joinPoint.getTarget().class} with ProcessingStep ${job.processingStep.id}")
    }
}
