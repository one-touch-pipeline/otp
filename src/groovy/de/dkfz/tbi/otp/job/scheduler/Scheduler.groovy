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
        Job job = joinPoint.target as Job
        job.end()
        schedulerService.removeRunningJob(job)
        ProcessingStep step = ProcessingStep.get(job.processingStep.id)
        // get the last ProcessingStepUpdate
        List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(step)
        // add a ProcessingStepUpdate to the ProcessingStep
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FINISHED,
            previous: existingUpdates.sort { it.date }.last(),
            processingStep: step
            )
        if (!update.save(flush: true)) {
            log.fatal("Could not create a FINISHED Update for Job of type ${joinPoint.target.class}")
            throw new ProcessingException("Could not create a FINISHED Update for Job")
        }
        Parameter failedOutputParameter
        job.getOutputParameters().each { Parameter param ->
            if (param.type.parameterUsage != ParameterUsage.OUTPUT) {
                failedOutputParameter = param
                return // continue
            }
            if (param.type.jobDefinition != step.jobDefinition) {
                failedOutputParameter = param
                return // continue
            }
            step.addToOutput(param)
        }
        if (!step.save(flush: true)) {
            log.fatal("Could not create a FINISHED Update for Job of type ${joinPoint.target.class}")
            throw new ProcessingException("Could not create a FINISHED Update for Job")
        }
        if (job instanceof PbsJob) {
            List<String> pbsIds = (job as PbsJob).getPbsIds()
            if (pbsIds.empty) {
                // list of Process IDs is empty - watchdog cannot be started
                createError(step, update, "PbsJob does not provide PBS Process Ids", joinPoint.target.class)
                log.error("PbsJob for JobDefinition ${step.jobDefinition.id} does not provide PBS Process Ids")
                // TODO Proper error handling here
                return
            }
            String pbsParameterValue = ""
            pbsIds.eachWithIndex { id, i ->
                if (i > 0) {
                    pbsParameterValue = pbsParameterValue + ","
                }
                pbsParameterValue = pbsParameterValue + id
            }
            ParameterType pbsIdType = ParameterType.findByJobDefinitionAndParameterUsageAndName(step.jobDefinition, ParameterUsage.OUTPUT, "__pbsIds")
            if (!pbsIdType) {
                // output type is missing
                createError(step, update, "PbsJob does not have required output parameter type", joinPoint.target.class)
                log.error("PbsJob for JobDefinition ${step.jobDefinition.id} does not have required output parameter type")
                // TODO Proper error handling here
                return
            }
            Parameter pbsIdParameter = new Parameter(type: pbsIdType, value: pbsParameterValue)
            step.addToOutput(pbsIdParameter)
            // get the Realm
            Long realmId = (job as PbsJob).getRealm()
            Realm realm = Realm.get(realmId)
            if (!realm) {
                // output type is missing
                createError(step, update, "PbsJob does not provide the Realm it is operating on or Realm Id is incorrect", joinPoint.target.class)
                log.error("PbsJob for JobDefinition ${step.jobDefinition.id} does not provide the Realm it is operating on or Realm Id is incorrect")
                // TODO Proper error handling here
                return
            }
            ParameterType pbsRealmType = ParameterType.findByJobDefinitionAndParameterUsageAndName(step.jobDefinition, ParameterUsage.OUTPUT, "__pbsRealm")
            if (!pbsRealmType) {
                // output type is missing
                createError(step, update, "PbsJob does not have required output parameter type for pbs realm", joinPoint.target.class)
                log.error("PbsJob for JobDefinition ${step.jobDefinition.id} does not have required output parameter type for pbs realm")
                // TODO Proper error handling here
                return
            }
            Parameter realmIdParameter = new Parameter(type: pbsRealmType, value: realmId)
            step.addToOutput(realmIdParameter)
            if (!step.save(flush: true)) {
                log.fatal("Could not add the PbsIds Parameter to Job of type ${joinPoint.target.class}")
                throw new ProcessingException("Could not add the PbsIds Parameter to Job")
            }
        }
        if (failedOutputParameter) {
            // at least one output parameter is wrong - set to failure
            createError(step, update, "Parameter ${failedOutputParameter.value} is either not defined for JobDefintion ${step.jobDefinition.id} or not of type Output.", joinPoint.target.class)
            log.error("Parameter ${failedOutputParameter.value} is either not defined for JobDefintion ${step.jobDefinition.id} or not of type Output.")
            // TODO Proper error handling here
            return
        }
        // check that all Output Parameters are set
        List<ParameterType> parameterTypes = ParameterType.findAllByJobDefinitionAndParameterUsage(step.jobDefinition, ParameterUsage.OUTPUT)
        for (ParameterType parameterType in parameterTypes) {
            boolean found = false
            for (Parameter param in step.output) {
                if (param.type == parameterType) {
                    found = true
                    break
                }
            }
            if (!found) {
                // a required output parameter has not been generated
                createError(step, update, "Required Output Parameter of type ${parameterType.id} is not set.", joinPoint.target.class)
                log.error("Required Output Parameter of type ${parameterType.id} is not set.")
                // TODO Proper error handling here
                return
            }
        }

        // test whether the Job knows if it ended
        if (job instanceof EndStateAwareJob) {
            EndStateAwareJob endStateAwareJob = job as EndStateAwareJob
            ProcessingStepUpdate endStateUpdate = new ProcessingStepUpdate(
                date: new Date(),
                state: endStateAwareJob.getEndState(),
                previous: update,
                processingStep: step
                )
            endStateUpdate.save()
            if (job instanceof DecisionJob && endStateUpdate.state == ExecutionState.SUCCESS) {
                ((DecisionProcessingStep)step).decision = (job as DecisionJob).getDecision()
            }
            if (!step.save(flush: true)) {
                log.fatal("Could not create a ERROR/SUCCESS Update for Job of type ${joinPoint.target.class}")
                throw new JobExcecutionException("Could not create a ERROR/SUCCESS Update for Job")
            }
            if (endStateAwareJob.getEndState() == ExecutionState.FAILURE) {
                log.debug("Something went wrong in endStateAwareJob of type ${joinPoint.target.class}, execution state set to FAILURE")
                ProcessingError error = new ProcessingError(errorMessage: "Something went wrong in endStateAwareJob of type ${joinPoint.target.class}, execution state set to FAILURE", processingStepUpdate: endStateUpdate)
                endStateUpdate.error = error
                if (!error.save(flush: true)) {
                    log.fatal("Could not create a FAILURE Update for Job of type ${jobClass}")
                    throw new ProcessingException("Could not create a FAILURE Update for Job of type ${jobClass}")
                }
                Process process = Process.get(step.process.id)
                process.finished = true
                if (!process.save(flush: true)) {
                    // TODO: trigger error handling
                    log.fatal("Could not set Process to finished")
                    throw new ProcessingException("Could not set Process to finished")
                }
                log.debug("doEndCheck performed for ${joinPoint.getTarget().class} with ProcessingStep ${step.id}")
                return
            }
            if (job instanceof ValidatingJob) {
                ValidatingJob validatingJob = job as ValidatingJob
                // get the last ProcessingStepUpdate
                ProcessingStep validatedStep = ProcessingStep.get(validatingJob.validatorFor.id)
                List<ProcessingStepUpdate> existingValidatingUpdates = ProcessingStepUpdate.findAllByProcessingStep(validatedStep)
                boolean succeeded = validatingJob.hasValidatedJobSucceeded()

                ProcessingStepUpdate validatedUpdate = new ProcessingStepUpdate(
                    date: new Date(),
                    state: succeeded ? ExecutionState.SUCCESS : ExecutionState.FAILURE,
                    previous: existingValidatingUpdates.sort { it.date }.last(),
                    processingStep: validatedStep
                    )
                validatedUpdate.save()
                if (!succeeded) {
                    // create error
                    ProcessingError validatedError = new ProcessingError(errorMessage: "Marked as failed by validating job", processingStepUpdate: validatedUpdate)
                    validatedError.save()
                    validatedUpdate.error = validatedError
                }
                if (!validatedUpdate.save(flush: true)) {
                    log.fatal("Could not create a FAILED/SUCCEEDED Update for validated job processed by ${joinPoint.target.class}")
                    throw new ProcessingException("Could not create a FAILED/SUCCEEDED Update for validated job")
                }
                if (!succeeded) {
                    Process process = Process.get(step.process.id)
                    process.finished = true
                    if (!process.save(flush: true)) {
                        // TODO: trigger error handling
                        log.fatal("Could not set Process to finished")
                        throw new ProcessingException("Could not set Process to finished")
                    }
                    // do not trigger next generation of Processing Step
                    log.debug("doEndCheck performed for ${joinPoint.getTarget().class} with ProcessingStep ${step.id}")
                    return
                }
            }
        }
        try {
            schedulerService.createNextProcessingStep(step)
        } catch(Exception se) {
            log.error("Could not create new ProcessingStep for Process ${step.process}")
            throw new SchedulerException("Could not create new ProcessingStep", se)
        }
        log.debug("doEndCheck performed for ${joinPoint.getTarget().class} with ProcessingStep ${step.id}")
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
        ProcessingStep step = ProcessingStep.get(job.processingStep.id)
        // get the last ProcessingStepUpdate
        List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(step)
        // add a ProcessingStepUpdate to the ProcessingStep
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FAILURE,
            previous: existingUpdates.sort { it.date }.last(),
            processingStep: step
            )
        update.save()
        String errorHash = null
        try {
            errorHash = errorLogService.log(e)
        } catch (Exception ex) {
            throw new LoggingException("Could not write error log file properly", ex.cause)
        }
        ProcessingError error = new ProcessingError(errorMessage: e.message ? e.message.substring(0, Math.min(e.message.length(), 255)) : "No Exception message", processingStepUpdate: update, stackTraceIdentifier: errorHash)
        error.save()
        update.error = error
        if (!update.save(flush: true)) {
            // TODO: trigger error handling
            log.fatal("Could not create a FAILURE Update for Job of type ${joinPoint.target.class}")
            throw new ProcessingException("Could not create a FAILURE Update for Job")
        }
        Process process = Process.get(step.process.id)
        process.finished = true
        if (!process.save(flush: true)) {
            // TODO: trigger error handling
            log.fatal("Could not set Process to finished")
            throw new ProcessingException("Could not set Process to finished")
        }
        // TODO: trigger error handling
        log.debug("doErrorHandling performed for ${joinPoint.getTarget().class} with ProcessingStep ${step.id}")
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
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FAILURE,
            previous: previous,
            processingStep: step)
        if (!update.save()) {
            log.fatal("Could not create a FAILURE Update for Job of type ${jobClass}")
            throw new ProcessingException("Could not create a FAILURE Update for Job of type ${jobClass}")
        }
        ProcessingError error = new ProcessingError(errorMessage: errorMessage, processingStepUpdate: update)
        update.error = error
        error.save()
        if (!error.save(flush: true)) {
            log.fatal("Could not create a FAILURE Update for Job of type ${jobClass}")
            throw new ProcessingException("Could not create a FAILURE Update for Job of type ${jobClass}")
        }
    }
}
