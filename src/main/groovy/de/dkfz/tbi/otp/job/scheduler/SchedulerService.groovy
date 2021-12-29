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
import org.slf4j.Logger
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.OptionProblem
import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.job.JobMailService
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.ExceptionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import static ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER
import static grails.async.Promises.task
import static org.springframework.util.Assert.notNull

@Deprecated
@Scope("singleton")
@Component
@Slf4j
// this class is NOT transactional
class SchedulerService {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    JobMailService jobMailService

    @Autowired
    ProcessService processService

    @Autowired
    PropertiesValidationService propertiesValidationService

    /**
     * Queue of next to be started ProcessingSteps
     */
    private final Queue<ProcessingStep> queue = [] as Queue
    /**
     * List of currently running Jobs.
     */
    private final List<Job> running = [].asSynchronized()
    /**
     * Lock to protect the internal data from Multi Threading issues
     */
    private final Lock lock = new ReentrantLock()
    /**
     * Whether the Scheduler is currently active.
     * This is false when shutting down the server
     */
    private boolean schedulerActive = false
    /**
     * Whether the scheduler startup has been successful.
     * False as long as the server has not started. If false the scheduler cannot be started,
     * if true the startup cannot be performed again.
     */
    private boolean startupOk = false

    @SuppressWarnings('ThreadLocalNotStaticFinal')
    private final ThreadLocal<Job> jobByThread = new ThreadLocal<Job>()

    boolean isActive() {
        schedulerActive
    }

