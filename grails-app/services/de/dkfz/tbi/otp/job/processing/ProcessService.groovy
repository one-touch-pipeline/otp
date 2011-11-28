package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import org.springframework.security.access.prepost.PreAuthorize

/**
 * Service providing methods to access information about Processes.
 *
 */
class ProcessService {
    static transactional = true

    /**
     * Retrieves the date when the given Process finished.
     * In case the Process has not yet finished a runtime exception is thrown.
     * @param process The Process for which the end date has to be retrieved
     * @return The date when the Process finished
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public Date getFinishDate(Process process) {
        if (!process.finished) {
            throw new IllegalArgumentException("Process is finished")
        }
        final List<ProcessingStep> allSteps = ProcessingStep.findAllByProcess(process)
        ProcessingStep lastStep = null
        for (ProcessingStep step : allSteps) {
            if (!step.next) {
                lastStep = step
                break
            }
        }
        if (!lastStep) {
            // TODO: throw proper exception
            throw new RuntimeException("Finished Process does not have an end ProcessingStep")
        }
        final List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(lastStep)
        if (updates.isEmpty()) {
            throw new RuntimeException("No ProcessingStepUpdates for last ProcessingStep")
        }
        return updates.sort { it.id }.last().date
    }

    /**
     * Calculates the duration between the starting of the Process and the finish date.
     * In case the Process has not yet finished a runtime exception is thrown.
     * The returned duration is in milliseconds.
     * @param process The Process for which the duration should be retrieved
     * @return The number of msec the Process took
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public long getDuration(Process process) {
        if (!process.finished) {
            throw new IllegalArgumentException("Process is finished")
        }
        Date endDate = getFinishDate(process)
        return endDate.time - process.started.time
    }

    /**
     * Retrieves the latest ProcessingStep of the given Process.
     * If there is no ProcessingStep for the Process {@code null} is returned.
     * @param process The Process for which the latest Processing step has to be retrieved.
     * @return The latest ProcessingStep of the Process
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public ProcessingStep getLatestProcessingStep(Process process) {
        List<ProcessingStep> steps = ProcessingStep.findAllByProcess(process)
        for (ProcessingStep step in steps) {
            if (!step.next) {
                return step
            }
        }
        return null
    }

    /**
     * Returns the latest execution state of the Process.
     * @param process The Process for which the ExecutionState should be retrieved
     * @return Latest execution state of the Process
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public ExecutionState getState(Process process) {
        getState(getLatestProcessingStep(process))
    }

    /**
     * Overloaded method for convenience.
     * @param step
     * @return
     * @see getState(Process)
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public ExecutionState getState(ProcessingStep step) {
        return lastUpdate(step).state
    }

    /**
     * Retrieves the last update date for the Process.
     * @param process
     * @return
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public Date getLastUpdate(Process process) {
        getLastUpdate(getLatestProcessingStep(process))
    }

    /**
     * Overloaded method for convenience
     * @param step
     * @return
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public Date getLastUpdate(ProcessingStep step) {
        return lastUpdate(step).date
    }

    /**
     * Helper function to retrieve the last ProcessingStepUpdate for given ProcessingStep.
     * @param step
     * @return
     */
    private ProcessingStepUpdate lastUpdate(ProcessingStep step) {
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step)
        if (updates.isEmpty()) {
            throw new IllegalArgumentException("ProcessingStep has no updates")
        }
        return updates.sort { it.id }.last()
    }
}
