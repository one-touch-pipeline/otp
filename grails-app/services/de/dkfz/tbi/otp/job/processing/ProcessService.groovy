package de.dkfz.tbi.otp.job.processing

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.context.SecurityContextHolder

import de.dkfz.tbi.otp.job.plan.PlanInformation

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
     * Dependency Injection of JobExecutionPlanService.
     **/
    def jobExecutionPlanService

    /**
     * Security aware way to access a Process.
     * @param id The Process's id
     * @return
     */
    @PostAuthorize("returnObject == null or hasPermission(returnObject.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
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
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public List<ProcessingStep> getAllProcessingSteps(Process process, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        return ProcessingStep.findAllByProcess(process, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    /**
     * Returns the number of ProcessesingSteps for the given Process.
     * @param plan The Process for which the number of ProcessingSteps should be returned
     * @return The number of ProcessingSteps for the given Process
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public int getNumberOfProcessessingSteps(Process process) {
        return ProcessingStep.countByProcess(process)
    }

    /**
     * Security aware way to access a ProcessingStep.
     * @param id The ProcessingStep's id
     * @return
     */
    @PostAuthorize("returnObject == null or hasPermission(returnObject.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public ProcessingStep getProcessingStep(long id) {
        return ProcessingStep.get(id)
    }

    /**
     * Checks whether there is a Log file for the given ProcessingStep.
     * @param step The ProcessingStep for which it should be looked for a log file
     * @return True if there is a log file, false otherwise
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public boolean processingStepLogExists(ProcessingStep step) {
        File file = new File("logs${File.separatorChar}${step.process.id}${File.separatorChar}${step.id}.log")
        return file.exists()
    }

    /**
     * Retrieves the Log file for the given ProcessingStep and returns the file content.
     * @param step The ProcessingStep for which the log file should be retrieved
     * @return Content of log file or empty String in case log file does not exist
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_ADMIN')")
    public String processingStepLog(ProcessingStep step) {
        File file = new File("logs${File.separatorChar}${step.process.id}${File.separatorChar}${step.id}.log")
        if (!file.exists() || !file.isFile()) {
            return ""
        }
        return file.getText()
    }

    /**
     * Restarts the given ProcessingStep.
     * Thin ACL aware wrapper around schedulerService.
     * @param step The ProcessingStep to restart
     **/
    // TODO: better ACL rights?
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', write) or hasRole('ROLE_OPERATOR')")
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
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public List<ProcessingStepUpdate> getAllUpdates(ProcessingStep step, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        return ProcessingStepUpdate.findAllByProcessingStep(step, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    /**
     * Returns the number of ProcessingStepUpdates for the given ProcessingStep.
     * @param step The ProcessingStep for which the number of Updates should be returned
     * @return The number of ProcessingStepUpdates for the given Step
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public int getNumberOfUpdates(ProcessingStep step) {
        return ProcessingStepUpdate.countByProcessingStep(step)
    }

    /**
     * Retrieves the date when the given Process finished.
     * In case the Process has not yet finished a runtime exception is thrown.
     * @param process The Process for which the end date has to be retrieved
     * @return The date when the Process finished
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
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
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
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
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public ProcessingStep getLatestProcessingStep(Process process) {
        return ProcessingStep.findByProcessAndNextIsNull(process)
    }

    /**
     * Returns the latest execution state of the Process.
     * @param process The Process for which the ExecutionState should be retrieved
     * @return Latest execution state of the Process
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public ExecutionState getState(Process process) {
        return lastUpdate(process).state
    }

    /**
     * Overloaded method for convenience.
     * @param step
     * @return
     * @see getState(Process)
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public ExecutionState getState(ProcessingStep step) {
        return step.latestProcessingStepUpdate.state
    }

    /**
     * Returns the error message in case the last ProcessingStep failed.
     * If the last ProcessingStep did not fail, {@code null} is returned.
     * @param process The Process for which the possible error message should be returned.
     * @return
     */
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
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
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public String getError(ProcessingStep step) {
        ProcessingError error = step.latestProcessingStepUpdate.error
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
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public Date getLastUpdate(Process process) {
        return lastUpdate(process).date
    }

    /**
     * Overloaded method for convenience
     * @param step
     * @return
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public Date getLastUpdate(ProcessingStep step) {
        return step.latestProcessingStepUpdate.date
    }

    /**
     * Provides access to the latest ProcessingStepUpdate for the given ProcessingStep.
     * @param step The ProcessingStep for which the latest ProcessingStepUpdate should be retrieved.
     * @return Latest ProcessingStepUpdate
     **/
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public ProcessingStepUpdate getLatestProcessingStepUpdate(ProcessingStep step) {
        return step.latestProcessingStepUpdate
    }

    /**
     * Retrieves the first update date for the ProcessingStep
     * @param step
     * @return
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public Date getFirstUpdate(ProcessingStep step) {
        return ProcessingStepUpdate.findByProcessingStep(step, [sort: "id", order: "asc"]).date
    }

    /**
     * Calculates the duration between the start and finish event. This means it ignores the
     * created event as well as finished and success.
     * @param step The processing step for which the duration has to be calculated
     * @return The duration between the started and finished event for this ProcessingStep
     */
    @PreAuthorize("hasPermission(#step.process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
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
     * Generates some information about the given Process.
     * Information is based on the information for the plan of the process. Each Job is extended
     * by information about the execution of the ProcessingStep. The added information to Job is:
     * <ul>
     * <li>processingStep (long): Id of the ProcessingStep or null if not yet present</li>
     * <li>created (boolean)</li>
     * <li>started (boolean)</li>
     * <li>finished (boolean)</li>
     * <li>succeeded (boolean)</li>
     * <li>failed (boolean)</li>
     * </ul>
     *
     * Additionally for input and output parameters the actual value is added.
     * @param process The Process for which the information should be extracted
     * @return Process Information in a JSON ready format
     * @see JobExecutionPlanService.planInformation
     **/
    @PreAuthorize("hasPermission(#process.jobExecutionPlan.id, 'de.dkfz.tbi.otp.job.plan.JobExecutionPlan', read) or hasRole('ROLE_OPERATOR')")
    public PlanInformation processInformation(Process process) {
        PlanInformation plan = jobExecutionPlanService.planInformation(process.jobExecutionPlan)
        List<ProcessingStep> processingSteps = ProcessingStep.findAllByProcess(process)
        List<Long> jobIdsOfProcessingSteps = processingSteps.collect { it.jobDefinition.id }
        plan.jobs.each { job ->
            if (jobIdsOfProcessingSteps.contains(job.id)) {
                ProcessingStep step = processingSteps.find { it.jobDefinition.id == job.id }
                job.processingStep = step.id
                job.created = true
                ProcessingStepUpdate update = step.latestProcessingStepUpdate
                job.started = update.state != ExecutionState.CREATED
                job.finished = (update.state == ExecutionState.FINISHED || update.state == ExecutionState.SUCCESS || update.state == ExecutionState.FAILURE)
                job.succeeded = update.state == ExecutionState.SUCCESS
                job.failed = (update.state == ExecutionState.FAILURE || update.state == ExecutionState.RESTARTED)
                step.input.each { param ->
                    job.inputParameters.each {
                        if (it.type.id == param.type.id) {
                            it.value = param.value
                        }
                    }
                }
                step.output.each { param ->
                    job.outputParameters.each {
                        if (it.type.id == param.type.id) {
                            it.value = param.value
                        }
                    }
                }
            }
        }
        return plan
    }

    /**
     * Retrieves the stacktrace saved for the ProcessingError.
     * @param id The id of the ProcessingError
     * @return The stacktrace or throws an exception if not found
     **/
    public String getProcessingErrorStackTrace(long id) {
        ProcessingError error = ProcessingError.get(id)
        if(!error) {
            throw new RuntimeException("No Processing Error could be found for the id: " + id)
        }
        if (!error.stackTraceIdentifier) {
            throw new RuntimeException("No stackTrace could be found for the processing error: " + id)
        }
        if (aclUtilService.hasPermission(SecurityContextHolder.context.authentication, error.processingStepUpdate.processingStep.process.jobExecutionPlan, BasePermission.READ) ||
        (SpringSecurityUtils.ifAllGranted("ROLE_OPERATOR"))) {
            return errorLogService.loggedError(error.stackTraceIdentifier)
        }
        throw new RuntimeException("The authentication was not granted for the processing error: " + id)
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
