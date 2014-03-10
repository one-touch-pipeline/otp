package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.job.plan.DecidingJobDefinition
import de.dkfz.tbi.otp.job.plan.DecisionMapping
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.DecisionJob
import de.dkfz.tbi.otp.job.processing.DecisionProcessingStep
import de.dkfz.tbi.otp.job.processing.EndStateAwareJob
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.IncorrectProcessingException
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.JobExcecutionException
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.PbsJob
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.RestartedProcessingStep
import de.dkfz.tbi.otp.job.processing.StartJob
import de.dkfz.tbi.otp.job.processing.ValidatingJob
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.notification.NotificationEvent
import de.dkfz.tbi.otp.notification.NotificationType
import de.dkfz.tbi.otp.utils.logging.JobAppender
import de.dkfz.tbi.otp.utils.logging.JobLog

import java.util.concurrent.Callable
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.prepost.PreAuthorize

class SchedulerService {
    static transactional = false
    /**
     * Dependency Injection of grailsApplication
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication
    /**
     * Dependency Injection of ExecutorService
     */
    def executorService
    /**
     * Dependency Injection of PersistenceContextInterceptor
     */
    @SuppressWarnings("GrailsStatelessService")
    PersistenceContextInterceptor persistenceInterceptor
    /**
     * Dependency Injection of PbsMonitorService
     */
    def pbsMonitorService
    /**
     * Queue of next to be started ProcessingSteps
     */
    private final Queue<ProcessingStep> queue = new LinkedList<ProcessingStep>()
    /**
     * List of currently running Jobs.
     */
    private final List<Job> running = []
    /**
     * Lock to protect the internal data from Multi Threading issues
     */
    private final Lock lock = new ReentrantLock()
    /**
     * Whether the Scheduler is currently active.
     * This is false when shutting down the server
     **/
    @SuppressWarnings("GrailsStatelessService")
    private boolean schedulerActive = false
    /**
     * Whether the scheduler startup has been successful.
     * False as long as the server has not started. If false the scheduler cannot be started,
     * if true the startup cannot be performed again.
     **/
    @SuppressWarnings("GrailsStatelessService")
    private boolean startupOk = false

