package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.job.plan.DecidingJobDefinition
import de.dkfz.tbi.otp.job.plan.DecisionMapping
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.DecisionProcessingStep
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.IncorrectProcessingException
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.StartJob
import de.dkfz.tbi.otp.notification.NotificationEvent
import de.dkfz.tbi.otp.notification.NotificationType;

import java.util.concurrent.Callable
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
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
    private boolean schedulerActive = true

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void suspendScheduler() {
        schedulerActive = false
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void resumeScheduler() {
        schedulerActive = true
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
            startJobClass: startJob.class.name,
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
            Job job = createJob(queue.peek())
            running.add(job)
            queue.poll()
            return job
        } finally {
            lock.unlock()
        }
        return null
    }

    /**
     * Called by the Scheduler aspect when a Job finished or failed.
     * Removes the Job from the list of running jobs.
     * @param job The Job which finished
     */
    public void removeRunningJob(Job job) {
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
     * Creates a Job for one ProcessingStep.
     * @param step
     * @return
     */
    private Job createJob(ProcessingStep step) {
        Job job = grailsApplication.mainContext.getBean(step.jobDefinition.bean, step, step.input) as Job
        step.jobClass = job.class.name
        step.jobVersion = job.getVersion()
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
