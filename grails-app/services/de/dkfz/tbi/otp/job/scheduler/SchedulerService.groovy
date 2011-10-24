package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate

import java.util.concurrent.Callable
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class SchedulerService {
    /**
     * Dependency Injection of grailsApplication
     */
    def grailsApplication
    /**
     * Dependency Injection of ExecutorService
     */
    def executorService
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
        if (!previous.jobDefinition.next) {
            endProcess(previous.process)
            return
        }
        JobDefinition nextJob = previous.jobDefinition.next
        ProcessingStep next = new ProcessingStep(jobDefinition: nextJob, process: previous.process, previous: previous)
        previous.output.each { Parameter param ->
            if (nextJob.parameterMapping?.containsKey(param.type)) {
                Parameter nextParam = new Parameter(type: nextJob.parameterMapping.get(param.type), value: param.value)
                next.addToInput(nextParam)
            }
        }
        // add constant parameters to the next processing step
        Parameter failedConstantParameter = null
        nextJob.constantParameters.each { Parameter param ->
            if (param.type.usage != ParameterUsage.INPUT) {
                failedConstantParameter = param
                return // continue
            }
            next.addToInput(param)
        }
        ProcessingStepUpdate created = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date())
        next.addToUpdates(created)
        lock.lock()
        try {
            if (!next.save()) {
                // TODO: proper error handling
                throw new RuntimeException("Something bad happened")
            }
            if (failedConstantParameter) {
                ProcessingStepUpdate failure = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: new Date(), previous: created)
                ProcessingError error = new ProcessingError(errorMessage: "Failed to add constant input parameter ${failedConstantParameter.id} of type ${failedConstantParameter.type.name} to new processing step",
                     processingStepUpdate: failure)
                failure.error = error
                next.addToUpdates(failure)
                if (!next.save()) {
                    // TODO: proper error handling
                    throw new RuntimeException("Something bad happened")
                }
            }
            previous.next = next
            if (!previous.save()) {
                // TODO: proper error handling
                throw new RuntimeException("Something bad happened")
            }
            if (!next.process.save(flush: true)) {
                // TODO: proper error handling
                throw new RuntimeException("Something bad happened")
            }
            queue.add(next)
        } catch (Exception e) {
            // TODO: proper error handling
            println e.message
        } finally {
            lock.unlock()
        }
    }

    public void createProcess(JobExecutionPlan plan, List<Parameter> input) {
    }

    /**
     * Invokes the primitive scheduler to determine which job is to execute next if any at all.
     */
    public void schedule() {
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

            executorService.submit({
                job.execute()
            } as Callable)
        } finally {
            lock.unlock()
        }
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
     * @param process The Process which finished
     */
    private void endProcess(Process process) {
        // TODO: add safety check that last ProcessingStep did not fail
        process.finished = true
        process.save(flush: true)
        // TODO: start some notifications?
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
