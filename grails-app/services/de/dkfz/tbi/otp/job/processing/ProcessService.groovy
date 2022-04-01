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
package de.dkfz.tbi.otp.job.processing

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.PlanInformation
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Service providing methods to access information about Processes.
 */
@Transactional
class ProcessService {

    /** Needed to restart Processing Steps. */
    SchedulerService schedulerService

    /** Required to read the stacktrace for an error. */
    ErrorLogService errorLogService

    JobExecutionPlanService jobExecutionPlanService

    LinkGenerator grailsLinkGenerator

    ConfigService configService

    /**
     * Security aware way to access a Process.
     * @param id The Process's id
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR')")
    Process getProcess(long id) {
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
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ProcessingStep> getAllProcessingSteps(Process process, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        return ProcessingStep.findAllByProcess(process, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    /**
     * Returns the number of ProcessingSteps for the given Process.
     * @param plan The Process for which the number of ProcessingSteps should be returned
     * @return The number of ProcessingSteps for the given Process
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    int getNumberOfProcessingSteps(Process process) {
        return ProcessingStep.countByProcess(process)
    }

    /**
     * Security aware way to access a ProcessingStep.
     * @param id The ProcessingStep's id
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR')")
    ProcessingStep getProcessingStep(long id) {
        return ProcessingStep.get(id)
    }

    private File getLogForProcessingStep(ProcessingStep step) {
        return new File(new File(configService.jobLogDirectory, "${step.process.id}"), "${step.id}.log")
    }

    /**
     * Checks whether there is a Log file for the given ProcessingStep.
     * @param step The ProcessingStep for which it should be looked for a log file
     * @return True if there is a log file, false otherwise
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean processingStepLogExists(ProcessingStep step) {
        return getLogForProcessingStep(step).exists()
    }

    /**
     * Retrieves the Log file for the given ProcessingStep and returns the file content.
     * @param step The ProcessingStep for which the log file should be retrieved
     * @return Content of log file or empty String in case log file does not exist
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    String processingStepLog(ProcessingStep step) {
        File file = getLogForProcessingStep(step)
        if (!file.exists() || !file.isFile()) {
            return ""
        }
        return file.text
    }

    /**
     * Restarts the given ProcessingStep.
     * Thin ACL aware wrapper around schedulerService.
     * @param step The ProcessingStep to restart
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void restartProcessingStep(ProcessingStep step) {
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
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ProcessingStepUpdate> getAllUpdates(ProcessingStep step, int max = 10, int offset = 0, String column = "id", boolean order = false) {
        return ProcessingStepUpdate.findAllByProcessingStep(step, [max: max, offset: offset, sort: column, order: order ? "asc" : "desc"])
    }

    /**
     * Returns the number of ProcessingStepUpdates for the given ProcessingStep.
     * @param step The ProcessingStep for which the number of Updates should be returned
     * @return The number of ProcessingStepUpdates for the given Step
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    int getNumberOfUpdates(ProcessingStep step) {
        return ProcessingStepUpdate.countByProcessingStep(step)
    }

    /**
     * Retrieves the latest ProcessingStep of the given Process.
     * If there is no ProcessingStep for the Process {@code null} is returned.
     * @param process The Process for which the latest Processing step has to be retrieved.
     * @return The latest ProcessingStep of the Process
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProcessingStep getLatestProcessingStep(Process process) {
        return CollectionUtils.atMostOneElement(ProcessingStep.findAllByProcessAndNextIsNull(process, [sort: "id", order: "desc", max: 1]))
    }

    /**
     * Returns the latest execution state of the Process.
     * @param process The Process for which the ExecutionState should be retrieved
     * @return Latest execution state of the Process
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ExecutionState getState(Process process) {
        return lastUpdate(process).state
    }

    /**
     * Overloaded method for convenience.
     * @see #getState(Process)
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ExecutionState getState(ProcessingStep step) {
        return step.latestProcessingStepUpdate.state
    }

    /**
     * Returns the error message in case the last ProcessingStep failed.
     * If the last ProcessingStep did not fail, {@code null} is returned.
     * @param process The Process for which the possible error message should be returned.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    String getError(Process process) {
        ProcessingStepUpdate update = lastUpdate(process)
        if (update.error) {
            return update.error.errorMessage
        }
        return null
    }

    /**
     * Overloaded method for convenience.
     * @see #getError(Process)
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    String getError(ProcessingStep step) {
        ProcessingError error = step.latestProcessingStepUpdate.error
        if (!error) {
            return null
        }
        return error.errorMessage
    }

    /**
     * Retrieves the last update date for the Process.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Date getLastUpdate(Process process) {
        return lastUpdate(process).date
    }

    /**
     * Overloaded method for convenience
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Date getLastUpdate(ProcessingStep step) {
        return step.latestProcessingStepUpdate.date
    }

    /**
     * Provides access to the latest ProcessingStepUpdate for the given ProcessingStep.
     * @param step The ProcessingStep for which the latest ProcessingStepUpdate should be retrieved.
     * @return Latest ProcessingStepUpdate
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProcessingStepUpdate getLatestProcessingStepUpdate(ProcessingStep step) {
        return step.latestProcessingStepUpdate
    }

    /**
     * Retrieves the first update date for the ProcessingStep
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Date getFirstUpdate(ProcessingStep step) {
        return CollectionUtils.atMostOneElement(ProcessingStepUpdate.findAllByProcessingStep(step, [sort: "id", order: "asc"])).date
    }

    /**
     * Calculates the duration the ProcessingStep was running for.
     *
     * If the ProcessingStep is not yet finished, it calculates the time relative to the @date
     * parameter. A ProcessingStep is considered finished if the last update is either FINISHED,
     * SUCCESS or RESTARTED.
     *
     * If there are no updates yet, return 0.
     *
     * @param step The processing step for which the duration has to be calculated
     * @return The duration between the started and finished event for this ProcessingStep
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Long getProcessingStepDuration(ProcessingStep step, Date date = new Date()) {
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step)?.sort { it.date }
        if (updates.isEmpty()) {
            return 0
        }

        List<ExecutionState> endStates = [
                ExecutionState.FAILURE,
                ExecutionState.FINISHED,
                ExecutionState.SUCCESS,
        ]

        List<ProcessingStepUpdate> endStateUpdates = updates.findAll { ProcessingStepUpdate update -> update.state in endStates }
        return (endStateUpdates ? endStateUpdates.last().date.time : date.time) - updates.first().date.time
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
     * @see JobExecutionPlanService#planInformation(JobExecutionPlan)
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    PlanInformation processInformation(Process process) {
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

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void setOperatorIsAwareOfFailureWithAuthentication(Process process, boolean operatorIsAwareOfFailure) {
        setOperatorIsAwareOfFailure(process, operatorIsAwareOfFailure)
    }

    void setOperatorIsAwareOfFailure(Process process, boolean operatorIsAwareOfFailure) {
        process.operatorIsAwareOfFailure = operatorIsAwareOfFailure
        process.save(flush: true)
    }

    /**
     * Retrieves the stacktrace saved for the ProcessingError.
     * @param id The id of the ProcessingError
     * @return The stacktrace or throws an exception if not found
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    String getProcessingErrorStackTrace(long id) {
        ProcessingError error = ProcessingError.get(id)
        if (!error) {
            throw new RuntimeException("No Processing Error could be found for the id: " + id)
        }
        if (!error.stackTraceIdentifier) {
            throw new RuntimeException("No stackTrace could be found for the processing error: " + id)
        }
        return errorLogService.loggedError(error.stackTraceIdentifier)
    }

    /**
     * Helper function to retrieve the last ProcessingSTepUpdate for given Process.
     */
    private ProcessingStepUpdate lastUpdate(Process process) {
        return ProcessingStepUpdate.withCriteria {
            processingStep {
                eq("process", process)
            }
            maxResults(1)
            order("id", "desc")
        } [0]
    }

    String processUrl(Process process) {
        return grailsLinkGenerator.link(controller: 'processes', action: 'process', id: process.id, absolute: true)
    }
}
