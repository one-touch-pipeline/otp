package de.dkfz.tbi.otp.job.processing

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Service providing methods to access information about Processes.
 *
 */
class ProcessService {
    static transactional = true
    static final profiled = true

    /**
     * Dependency Injection of schedulerService.
     * Needed to restart Processing Steps.
     **/
    def schedulerService

    /**
     * Dependency Injection of errorLogService.
     * Required to read the stacktrace for an error.
     **/
    def errorLogService

    /**
     * Dependency Injection of aclUtilService.
     **/
    def aclUtilService

    /**
     * Security aware way to access a Process.
     * @param id The Process's id
     * @return
     */
    @PostAuthorize("hasPermission(returnObject.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
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
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public List<ProcessingStep> getAllProcessingSteps(Process process, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        return ProcessingStep.findAllByProcess(process, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    /**
    * Returns the number of ProcessesingSteps for the given Process.
    * @param plan The Process for which the number of ProcessingSteps should be returned
    * @return The number of ProcessingSteps for the given Process
    */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public int getNumberOfProcessessingSteps(Process process) {
        return ProcessingStep.countByProcess(process)
    }

    /**
     * Security aware way to access a ProcessingStep.
     * @param id The ProcessingStep's id
     * @return
     */
    @PostAuthorize("hasPermission(returnObject.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public ProcessingStep getProcessingStep(long id) {
        return ProcessingStep.get(id)
    }

    /**
     * Restarts the given ProcessingStep.
     * Thin ACL aware wrapper around schedulerService.
     * @param step The ProcessingStep to restart
     **/
    // TODO: better ACL rights?
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', write) or hasRole('ROLE_ADMIN')")
    public void restartProcessingStep(ProcessingStep step) {
        schedulerService.restartProcessingStep(step)
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
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public List<ProcessingStepUpdate> getAllUpdates(ProcessingStep step, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        return ProcessingStepUpdate.findAllByProcessingStep(step, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    /**
     * Returns the number of ProcessingStepUpdates for the given ProcessingStep.
     * @param step The ProcessingStep for which the number of Updates should be returned
     * @return The number of ProcessingStepUpdates for the given Step
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
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
            throw new IllegalArgumentException("Process is not finished")
        }
        String query =
'''
SELECT u.date
FROM ProcessingStepUpdate AS u
INNER JOIN u.processingStep AS step
INNER JOIN step.process AS process
WHERE
step.next IS NULL
AND
process.id = :process
ORDER BY u.id desc
'''
        List results = ProcessingStepUpdate.executeQuery(query, [process: process.id], [max: 1])
        if (results.isEmpty()) {
            throw new RuntimeException("No ProcessingStepUpdates for last ProcessingStep")
        }
        return results[0]
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
            throw new IllegalArgumentException("Process is not finished")
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
        return ProcessingStep.findByProcessAndNextIsNull(process)
    }

    /**
     * Returns the latest execution state of the Process.
     * @param process The Process for which the ExecutionState should be retrieved
     * @return Latest execution state of the Process
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public ExecutionState getState(Process process) {
        return lastUpdate(process).state
    }

    /**
     * Overloaded method for convenience.
     * @param step
     * @return
     * @see getState(Process)
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public ExecutionState getState(ProcessingStep step) {
        return lastUpdate(step).state
    }

    /**
     * Returns the error message in case the last ProcessingStep failed.
     * If the last ProcessingStep did not fail, {@code null} is returned.
     * @param process The Process for which the possible error message should be returned.
     * @return
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public String getError(Process process) {
        ProcessingStepUpdate update = lastUpdate(process)
        if (update.error) {
            return update.error.errorMessage
        }
        return null
    }

    /**
     * Overloaded method for convenience.
     * @param step
     * @return
     * @see getError(Process)
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public String getError(ProcessingStep step) {
        ProcessingError error = lastUpdate(step).error
        if (!error) {
            return null
        }
        return error.errorMessage
    }

    /**
     * Retrieves the last update date for the Process.
     * @param process
     * @return
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan, read) or hasRole('ROLE_ADMIN')")
    public Date getLastUpdate(Process process) {
        return lastUpdate.date
    }

    /**
     * Overloaded method for convenience
     * @param step
     * @return
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public Date getLastUpdate(ProcessingStep step) {
        return lastUpdate(step).date
    }

    /**
     * Provides access to the latest ProcessingStepUpdate for the given ProcessingStep.
     * @param step The ProcessingStep for which the latest ProcessingStepUpdate should be retrieved.
     * @return Latest ProcessingStepUpdate
     **/
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public ProcessingStepUpdate getLatestProcessingStepUpdate(ProcessingStep step) {
        return lastUpdate(step)
    }

    /**
     * Retrieves the first update date for the ProcessingStep
     * @param step
     * @return
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public Date getFirstUpdate(ProcessingStep step) {
        return ProcessingStepUpdate.findByProcessingStep(step, [sort: "id", order: "asc"]).date
    }

    /**
     * Calculates the duration between the start and finish event. This means it ignores the
     * created event as well as finished and success.
     * @param step The processing step for which the duration has to be calculated
     * @return The duration between the started and finished event for this ProcessingStep
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
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
     * Retrieves the stacktrace saved for the ProcessingError.
     * @param id The id of the ProcessingError
     * @return The stacktrace or null if not found
     **/
    public String getProcessingErrorStackTrace(long id) {
        ProcessingError error = ProcessingError.get(id)
        if (!error.stackTraceIdentifier) {
            return null
        }
        if (aclUtilService.hasPermission(SecurityContextHolder.context.authentication, error.processingStepUpdate.processingStep.process.jobExecutionPlan, BasePermission.READ) ||
                (SpringSecurityUtils.ifAllGranted("ROLE_ADMIN"))) {
            return errorLogService.loggedError(error.stackTraceIdentifier)
        }
        return null
    }

    /**
     * Helper function to retrieve the last ProcessingStepUpdate for given ProcessingStep.
     * @param step
     * @return
     */
    private ProcessingStepUpdate lastUpdate(ProcessingStep step) {
        return ProcessingStepUpdate.findByProcessingStep(step, [sort: "id", order: "desc"])
    }

    /**
     * Helper function to retrieve the last ProcessingSTepUpdate for given Process.
     * @param process
     * @return
     **/
    private ProcessingStepUpdate lastUpdate(Process process) {
        return ProcessingStepUpdate.withCriteria {
            processingStep {
                eq("process", process)
            }
            maxResults(1)
            order("id", "desc")
        }[0]
    }
}