    /**
     * Starts the Scheduler at Server startup.
     * This method inspects the state of the Job system before the shutdown. In case of a clean
     * Shutdown the scheduler will be started again. All ProcessingSteps in status CREATED will
     * be queued and all ProcessingSteps in status SUSPENDED will be RESUMED and added to the queue.
     * In case the method finds a Process in another state, the server performed an unclean shutdown.
     * This requires manual intervention and the server does not start up. If an unclean shutdown is
     * detected the changes to the ProcessingSteps are discarded, so that this method can be executed
     * once again after the manual intervention fixed all ProcessingSteps in unknown state.
     **/
    public void startup() {
        if (schedulerActive || startupOk) {
            return
        }
        boolean ok = true
        List<ProcessingStep> processesToRestart = []
        try {
            ProcessingStep.withTransaction { status ->
                List<Process> processes = Process.findAllByFinished(false)
                List<ProcessingStep> lastProcessingSteps = ProcessingStep.findAllByProcessInListAndNextIsNull(processes)
                lastProcessingSteps.each { ProcessingStep step ->
                    List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step)
                    if (updates.isEmpty()) {
                        status.setRollbackOnly()
                        ok = false
                        log.error("Error during startup: ProcessingStep ${step.id} does not have any Updates")
                        return
                    }
                    ProcessingStepUpdate last = updates.sort { it.id }.last()
                    if (last.state == ExecutionState.CREATED) {
                        processesToRestart << step
                    } else if (last.state == ExecutionState.SUSPENDED) {
                        ProcessingStepUpdate update = new ProcessingStepUpdate(
                            date: new Date(),
                            state: ExecutionState.RESUMED,
                            previous: last,
                            processingStep: step
                        )
                        if (!update.save(flush: true)) {
                            ok = false
                            status.setRollbackOnly()
                            log.error("ProcessingStep ${step.id} could not be resumed")
                        }
                        processesToRestart << step
                    } else if (last.state == ExecutionState.RESTARTED) {
                        // look whether there is a RestartedProcessingStep which has a link to step
                        RestartedProcessingStep restarted = RestartedProcessingStep.findByOriginal(step)
                        if (!restarted) {
                            status.setRollbackOnly()
                            ok = false
                            log.error("Error during startup: ProcessingStep ${step.id} is in state ${last.state}, but no RestartedProcessingStep exists")
                        }
                    } else {
                        status.setRollbackOnly()
                        ok = false
                        log.error("Error during startup: ProcessingStep ${step.id} is in state ${last.state}")
                    }
                }
            }
        } catch (Exception e) {
            ok = false
            log.error(e.message)
            e.printStackTrace()
        }
        if (ok) {
            startupOk = true
            schedulerActive = true
            lock.lock()
            try {
                processesToRestart.each { ProcessingStep step ->
                    for (ProcessingStep queued in queue) {
                        if (queue.id == step.id) {
                            return
                        }
                    }
                    queue << step
                }
            } finally {
                lock.unlock()
            }
        }
        log.info("Scheduler started after server startup: ${schedulerActive}")
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void suspendScheduler() {
        if (!startupOk) {
            return
        }
        schedulerActive = false
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void resumeScheduler() {
        if (!startupOk) {
            return
        }
        schedulerActive = true
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public boolean isStartupOk() {
        return startupOk
    }

    public void createNextProcessingStep(ProcessingStep previous) {
        // test whether the Process ended
        if (!previous.jobDefinition.next && !(previous.jobDefinition instanceof DecidingJobDefinition)) {
            endProcess(previous)
            return
        }
        JobDefinition nextJob = previous.jobDefinition.next
        if (!nextJob && (previous.jobDefinition instanceof DecidingJobDefinition)) {
            nextJob = DecisionMapping.findByDecision(((DecisionProcessingStep)previous).decision).definition
        }
        ProcessingStep next = createProcessingStep(previous.process, nextJob, previous.output, previous)
        lock.lock()
        try {
            previous.next = next
            if (!previous.save()) {
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
     * @return true if the Process got started, false otherwise (e.g. scheduler is not active)
     */
    public boolean createProcess(StartJob startJob, List<Parameter> input, ProcessParameter processParameter = null) {
        if (!schedulerActive) {
            return false
        }
        JobExecutionPlan plan = JobExecutionPlan.get(startJob.getExecutionPlan().id)
        Process process = new Process(started: new Date(),
            jobExecutionPlan: plan,
            startJobClass: startJob.class.getName(),
            startJobVersion: startJob.getVersion()
        )
        if (!process.save()) {
            throw new SchedulerPersistencyException("Could not save the process for the JobExecutionPlan ${plan.id}")
        }
        if (processParameter) {
            processParameter.process = process
            if (!processParameter.save()) {
                throw new SchedulerPersistencyException("Could not save the process parameter for the Process ${process.id}")
            }
        }
        // create the first processing step
        ProcessingStep step = createProcessingStep(process, JobDefinition.get(plan.firstJob.id), input)
        if (!process.save(flush: true)) {
            throw new SchedulerPersistencyException("Could not save the process for the JobExecutionPlan ${plan.id}")
        }
        NotificationEvent event = new NotificationEvent(this, process, NotificationType.PROCESS_STARTED)
        grailsApplication.mainContext.publishEvent(event)
        lock.lock()
        try {
            queue.add(step)
        } finally {
            lock.unlock()
        }
        return true
    }

    /**
     * Invokes the primitive scheduler to determine which job is to execute next if any at all.
     */
    @Scheduled(fixedRate=100l)
    public void schedule() {
        if (!schedulerActive) {
            return
        }
        if (queue.isEmpty()) {
            return
        }
        Job job = null
        persistenceInterceptor.init()
        try {
            job = doSchedule()
        } finally {
            persistenceInterceptor.flush()
            persistenceInterceptor.destroy()
        }

        if (job) {
            // start the Job in an own thread
            executorService.submit({job.execute()} as Callable)
        }
    }

    /**
     * Scheduled method to check the PBS for finished jobs.
     * Execution is wrapped in a persistence interceptor.
     *
     * Do not invoke this method manually.
     */
    @Scheduled(fixedDelay=180000l)
    public void pbsMonitorCheck() {
        if (!schedulerActive) {
            return
        }
        // method to proxy the invocation of PbsMonitorService::check() to workaround strange behavior of Spring
        persistenceInterceptor.init()
        try {
            pbsMonitorService.check()
        } finally {
            persistenceInterceptor.flush()
            persistenceInterceptor.destroy()
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
            final ProcessingStep stepFromDatabase = ProcessingStep.get(id)
            if (stepFromDatabase == null) {
                throw new AssertionError("The ProcessingStep ${stepFromQueue} with id ${id} seems not to exist in the database. That's weird.")
            }
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
    public void doEndCheck(Job job) {
        job.end()
        removeRunningJob(job)
        ProcessingStep step = ProcessingStep.get(job.processingStep.id)
        // add a ProcessingStepUpdate to the ProcessingStep
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FINISHED,
            previous: step.latestProcessingStepUpdate,
            processingStep: step
            )
        if (!update.save(flush: true)) {
            log.fatal("Could not create a FINISHED Update for Job of type ${job.class}")
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
            log.fatal("Could not create a FINISHED Update for Job of type ${job.class}")
            throw new ProcessingException("Could not create a FINISHED Update for Job")
        }
        if (job instanceof PbsJob) {
            List<String> pbsIds = (job as PbsJob).getPbsIds()
            if (pbsIds.empty) {
                // list of Process IDs is empty - watchdog cannot be started
                createError(step, update, "PbsJob does not provide PBS Process Ids", job.class)
                log.error("PbsJob for JobDefinition ${step.jobDefinition.id} does not provide PBS Process Ids")
                markProcessAsFailed(step, "PbsJob for JobDefinition ${step.jobDefinition.id} does not provide PBS Process Ids.")
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
                createError(step, update, "PbsJob does not have required output parameter type", job.class)
                log.error("PbsJob for JobDefinition ${step.jobDefinition.id} does not have required output parameter type")
                markProcessAsFailed(step, "PbsJob for JobDefinition ${step.jobDefinition.id} does not have required output parameter type.")
                return
            }
            Parameter pbsIdParameter = new Parameter(type: pbsIdType, value: pbsParameterValue)
            step.addToOutput(pbsIdParameter)
            // get the Realm
            Long realmId = (job as PbsJob).getRealm()
            Realm realm = Realm.get(realmId)
            if (!realm) {
                // output type is missing
                createError(step, update, "PbsJob does not provide the Realm it is operating on or Realm Id is incorrect", job.class)
                log.error("PbsJob for JobDefinition ${step.jobDefinition.id} does not provide the Realm it is operating on or Realm Id is incorrect")
                markProcessAsFailed(step, "PbsJob for JobDefinition ${step.jobDefinition.id} does not provide the Realm it is operating on or Realm Id is incorrect.")
                return
            }
            ParameterType pbsRealmType = ParameterType.findByJobDefinitionAndParameterUsageAndName(step.jobDefinition, ParameterUsage.OUTPUT, "__pbsRealm")
            if (!pbsRealmType) {
                // output type is missing
                createError(step, update, "PbsJob does not have required output parameter type for pbs realm", job.class)
                log.error("PbsJob for JobDefinition ${step.jobDefinition.id} does not have required output parameter type for pbs realm")
                markProcessAsFailed(step, "PbsJob for JobDefinition ${step.jobDefinition.id} does not have required output parameter type for pbs realm.")
                return
            }
            Parameter realmIdParameter = new Parameter(type: pbsRealmType, value: realmId)
            step.addToOutput(realmIdParameter)
            if (!step.save(flush: true)) {
                log.fatal("Could not add the PbsIds Parameter to Job of type ${job.class}")
                throw new ProcessingException("Could not add the PbsIds Parameter to Job")
            }
        }
        if (failedOutputParameter) {
            // at least one output parameter is wrong - set to failure
            createError(step, update, "Parameter ${failedOutputParameter.value} is either not defined for JobDefintion ${step.jobDefinition.id} or not of type Output.", job.class)
            log.error("Parameter ${failedOutputParameter.value} is either not defined for JobDefintion ${step.jobDefinition.id} or not of type Output.")
            markProcessAsFailed(step, "Parameter ${failedOutputParameter.value} is either not defined for JobDefintion $step.jobDefinition.id} or not of type Output.")
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
                createError(step, update, "Required Output Parameter of type ${parameterType.id} is not set.", job.class)
                log.error("Required Output Parameter of type ${parameterType.id} is not set.")
                markProcessAsFailed(step, "Required Output Parameter of type ${parameterType.id} is not set.")
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
                log.fatal("Could not create a ERROR/SUCCESS Update for Job of type ${job.class}")
                throw new JobExcecutionException("Could not create a ERROR/SUCCESS Update for Job")
            }
            if (endStateAwareJob.getEndState() == ExecutionState.FAILURE) {
                log.debug("Something went wrong in endStateAwareJob of type ${job.class}, execution state set to FAILURE")
                ProcessingError error = new ProcessingError(errorMessage: "Something went wrong in endStateAwareJob of type ${job.class}, execution state set to FAILURE", processingStepUpdate: endStateUpdate)
                endStateUpdate.error = error
                if (!error.save(flush: true)) {
                    log.fatal("Could not create a FAILURE Update for Job of type ${jobClass}")
                    throw new ProcessingException("Could not create a FAILURE Update for Job of type ${jobClass}")
                }
                markProcessAsFailed(step, "Something went wrong in endStateAwareJob of type ${job.class}, execution state set to FAILURE")
                log.debug("doEndCheck performed for ${job.class} with ProcessingStep ${step.id}")
                return
            }
            if (job instanceof ValidatingJob) {
                ValidatingJob validatingJob = job as ValidatingJob
                // get the last ProcessingStepUpdate
                ProcessingStep validatedStep = ProcessingStep.get(validatingJob.validatorFor.id)
                boolean succeeded = validatingJob.hasValidatedJobSucceeded()

                ProcessingStepUpdate validatedUpdate = new ProcessingStepUpdate(
                    date: new Date(),
                    state: succeeded ? ExecutionState.SUCCESS : ExecutionState.FAILURE,
                    previous: validatedStep.latestProcessingStepUpdate,
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
                    log.fatal("Could not create a FAILED/SUCCEEDED Update for validated job processed by ${job.class}")
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
                    log.debug("doEndCheck performed for ${job.class} with ProcessingStep ${step.id}")
                    return
                }
            }
        }
        try {
            createNextProcessingStep(step)
        } catch(Exception se) {
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
    public void markProcessAsFailed(ProcessingStep step, String error) {
        Process process = Process.get(step.process.id)
        process.finished = true
        if (!process.save(flush: true)) {
            // TODO: trigger error handling
            log.fatal("Could not set Process to finished")
            throw new ProcessingException("Could not set Process to finished")
        }
        // send notification
        NotificationEvent event = new NotificationEvent(this, [process: process, error: error], NotificationType.PROCESS_FAILED)
        grailsApplication.mainContext.publishEvent(event)
        NotificationEvent event2 = new NotificationEvent(this, [processingStep: step, error: error], NotificationType.PROCESS_STEP_FAILED)
        grailsApplication.mainContext.publishEvent(event2)
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
    public void createError(ProcessingStep step, ProcessingStepUpdate previous, String errorMessage, Class jobClass) {
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

    /**
     * Called by the Scheduler aspect when a Job finished or failed.
     * Removes the Job from the list of running jobs.
     * @param job The Job which finished
     */
    public void removeRunningJob(Job job) {
        // unregister the logging
        Logger logger = Logger.getLogger(job.class.getName())
        if (logger) {
            // the JobAppender might not be added to the Logger directly but to a parent Logger
            // therefore move up the hierarchy till we are at the root Logger (getParent() returns null)
            // and check for each whether our jobs appender is added
            JobAppender jobAppender = null
            while (!jobAppender && logger) {
                jobAppender = (JobAppender)logger.getAppender("jobs")
                logger = (Logger)logger.getParent()
            }
            if (jobAppender) {
                jobAppender.unregisterProcessingStep(job.processingStep)
            }
        }
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
     **/
    public void restartProcessingStep(ProcessingStep step, boolean schedule = true) {
        RestartedProcessingStep restartedStep = null
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
            // create restart event
            ProcessingStepUpdate restart = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.RESTARTED,
                previous: lastUpdate,
                processingStep: step)
            if (!restart.save(flush: true)) {
                log.fatal("Could not create a RESTARTED Update for ProcessingStep ${step.id}")
                throw new ProcessingException("Could not create a RESTARTED Update for ProcessingStep ${step.id}")
            }
            restartedStep = RestartedProcessingStep.create(step)
            if (!restartedStep.save(flush: true)) {
                log.fatal("Could not create a RestartedProcessingStep for ProcessingStep ${step.id}")
                throw new SchedulerPersistencyException("Could not create a RestartedProcessingStep for ProcessingStep ${step.id}")
            }
            // Limitation: in case the restartedStep is the first step of the Process and the Process had been started with Input Parameters
            // those will not be available as it is difficult to map them back
            // this is considered as a theoretical problem as input parameters to the first ProcessingStep are from a time when the ProcessParameter
            // did not yet exist and all existing Workflows do not use this feature
            mapInputParamatersToStep(restartedStep, step.previous ? step.previous.output  : [])
            // update the previous link
            if (restartedStep.previous) {
                restartedStep.previous.next = restartedStep
                if (!restartedStep.previous.save(flush: true)) {
                    log.fatal("Could not update previous ProcessingStep of ProcessingStep ${step.id}")
                    throw new SchedulerPersistencyException("Could not update previous ProcessingStep of ProcessingStep ${step.id}")
                }
            }
            ProcessingStepUpdate created = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: restartedStep)
            if (!created.save(flush: true)) {
                throw new SchedulerPersistencyException("Could not save the first ProcessingStepUpdate for RestartedProcessingStep ${restartedStep.id}")
            }

            Process process = Process.get(step.process.id)
            process.finished = false
            if (!process.save(flush: true)) {
                log.fatal("Could not set Process ${process.id} to not finished")
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
     **/
    public List<ProcessingStep> retrieveRunningProcessingSteps() {
        List<ProcessingStep> runningSteps = []
        List<Process> process = Process.findAllByFinished(false)
        List<ProcessingStep> lastProcessingSteps = ProcessingStep.findAllByProcessInListAndNextIsNull(process)
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
     * Creates a Job for one ProcessingStep.
     * @param step
     * @return
     */
    private Job createJob(ProcessingStep step) {
        Job job = grailsApplication.mainContext.getBean(step.jobDefinition.bean, step, step.input) as Job
        step.jobClass = job.class.getName()
        step.jobVersion = job.getVersion()
        step.save(flush: true)
        job.log = new JobLog(step, job.__internalLog)
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
                throw new IncorrectProcessingException("Process finished but is not in success state")
            }
            last.process.finished = true
            last.process.save(flush: true)
            JobExecutionPlan.withNewSession {
                JobExecutionPlan plan = JobExecutionPlan.get(last.process.jobExecutionPlan.id)
                if (!plan.finishedSuccessful) {
                    plan.finishedSuccessful = 1
                } else {
                    plan.finishedSuccessful++
                }
                plan.save(flush: true)
            }
        } finally {
            lock.unlock()
        }

        // send notification
        NotificationEvent event = new NotificationEvent(this, last.process, NotificationType.PROCESS_SUCCEEDED)
        grailsApplication.mainContext.publishEvent(event)
    }

    // TODO: comment me
    private ProcessingStep createProcessingStep(Process process, JobDefinition jobDefinition, Collection<Parameter> input, ProcessingStep previous = null) {
        ProcessingStep step = null
        if (jobDefinition instanceof DecidingJobDefinition) {
            step = new DecisionProcessingStep(jobDefinition: JobDefinition.get(jobDefinition.id), process: Process.get(process.id), previous: previous ? ProcessingStep.get(previous.id) : null)
        } else {
            step = new ProcessingStep(jobDefinition: JobDefinition.get(jobDefinition.id), process: Process.get(process.id), previous: previous ? ProcessingStep.get(previous.id) : null)
        }
        if (input && !step.save()) {
            // we have to save the next processing step as the ParameterMapping references the JobDefinition
            throw new SchedulerPersistencyException("Could not create new ProcessingStep for Process ${process.id}")
        }
        Parameter failedConstantParameter = mapInputParamatersToStep(step, input)
        step = step.save(flush: true)
        if (!step) {
            throw new SchedulerPersistencyException("Could not save the ProcessingStep for Process ${process.id}")
        }
        ProcessingStepUpdate created = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step)
        if (!created.save(flush: true)) {
            throw new SchedulerPersistencyException("Could not save the first ProcessingStep for Process ${process.id}")
        }
        if (failedConstantParameter) {
            if (!created.save()) {
                throw new SchedulerPersistencyException("Could not save ProcessingStepUpdate for Process ${process.id}")
            }
            ProcessingStepUpdate failure = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: new Date(), previous: created, processingStep: step)
            ProcessingError error = new ProcessingError(errorMessage: "Failed to add constant input parameter ${failedConstantParameter.id} of type ${failedConstantParameter.type.name} to new processing step",
                 processingStepUpdate: failure)
            failure.error = error
            if (!failure.save()) {
                throw new SchedulerPersistencyException("Could not save the ProcessingStep for Process ${process.id}")
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
     * Getter for Unit Tests
     * @return
     */
    protected Queue<ProcessingStep> getQueue() {
        return this.queue
    }

    /**
     * Getter for Unit Tests
     * @return
     */
    protected List<Job> getRunning() {
        return this.running
    }
}
