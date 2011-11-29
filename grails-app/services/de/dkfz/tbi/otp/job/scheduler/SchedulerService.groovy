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
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.StartJob

import java.util.concurrent.Callable
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.springframework.scheduling.annotation.Scheduled

class SchedulerService {
    static transactional = false
    /**
     * Dependency Injection of grailsApplication
     */
    def grailsApplication
    /**
     * Dependency Injection of ExecutorService
     */
    def executorService
    /**
     * Dependency Injection of PersistenceContextInterceptor
     */
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
     * Last but not least the created ProcessingStep gets scheduled.
     * @param startJob The StartJob which wants to trigger the Process
     * @param input List of Parameters provided by the StartJob for this Process.
     */
    public void createProcess(StartJob startJob, List<Parameter> input) {
        JobExecutionPlan plan = JobExecutionPlan.get(startJob.getExecutionPlan().id)
        Process process = new Process(started: new Date(),
            jobExecutionPlan: plan,
            startJobClass: startJob.class.toString(),
            startJobVersion: startJob.getVersion()
        )
        if (!process.save()) {
            throw new SchedulerPersistencyException("Could not save the process for the JobExecutionPlan ${plan.id}")
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
    }

    /**
     * Invokes the primitive scheduler to determine which job is to execute next if any at all.
     */
    @Scheduled(fixedRate=10000l)
    public void schedule() {
        Job job = null
        persistenceInterceptor.init()
        try {
            job = doSchedule()
            if (job) {
                job.execute()
            }
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
            // simple scheduler: run a new Job if there are less running Jobs than executed Jobs
            if (running.size() > queue.size() || queue.isEmpty()) {
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
        step.jobClass = job.class.toString()
        step.jobVersion = job.getVersion()
        step.save(flush: true)
        return job
    }

    /**
     * Method responsible for handling the successful ending of a Process.
     * @param last The last executed ProcessingStep
     */
    private void endProcess(ProcessingStep last) {
        ProcessingStepUpdate update = last.updates.toList().sort { it.id }.last()
        if (update.state != ExecutionState.SUCCESS) {
            throw new IncorrectProcessingException("Process finished but is not in success state")
        }
        last.process.finished = true
        last.process.save()
        if (!last.process.jobExecutionPlan.finishedSuccessful) {
            last.process.jobExecutionPlan.finishedSuccessful = 1
        } else {
            last.process.jobExecutionPlan.finishedSuccessful++
        }
        last.process.jobExecutionPlan.save(flush: true)
        // TODO: start some notifications?
    }

    // TODO: comment me
    private ProcessingStep createProcessingStep(Process process, JobDefinition jobDefinition, Collection<Parameter> input, ProcessingStep previous = null) {
        ProcessingStep step = null
        if (jobDefinition instanceof DecidingJobDefinition) {
            step = new DecisionProcessingStep(jobDefinition: jobDefinition, process: process, previous: previous)
        } else {
            step = new ProcessingStep(jobDefinition: jobDefinition, process: process, previous: previous)
        }
        if (input && !step.save()) {
            // we have to save the next processing step as the ParameterMapping references the JobDefinition
            throw new SchedulerPersistencyException("Could not create new ProcessingStep for Process ${process.id}")
        }
        input.each { Parameter param ->
            List<ParameterMapping> mappings = ParameterMapping.findAllByFromAndJob(param.type, jobDefinition)
            mappings.each { ParameterMapping mapping ->
                Parameter nextParam = new Parameter(type: mapping.to, value: param.value)
                if (mapping.to.usage == ParameterUsage.PASSTHROUGH) {
                    step.addToOutput(nextParam)
                } else {
                    step.addToInput(nextParam)
                }
            }
        }
        // add constant parameters to the next processing step
        Parameter failedConstantParameter = null
        jobDefinition.constantParameters.each { Parameter param ->
            if (param.type.usage != ParameterUsage.INPUT) {
                failedConstantParameter = param
                return // continue
            }
            step.addToInput(Parameter.get(param.id))
        }
        if (!step.save()) {
            throw new SchedulerPersistencyException("Could not save the ProcessingStep for Process ${process.id}")
        }
        ProcessingStepUpdate created = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date())
        step.addToUpdates(created)
        if (!step.save(flush: true)) {
            throw new SchedulerPersistencyException("Could not save the first ProcessingStep for Process ${process.id}")
        }
        if (failedConstantParameter) {
            if (!created.save()) {
                throw new SchedulerPersistencyException("Could not save ProcessingStepUpdate for Process ${process.id}")
            }
            ProcessingStepUpdate failure = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: new Date(), previous: created)
            ProcessingError error = new ProcessingError(errorMessage: "Failed to add constant input parameter ${failedConstantParameter.id} of type ${failedConstantParameter.type.name} to new processing step",
                 processingStepUpdate: failure)
            failure.error = error
            step.addToUpdates(failure)
            if (!step.save()) {
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