    /**
     * Starts the Scheduler at Server startup.
     * This method inspects the state of the Job system before the shutdown. In case of a clean
     * Shutdown the scheduler will be started again. All ProcessingSteps in status CREATED will
     * be queued and all ProcessingSteps in status SUSPENDED will be RESUMED and added to the queue.
     * In case the method finds a Process in another state, the server performed an unclean shutdown.
     * This requires manual intervention and the server does not start up. If an unclean shutdown is
     * detected the changes to the ProcessingSteps are discarded, so that this method can be executed
     * once again after the manual intervention fixed all ProcessingSteps in unknown state.
     */
    void startup() {
        if (schedulerActive || startupOk) {
            return
        }
        boolean valid = true
        List<ProcessingStep> processesToRestart = []
        List<ProcessingStep> processesToResume = []

        List<OptionProblem> validationResult = propertiesValidationService.validateProcessingOptions()
        if (!validationResult.isEmpty()) {
            log.error(validationResult.join("\n"))
            valid = false
            return
        }

        try {
            ProcessingStep.withTransaction { status ->
                List<Process> processes = Process.findAllByFinished(false)
                log.info("Found ${processes.size()} running processes")
                List<ProcessingStep> lastProcessingSteps = processes ? ProcessingStep.findAllByProcessInListAndNextIsNull(processes) : []
                lastProcessingSteps.each { ProcessingStep step ->
                    List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step)
                    if (updates.isEmpty()) {
                        status.setRollbackOnly()
                        valid = false
                        log.error("Error during startup: ProcessingStep ${step.id} does not have any Updates")
                        return
                    }
                    ProcessingStepUpdate last = updates.max { it.id }
                    if (last.state == ExecutionState.CREATED) {
                        processesToRestart << step
                    } else if (last.state == ExecutionState.SUSPENDED) {
                        processService.setOperatorIsAwareOfFailure(step.process, false)
                        ProcessingStepUpdate update = new ProcessingStepUpdate(
                                date: new Date(),
                                state: ExecutionState.RESUMED,
                                previous: last,
                                processingStep: step
                        )
                        if (!update.save(flush: true)) {
                            valid = false
                            status.setRollbackOnly()
                            log.error("ProcessingStep ${step.id} could not be resumed")
                        }
                        processesToResume << step
                    } else if (last.state == ExecutionState.RESTARTED) {
                        // look whether there is a RestartedProcessingStep which has a link to step
                        RestartedProcessingStep restarted = RestartedProcessingStep.findByOriginal(step)
                        if (!restarted) {
                            status.setRollbackOnly()
                            valid = false
                            log.error("Error during startup: ProcessingStep ${step.id} is in state ${last.state}, but no RestartedProcessingStep exists")
                        }
                    } else {
                        status.setRollbackOnly()
                        valid = false
                        log.error("Error during startup: ProcessingStep ${step.id} is in state ${last.state}")
                    }
                }
            }
        } catch (Exception e) {
            valid = false
            log.error(e.message, e)
        }
        if (valid) {
            lock.lock()
            try {
                Set<ProcessingStep> toReQueueJobs = new HashSet<ProcessingStep>(processesToRestart)
                toReQueueJobs.removeAll(queue)
                queue.addAll(toReQueueJobs)

                processesToResume.each {
                    Job job = createJob(it)
                    running.add(job)
                    executeInNewThread(job) {
                        grailsApplication.mainContext.scheduler.executeJob(job)
                    }
                }
                Thread.sleep(10000)
            } finally {
                lock.unlock()
            }
            startupOk = true
            schedulerActive = true
        }
        log.info("Scheduler started after server startup: ${schedulerActive}")
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void suspendScheduler() {
        if (!startupOk) {
            return
        }
        schedulerActive = false
        forEachRunningSometimesResumableJob { SometimesResumableJob job -> job.planSuspend() }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void resumeScheduler() {
        if (!startupOk) {
            return
        }
        schedulerActive = true
        forEachRunningSometimesResumableJob { SometimesResumableJob job -> job.cancelSuspend() }
    }

    private void forEachRunningSometimesResumableJob(final Closure closure) {
        lock.lock()
        try {
            running.each {
                if (it instanceof SometimesResumableJob) {
                    closure((SometimesResumableJob) it)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    boolean isStartupOk() {
        return startupOk
    }

    void createNextProcessingStep(ProcessingStep previous) {
        // test whether the Process ended
        if (!previous.jobDefinition.next && !(previous.jobDefinition instanceof DecidingJobDefinition)) {
            endProcess(previous)
            return
        }
        JobDefinition nextJob = previous.jobDefinition.next
        if (!nextJob && (previous.jobDefinition instanceof DecidingJobDefinition)) {
            nextJob = DecisionMapping.findByDecision(((DecisionProcessingStep) previous).decision).definition
        }
        ProcessingStep next = createProcessingStep(previous.process, nextJob, previous.output, previous)
        lock.lock()
        try {
            previous.next = next
            if (!previous.save(flush: true)) {
                throw new SchedulerPersistencyException("Could not save previous ProcessingStep for the process ${previous.process.id}")
            }
            if (!next.process.save(flush: true)) {
                throw new SchedulerPersistencyException("Could not save next ProcessingStep for the process ${next.process.id}")
            }
            queue.add(next)
        } finally {
            lock.unlock()
        }
    }

    /**
     * Creates a new {@link Process} for the {@link JobExecutionPlan} plan triggered by
     * the {@link StartJob}.
     *
     * From the JobExecutionPlan the first {@link JobDefinition} describing the first Job
     * is derived and a {@link ProcessingStep} is created. The {@link Parameter}s passed into
     * the method are mapped to the input parameters accepted by the first JobDefinition.
     * The {@link ProcessParameter} passed into the method is put to the Process.
     * Last but not least the created ProcessingStep gets scheduled.
     * @param startJob The StartJob which wants to trigger the Process
     * @param input List of Parameters provided by the StartJob for this Process.
     * @param processParameter ProcessParameter provided by the StartJob for this Process.
     * @return the new created process
     */
    Process createProcess(StartJob startJob, List<Parameter> input, ProcessParameter processParameter = null) {
        if (!schedulerActive) {
            throw new RuntimeException("Scheduler is disabled")
        }
        JobExecutionPlan plan = JobExecutionPlan.get(startJob.getJobExecutionPlan().id)
        Process process = new Process(started: new Date(),
                jobExecutionPlan: plan,
                startJobClass: startJob.class.getName(),
        )
        if (!process.save(flush: true)) {
            throw new SchedulerPersistencyException("Could not save the process for the JobExecutionPlan ${plan.id}")
        }
        if (processParameter) {
            processParameter.process = process
            if (!processParameter.save(flush: true)) {
                throw new SchedulerPersistencyException("Could not save the process parameter for the Process ${process.id}")
            }
        }
        // create the first processing step
        ProcessingStep step = createProcessingStep(process, JobDefinition.get(plan.firstJob.id), input)
        if (!process.save(flush: true)) {
            throw new SchedulerPersistencyException("Could not save the process for the JobExecutionPlan ${plan.id}")
        }
        lock.lock()
        try {
            queue.add(step)
        } finally {
            lock.unlock()
        }
        return process
    }

    /**
     * Invokes the primitive scheduler to determine which job is to execute next if any at all.
     */
    @Scheduled(fixedRate = 1000L)
    void schedule() throws Exception {
        if (!schedulerActive) {
            return
        }
        if (queue.isEmpty()) {
            return
        }
        Job job = null
        SessionUtils.withNewSession {
            job = doSchedule()
        }

        if (job) {
            executeInNewThread(job) {
                grailsApplication.mainContext.scheduler.executeJob(job)
            }
        }
    }

    protected void executeInNewThread(Job job, Closure closure) {
        task {
            SessionUtils.withNewSession {
                withLoggingContext(job) {
                    closure()
                }
            }
        }
    }

    void withLoggingContext(Job job, Closure closure) {
        MDC.put("PROCESS_AND_JOB_ID", "${job.processingStep.process.id}${File.separator}${job.processingStep.id}")
        try {
            job.log.info("SchedulerService: starting job")
            closure()
            job.log.info(FINALIZE_SESSION_MARKER, "SchedulerService: finished job")
        } finally {
            MDC.clear()
        }
    }

    /**
     * Performs the actual scheduling and returns a scheduled Job if any.
     * @return The Job to run or null
     */
    private Job doSchedule() {
        lock.lock()
        try {
            // TODO: add a proper scheduling method
            // simple scheduler: run a new Job if something is in the queue
            if (queue.isEmpty()) {
                return
            }
            final ProcessingStep stepFromQueue = queue.peek()
            final def id = stepFromQueue.id
            final ProcessingStep stepFromDatabase = ProcessingStep.getInstance(id)
            Job job = createJob(stepFromDatabase)
            running.add(job)
            queue.poll()
            return job
        } finally {
            lock.unlock()
        }
        return null
    }

    /**
     * This method takes care of creating the ProcessingStepUpdate for state FINISHED after a Job
     * execution ended. If the Job is an EndStateAwareJob it will also create the Update for the state
     * provided by the Job. In case of a failure the error handling is invoked.
     *
     * This method takes also care of persisting the output parameters provided by the Job and will invoke
     * the next Job of the JobExecutionPlan.
     *
     * @param job The Job which ended
     */
    void doEndCheck(Job job) {
        job.end()
        removeRunningJob(job)
        ProcessingStep step = ProcessingStep.get(job.processingStep.id)
        if (ProcessingStepUpdate.findByProcessingStepAndState(step, ExecutionState.FAILURE)) {
            job.log.info "SchedulerService.doEndCheck was called for this job, but the job has already failed. A FINISHED ProcessingStepUpdate will NOT " +
                    "be created."
        } else {
            // add a ProcessingStepUpdate to the ProcessingStep
            processService.setOperatorIsAwareOfFailure(step.process, false)
            ProcessingStepUpdate update = new ProcessingStepUpdate(
                    date: new Date(),
                    state: ExecutionState.FINISHED,
                    previous: step.latestProcessingStepUpdate,
                    processingStep: step
            )
            if (!update.save(flush: true)) {
                log.error("Could not create a FINISHED Update for Job of type ${job.class}")
                throw new ProcessingException("Could not create a FINISHED Update for Job")
            }
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
            log.error("Could not create a FINISHED Update for Job of type ${job.class}")
            throw new ProcessingException("Could not create a FINISHED Update for Job")
        }
        if (failedOutputParameter) {
            // at least one output parameter is wrong - set to failure
            createError(step, "Parameter ${failedOutputParameter.value} is either not defined for JobDefintion ${step.jobDefinition.id} or not of " +
                    "type Output.", job.class)
            log.error("Parameter ${failedOutputParameter.value} is either not defined for JobDefintion ${step.jobDefinition.id} or not of type Output.")
            markProcessAsFailed(step)
            return
        }
        // check that all Output Parameters are set
        List<ParameterType> parameterTypes = ParameterType.findAllByJobDefinitionAndParameterUsage(step.jobDefinition, ParameterUsage.OUTPUT)
        for (ParameterType parameterType in parameterTypes) {
            if (!step.output.any { Parameter param -> param.type == parameterType }) {
                // a required output parameter has not been generated
                createError(step, "Required Output Parameter of type ${parameterType.id} is not set.", job.class)
                log.error("Required Output Parameter of type ${parameterType.id} is not set.")
                markProcessAsFailed(step)
                return
            }
        }

        // test whether the Job knows if it ended
        if (job instanceof EndStateAwareJob) {
            try {
                EndStateAwareJob endStateAwareJob = job as EndStateAwareJob
                final ExecutionState endState = endStateAwareJob.getEndState()
                if (endState == ExecutionState.FAILURE) {
                    jobMailService.sendErrorNotification(job, "Something went wrong in endStateAwareJob")
                }
                if (endState != ExecutionState.SUCCESS && endState != ExecutionState.FAILURE) {
                    throw new RuntimeException("Job ${job} has endState ${endState}, but only SUCCESS and FAILURE are allowed.")
                }
                ProcessingStepUpdate endStateUpdate
                if (endState == ExecutionState.FAILURE || !ProcessingStepUpdate.findByProcessingStepAndState(step, ExecutionState.FAILURE)) {
                    processService.setOperatorIsAwareOfFailure(step.process, false)
                    endStateUpdate = new ProcessingStepUpdate(
                            date: new Date(),
                            state: endState,
                            previous: step.latestProcessingStepUpdate,
                            processingStep: step
                    )
                    endStateUpdate.save(flush: true)
                } else {
                    job.log.info "SchedulerService.doEndCheck was called for this job, but the job has already failed. A SUCCESS ProcessingStepUpdate " +
                            "will NOT be created."
                }
                if (job instanceof DecisionJob && endStateUpdate.state == ExecutionState.SUCCESS) {
                    ((DecisionProcessingStep) step).decision = (job as DecisionJob).getDecision()
                }
                if (!step.save(flush: true)) {
                    log.error("Could not create a ERROR/SUCCESS Update for Job of type ${job.class}")
                    throw new JobExcecutionException("Could not create a ERROR/SUCCESS Update for Job")
                }
                if (endState == ExecutionState.FAILURE) {
                    log.debug("Something went wrong in endStateAwareJob of type ${job.class}, execution state set to FAILURE")
                    ProcessingError error = new ProcessingError(errorMessage: "Something went wrong in endStateAwareJob of type ${job.class}, execution " +
                            "state set to FAILURE", processingStepUpdate: endStateUpdate)
                    endStateUpdate.error = error
                    if (!error.save(flush: true)) {
                        log.error("Could not create a FAILURE Update for Job of type ${jobClass}")
                        throw new ProcessingException("Could not create a FAILURE Update for Job of type ${jobClass}")
                    }
                    markProcessAsFailed(step)
                    log.debug("doEndCheck performed for ${job.class} with ProcessingStep ${step.id}")
                    return
                }
                if (job instanceof ValidatingJob) {
                    ValidatingJob validatingJob = job as ValidatingJob
                    // get the last ProcessingStepUpdate
                    ProcessingStep validatedStep = ProcessingStep.get(validatingJob.validatorFor.id)
                    boolean succeeded = validatingJob.hasValidatedJobSucceeded()

                    processService.setOperatorIsAwareOfFailure(step.process, false)
                    ProcessingStepUpdate validatedUpdate = new ProcessingStepUpdate(
                            date: new Date(),
                            state: succeeded ? ExecutionState.SUCCESS : ExecutionState.FAILURE,
                            previous: validatedStep.latestProcessingStepUpdate,
                            processingStep: validatedStep
                    )
                    validatedUpdate.save(flush: true)
                    if (!succeeded) {
                        // create error
                        ProcessingError validatedError = new ProcessingError(
                                errorMessage: "Marked as failed by validating job", processingStepUpdate: validatedUpdate
                        )
                        validatedError.save(flush: true)
                        validatedUpdate.error = validatedError
                    }
                    if (!validatedUpdate.save(flush: true)) {
                        log.error("Could not create a FAILED/SUCCEEDED Update for validated job processed by ${job.class}")
                        throw new ProcessingException("Could not create a FAILED/SUCCEEDED Update for validated job")
                    }
                    if (!succeeded) {
                        Process process = Process.get(step.process.id)
                        process.finished = true
                        if (!process.save(flush: true)) {
                            // TODO: trigger error handling
                            log.error("Could not set Process to finished")
                            throw new ProcessingException("Could not set Process to finished")
                        }
                        // do not trigger next generation of Processing Step
                        log.debug("doEndCheck performed for ${job.class} with ProcessingStep ${step.id}")
                        return
                    }
                }
            } catch (Throwable t) {
                jobMailService.sendErrorNotification(job, t)
                throw t
            }
        }
        try {
            createNextProcessingStep(step)
        } catch (Exception se) {
            log.error("Could not create new ProcessingStep for Process ${step.process}")
            throw new SchedulerException("Could not create new ProcessingStep", se)
        }
        log.debug("doEndCheck performed for ${job.class} with ProcessingStep ${step.id}")
    }

    /**
     * Helper function to set a Process as failed and send out an error notification.
     * @param step The ProcessignStep which failed
     * @param error The error message why this step failed.
     */
    void markProcessAsFailed(ProcessingStep step) {
        Process process = Process.get(step.process.id)
        process.finished = true
        if (!process.save(flush: true)) {
            // TODO: trigger error handling
            log.error("Could not set Process to finished")
            throw new ProcessingException("Could not set Process to finished")
        }
    }

    /**
     * Helper method to create a ProcessingError and add it to given ProcessingStep.
     * Includes creating the Failure ProcessingStepUpdate.
     * @param step The ProcessingStep for which the Error needs to be created.
     * @param errorMessage The message to be stored for the Error
     * @param jobClass The Job Class to use in logging in case of severe error
     * @throws ProcessingException In case the ProcessingError cannot be saved.
     */
    void createError(ProcessingStep step, String errorMessage, Class jobClass) {
        processService.setOperatorIsAwareOfFailure(step.process, false)
        ProcessingStepUpdate update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FAILURE,
                previous: step.latestProcessingStepUpdate,
                processingStep: step)
        if (!update.save(flush: true)) {
            log.error("Could not create a FAILURE Update for Job of type ${jobClass}")
            throw new ProcessingException("Could not create a FAILURE Update for Job of type ${jobClass}")
        }
        ProcessingError error = new ProcessingError(errorMessage: errorMessage, processingStepUpdate: update)
        update.error = error
        error.save(flush: true)
        if (!error.save(flush: true)) {
            log.error("Could not create a FAILURE Update for Job of type ${jobClass}")
            throw new ProcessingException("Could not create a FAILURE Update for Job of type ${jobClass}")
        }
    }

    /**
     * Called by the Scheduler when a Job finished or failed.
     * Removes the Job from the list of running jobs.
     * @param job The Job which finished
     */
    void removeRunningJob(Job job) {
        lock.lock()
        try {
            // need to use an iterator and compare the processing step as identity of the Job
            // may not be the same due to Spring proxying (?)
            Iterator<Job> it = running.iterator()
            while (it.hasNext()) {
                if (job.processingStep == it.next().processingStep) {
                    it.remove()
                }
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Restarts the given ProcessingStep.
     * Checks whether the given ProcessingStep is actually failed and creates a RESTARTED update
     * for it and sets the Process to no longer being finished.
     * If the boolean parameter schedule is provided the updated ProcessingStep is readded to the
     * queue of waiting ProcessingSteps. Setting this parameter to false might make sense for the
     * case that the ProcessingStep will be added to the queue by a different method, e.g. restarting
     * the scheduler.
     * @param step The failed ProcessingStep which needs to be restarted.
     * @param schedule Whether to add the restarted ProcessingStep to the scheduler or not
     */
    void restartProcessingStep(ProcessingStep step, boolean schedule = true, boolean resume3in1job = false) {
        ProcessingStep restartedStep = null
        ProcessingStep.withTransaction {
            final ProcessingStepUpdate lastUpdate = step.latestProcessingStepUpdate
            if (lastUpdate == null) {
                throw new IncorrectProcessingException("ProcessingStep ${step.id} cannot be restarted as it has no updates")
            }
            if (!step.process.finished) {
                throw new IncorrectProcessingException("ProcessingStep ${step.id} cannot be restarted as its Process is not in finished state")
            }
            if (lastUpdate.state != ExecutionState.FAILURE) {
                throw new IncorrectProcessingException("ProcessingStep ${step.id} cannot be restarted as it is in state ${lastUpdate.state}")
            }
            if (step.belongsToMultiJob() && resume3in1job) {
                restartedStep = step
                processService.setOperatorIsAwareOfFailure(step.process, false)
                ProcessingStepUpdate resumed = new ProcessingStepUpdate(
                        date: new Date(),
                        state: ExecutionState.SUSPENDED,
                        previous: lastUpdate,
                        processingStep: restartedStep)
                if (!resumed.save(flush: true)) {
                    throw new SchedulerPersistencyException("Could not save ProcessingStepUpdate for resumed ProcessingStep ${restartedStep.id}")
                }
            } else {
                // create restart event
                processService.setOperatorIsAwareOfFailure(step.process, false)
                ProcessingStepUpdate restart = new ProcessingStepUpdate(
                        date: new Date(),
                        state: ExecutionState.RESTARTED,
                        previous: lastUpdate,
                        processingStep: step)
                if (!restart.save(flush: true)) {
                    log.error("Could not create a RESTARTED Update for ProcessingStep ${step.id}")
                    throw new ProcessingException("Could not create a RESTARTED Update for ProcessingStep ${step.id}")
                }
                restartedStep = RestartedProcessingStep.create(step)
                if (!restartedStep.save(flush: true)) {
                    log.error("Could not create a RestartedProcessingStep for ProcessingStep ${step.id}")
                    throw new SchedulerPersistencyException("Could not create a RestartedProcessingStep for ProcessingStep ${step.id}")
                }
                // Limitation: in case the restartedStep is the first step of the Process and the Process had been started with Input Parameters
                // those will not be available as it is difficult to map them back
                // this is considered as a theoretical problem as input parameters to the first ProcessingStep are from a time when the ProcessParameter
                // did not yet exist and all existing Workflows do not use this feature
                mapInputParamatersToStep(restartedStep, step.previous ? step.previous.output : [])
                // update the previous link
                if (restartedStep.previous) {
                    restartedStep.previous.next = restartedStep
                    if (!restartedStep.previous.save(flush: true)) {
                        log.error("Could not update previous ProcessingStep of ProcessingStep ${step.id}")
                        throw new SchedulerPersistencyException("Could not update previous ProcessingStep of ProcessingStep ${step.id}")
                    }
                }
                processService.setOperatorIsAwareOfFailure(step.process, false)
                ProcessingStepUpdate created = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: restartedStep)
                if (!created.save(flush: true)) {
                    throw new SchedulerPersistencyException("Could not save the first ProcessingStepUpdate for RestartedProcessingStep ${restartedStep.id}")
                }
            }
            Process process = Process.get(step.process.id)
            process.finished = false
            if (!process.save(flush: true)) {
                log.error("Could not set Process ${process.id} to not finished")
                throw new ProcessingException("Could not set Process ${process.id} to not finished")
            }
        }
        // add to queue
        if (schedule) {
            lock.lock()
            try {
                if (restartedStep) {
                    queue.add(restartedStep)
                }
            } finally {
                lock.unlock()
            }
        }
    }

    /**
     * Retrieves from the database all {@link ProcessingStep}s with the latest
     * {@link ProcessingStepUpdate} in {@link ProcessingStepUpdate#state state}
     * {@link ExecutionState#STARTED STARTED} or {@link ExecutionState#RESUMED RESUMED}.
     */
    List<ProcessingStep> retrieveRunningProcessingSteps() {
        List<ProcessingStep> runningSteps = []
        List<Process> process = Process.findAllByFinished(false)
        List<ProcessingStep> lastProcessingSteps = process ? ProcessingStep.findAllByProcessInListAndNextIsNull(process) : []
        lastProcessingSteps.each { ProcessingStep step ->
            ProcessingStepUpdate last = step.latestProcessingStepUpdate
            if (last == null) {
                return
            }
            if (last.state == ExecutionState.STARTED || last.state == ExecutionState.RESUMED) {
                runningSteps << step
            }
        }
        return runningSteps
    }

    /**
     * Checks whether the Job running for the given ProcessingStep is in a resumable state.
     * @return <code>true</code> if the Job is annotated with {@link ResumableJob} or the Job
     * implements {@link SometimesResumableJob} and its {@link SometimesResumableJob#isResumable()}
     * method returns <code>true</code>. Otherwise <code>false</code>.
     */
    boolean isJobResumable(ProcessingStep step) {
        Class jobClass = grailsApplication.classLoader.loadClass(step.jobClass)
        if (jobClass.isAnnotationPresent(ResumableJob)) {
            return true
        }
        if (SometimesResumableJob.class.isAssignableFrom(jobClass)) {
            final SometimesResumableJob job
            lock.lock()
            try {
                job = (SometimesResumableJob) running.find { it.processingStep.id == step.id }
            } finally {
                lock.unlock()
            }
            if (job == null) {
                throw new RuntimeException("No running job found for processing step ${step}.")
            }
            return job.resumable
        }
        return false
    }

    /**
     * Creates a Job for one ProcessingStep.
     */
    private Job createJob(ProcessingStep step) {
        Job job = grailsApplication.mainContext.getBean(step.jobDefinition.bean) as Job
        job.processingStep = step
        step.jobClass = job.class.getName()
        step.save(flush: true)
        return job
    }

    /**
     * Method responsible for handling the successful ending of a Process.
     * @param last The last executed ProcessingStep
     */
    private void endProcess(ProcessingStep last) {
        // TODO: directly sort
        lock.lock()
        try {
            // updating the JobExecutionPlan information has to be thread save due to multiple processes ending in the same time is possible
            ProcessingStepUpdate update = ProcessingStepUpdate.findAllByProcessingStep(last).sort { it.id }.last()
            if (update.state != ExecutionState.SUCCESS) {
                throw new IncorrectProcessingException("Process finished but is not in success state. (Note that currently the last job of a workflow has " +
                        "to be an EndStateAwareJob. See OTP-991.)")
            }
            last.process.finished = true
            last.process.save(flush: true)
        } finally {
            lock.unlock()
        }
    }

    // TODO: comment me
    private ProcessingStep createProcessingStep(Process process, JobDefinition jobDefinition, Collection<Parameter> input, ProcessingStep previous = null) {
        ProcessingStep step = null
        ProcessingStep.withTransaction {
            Map processingStepParameters = [
                    jobDefinition: JobDefinition.get(jobDefinition.id),
                    process: Process.get(process.id),
                    previous: previous ? ProcessingStep.getInstance(previous.id) : null,
            ]
            if (jobDefinition instanceof DecidingJobDefinition) {
                step = new DecisionProcessingStep(processingStepParameters)
            } else {
                step = new ProcessingStep(processingStepParameters)
            }
            if (input && !step.save(flush: true)) {
                // we have to save the next processing step as the ParameterMapping references the JobDefinition
                throw new SchedulerPersistencyException("Could not create new ProcessingStep for Process ${process.id}")
            }
            Parameter failedConstantParameter = mapInputParamatersToStep(step, input)
            step = step.save(flush: true)
            if (!step) {
                throw new SchedulerPersistencyException("Could not save the ProcessingStep for Process ${process.id}")
            }
            processService.setOperatorIsAwareOfFailure(step.process, false)
            ProcessingStepUpdate created = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step)
            if (!created.save(flush: true)) {
                throw new SchedulerPersistencyException("Could not save the first ProcessingStep for Process ${process.id}")
            }
            if (failedConstantParameter) {
                if (!created.save(flush: true)) {
                    throw new SchedulerPersistencyException("Could not save ProcessingStepUpdate for Process ${process.id}")
                }
                processService.setOperatorIsAwareOfFailure(step.process, false)
                ProcessingStepUpdate failure = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: new Date(), previous: created,
                        processingStep: step)
                ProcessingError error = new ProcessingError(errorMessage: "Failed to add constant input parameter ${failedConstantParameter.id} of type" +
                        " ${failedConstantParameter.type.name} to new processing step",
                        processingStepUpdate: failure)
                failure.error = error
                if (!failure.save(flush: true)) {
                    throw new SchedulerPersistencyException("Could not save the ProcessingStep for Process ${process.id}")
                }
            }
        }
        return step
    }

    /**
     * Maps the given input parameters to the ProcessingStep and searches also for constant parameters and creates them.
     *
     * In case one of the constant parameters cannot be created it gets returned. In case everything works fine null is returned.
     * @param step The ProcessingStep for which the Input Parameters need to be created
     * @param input The Output Parameters of the previous Step to be mapped to the input
     * @return Null in success case, failed constant parameter in error case
     */
    private Parameter mapInputParamatersToStep(ProcessingStep step, Collection<Parameter> input) {
        JobDefinition jobDefinition = step.jobDefinition
        input.each { Parameter param ->
            List<ParameterMapping> mappings = ParameterMapping.findAllByFromAndJob(param.type, jobDefinition)
            mappings.each { ParameterMapping mapping ->
                Parameter nextParam = new Parameter(type: mapping.to, value: param.value)
                if (mapping.to.parameterUsage == ParameterUsage.PASSTHROUGH) {
                    step.addToOutput(nextParam)
                } else {
                    step.addToInput(nextParam)
                }
            }
        }
        // add constant parameters to the next processing step
        Parameter failedConstantParameter = null
        jobDefinition.constantParameters.each { Parameter param ->
            if (param.type.parameterUsage != ParameterUsage.INPUT) {
                failedConstantParameter = param
                return // continue
            }
            step.addToInput(Parameter.get(param.id))
        }
        return failedConstantParameter
    }

    /**
     * Must be called by a thread when it starts to execute a {@link Job}.
     * @param job The job that the current thread is about to execute.
     */
    void startingJobExecutionOnCurrentThread(final Job job) {
        assert job
        assert job.log
        final Job currentJob = jobExecutedByCurrentThread
        if (currentJob != null) {
            throw new IllegalStateException("SchedulerService was notified by the current thread that it is about to start executing job ${job}, but " +
                    "SchedulerService thinks that the thread is already executing job ${currentJob}. Apparently the thread did not call " +
                    "SchedulerService.finishedJobExecutionOnCurrentThread() or it called startingJobExecutionOnCurrentThread() more than once.")
        }
        final Logger currentLog = LogThreadLocal.threadLog
        if (currentLog != null) {
            throw new IllegalStateException("SchedulerService was notified by the current thread that it is about to start executing job ${job}, but " +
                    "LogThreadLocal is already holding a log (${currentLog}).")
        }
        jobByThread.set(job)
        LogThreadLocal.threadLog = job.log
    }

    /**
     * Must be called by a thread when it finishes to execute a {@link Job}.
     * @param job The job that the current thread just finished executing.
     */
    void finishedJobExecutionOnCurrentThread(final Job job) {
        //log.debug "Job ${System.identityHashCode(job)}, Thread ${Thread.currentThread()}", new Throwable()
        notNull job
        final Job currentJob = jobExecutedByCurrentThread
        if (currentJob == null) {
            ExceptionUtils.logOrThrow log, new IllegalStateException("SchedulerService was notified by the current thread that it finished executing " +
                    "job ${job}, but SchedulerService thought that the thread is not executing any job. Apparently the thread did not call " +
                    "SchedulerService.startingJobExecutionOnCurrentThread() or it called finishedJobExecutionOnCurrentThread() more than once.")
        } else if (job != currentJob) {
            ExceptionUtils.logOrThrow log, new IllegalStateException("SchedulerService was notified by the current thread that it finished executing " +
                    "job ${job}, but SchedulerService thought that the thread is executing job ${currentJob}.")
        } else {
            final Logger currentLog = LogThreadLocal.threadLog
            if (currentLog != job.log) {
                ExceptionUtils.logOrThrow log, new IllegalStateException("SchedulerService was notified by the current thread that it finished executing " +
                        "job ${job} and SchedulerService thought that LogThreadLocal is holding that job's log, but LogThreadLocal is holding " +
                        "log ${currentLog}.")
            }
        }
        jobByThread.remove()
        LogThreadLocal.removeThreadLog()
    }

    /**
     * @return The {@link Job} which is executed by the current thread or null if the current thread is not executing
     *      any job.
     */
    Job getJobExecutedByCurrentThread() {
        return jobByThread.get()
    }

    Job getJobForProcessingStep(ProcessingStep processingStep) {
        lock.lock()
        try {
            running.find {
                it.processingStep == processingStep
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Getter for Unit Tests
     */
    protected Queue<ProcessingStep> getQueue() {
        return this.queue
    }

    /**
     * Getter for Unit Tests
     */
    List<Job> getRunning() {
        return this.running
    }
}
