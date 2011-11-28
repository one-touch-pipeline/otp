package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PostAuthorize

/**
 * Service providing methods to access information about Processes.
 *
 */
class ProcessService {
    static transactional = true

    /**
     * Security aware way to access a Process.
     * @param id The Process's id
     * @return
     */
    @PostAuthorize("hasPermission(returnObject.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public Process getProcess(long id) {
        return Process.get(id)
    }

    /**
    * Retrieves all ProcessingSteps for the given Process.
    * @param process The Process whose ProcessingSteps should be retrieved
    * @param max The number of elements to retrieve, default 10
    * @param offset The offset in the list, default -
    * @param column The column to search for, default "id"
    * @param order {@code true} for ascending ordering, {@code false} for descending, default {@code false}
    * @return List of all ProcessingSteps run for the Process filtered as requested
    */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public List<ProcessingStep> getAllProcessingSteps(Process process, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        return ProcessingStep.findAllByProcess(process, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    /**
    * Returns the number of ProcessesingSteps for the given Process.
    * @param plan The Process for which the number of ProcessingSteps should be returned
    * @return The number of ProcessingSteps for the given Process
    */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public int getNumberOfProcessessingSteps(Process process) {
        return ProcessingStep.countByProcess(process)
    }

    /**
     * Security aware way to access a ProcessingStep.
     * @param id The ProcessingStep's id
     * @return
     */
    @PostAuthorize("hasPermission(returnObject.process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public ProcessingStep getProcessingStep(long id) {
        return ProcessingStep.get(id)
    }

    /**
     * Retrieves all ProcessingStepUpdates for the given ProcessingStep.
     * @param step The ProcessingStep whose Updates should be retrieved
     * @param max The number of elements to retrieve, default 10
     * @param offset The offset in the list, default -
     * @param column The column to search for, default "id"
     * @param order {@code true} for ascending ordering, {@code false} for descending, default {@code false}
     * @return List of all ProcessingStepUpdates for the Step filtered as requested
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public List<ProcessingStepUpdate> getAllUpdates(ProcessingStep step, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        return ProcessingStepUpdate.findAllByProcessingStep(step, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    /**
     * Returns the number of ProcessingStepUpdates for the given ProcessingStep.
     * @param step The ProcessingStep for which the number of Updates should be returned
     * @return The number of ProcessingStepUpdates for the given Step
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public int getNumberOfUpdates(ProcessingStep step) {
        return ProcessingStepUpdate.countByProcessingStep(step)
    }

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
     * Retrieves the first update date for the ProcessingStep
     * @param step
     * @return
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public Date getFirstUpdate(ProcessingStep step) {
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step)
        if (updates.isEmpty()) {
            throw new IllegalArgumentException("ProcessingStep has no updates")
        }
        return updates.sort { it.id }.first().date
    }

    /**
     * Calculates the duration between the start and finish event. This means it ignores the
     * created event as well as finished and success.
     * @param step The processing step for which the duration has to be calculated
     * @return The duration between the started and finished event for this ProcessingStep
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public Long getProcessingStepDuration(ProcessingStep step) {
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step)
        if (updates.isEmpty()) {
            throw new IllegalArgumentException("ProcessingStep has no updates")
        }
        updates = updates.sort { it.id }
        Date startDate = null
        Date finishDate = null
        // find the finished
        for (ProcessingStepUpdate update in updates) {
            if (update.state == ExecutionState.STARTED) {
                startDate = update.date
                continue
            }
            if (update.state == ExecutionState.FINISHED) {
                finishDate = update.date
                break
            }
            if (update.state == ExecutionState.FAILURE) {
                finishDate = update.date
                // no break as there could be another finish afterwards due to manual restart
            }
        }
        if (finishDate) {
            return finishDate.time - startDate.time
        }
        return null
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
