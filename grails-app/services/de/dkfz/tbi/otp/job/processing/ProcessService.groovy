package de.dkfz.tbi.otp.job.processing

import org.springframework.security.access.prepost.PreAuthorize

/**
 * Service providing methods to access information about Processes.
 *
 */
class ProcessService {

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
}
