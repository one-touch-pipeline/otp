package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingError
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.scheduler.SchedulerService

import org.springframework.security.access.prepost.PreAuthorize

class CrashRecoveryService {

    SchedulerService schedulerService

    /**
     * @return Whether there is currently a crash recovery in process
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    boolean isCrashRecovery() {
        return !schedulerService.isStartupOk()
    }

    /**
     * Marks the ProcessingSteps identified by ids as finished.
     * After the ProcessingSteps have been set to finished the scheduler is invoked to plan the
     * next ProcessingStep.
     * @param ids The Ids of the ProcessingSteps
     * @param parameters Key, value pairs of the manually set parameters
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void markJobsAsFinished(List<Long> ids, Map parameters) {
        assert null != ids
        assert null != parameters
        List<ProcessingStep> steps = getProcessingSteps(ids)
        storeParameters(steps, parameters)
        steps.each { ProcessingStep step->
            // finished update
            createNewProcessingStepUpdate(step, ExecutionState.FINISHED)
            // and schedule
            schedulerService.createNextProcessingStep(step)
        }
    }

    /**
     * Marks the ProcessingSteps identified by ids as finished and succeeded.
     * After the ProcessingSteps have been set to succeeded the scheduler is invoked to plan the
     * next ProcessingStep.
     * @param ids The Ids of the ProcessingSteps
     * @param parameters Key, value pairs of the manually set parameters
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void markJobsAsSucceeded(List<Long> ids, Map parameters) {
        assert null != ids
        assert null != parameters
        List<ProcessingStep> steps = getProcessingSteps(ids)
        storeParameters(steps, parameters)
        steps.each { ProcessingStep step->
            // finished and success update
            createNewProcessingStepUpdate(step, ExecutionState.FINISHED)
            createNewProcessingStepUpdate(step, ExecutionState.SUCCESS)
            // and schedule
            schedulerService.createNextProcessingStep(step)
        }
    }

    /**
     * Marks the ProcessingSteps identified by ids as failed and ends the step's processes.
     * @param ids The ids of the ProcessingSteps which failed
     * @param reason A reason why the ProcessingSteps failed
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void markJobsAsFailed(List<Long> ids, String reason) {
        assert null != ids
        assert null != reason
        List<ProcessingStep> steps = getProcessingSteps(ids)
        steps.each { ProcessingStep processingStep->
            performMarkProcessingStepAsFailed(processingStep, reason)
        }
    }

    /**
     * Restarts the ProcessingSteps identified by ids.
     * This puts the ProcessingSteps into a failed state first and reschedules the Jobs afterwards.
     * @param ids The ids of the ProcessingSteps to restart
     * @param reason Why the Jobs need to be restarted.
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void restartJobs(List<Long> ids, String reason) {
        assert null != ids
        assert null != reason
        List<ProcessingStep> steps = getProcessingSteps(ids)
        steps.each { ProcessingStep processingStep->
            performMarkProcessingStepAsFailed(processingStep, reason)
            schedulerService.restartProcessingStep(processingStep, false, true)
        }
    }

    /**
     * List of all ProcessingSteps which crashed during the last Shutdown.
     * This includes for all running Processes the ProcessingSteps which are in a running state.
     * @return List of all crashed ProcessingSteps
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<ProcessingStep> crashedJobs() {
        if (!isCrashRecovery()) {
            // TODO throw proper exception
            throw new RuntimeException("Not in Crash Recovery")
        }
        return schedulerService.retrieveRunningProcessingSteps()
    }

    /**
     * Retrieves all Output Parameter Types for the given ProcessingSteps.
     * @param ids The ids of the ProcessingSteps for which the output parameters should be retrieved
     * @return List of Maps containing for each processing step the tripple processing step id, job name
     *         and the list of output parameters used by the corresponding processing step
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Map<String, Object>> getOutputParametersOfJobs(List<Long> ids) {
        assert null != ids
        List<ProcessingStep> steps = getProcessingSteps(ids)
        return steps.collect {ProcessingStep step ->
            return [
                id: step.id,
                jobName: step.jobDefinition.name,
                parameter: ParameterType.findAllByJobDefinitionAndParameterUsage(step.jobDefinition, ParameterUsage.OUTPUT),
                ]

        }
    }

    /**
     * Helper function to retrieve the ProcessingSteps for the given Ids.
     * The method does not only retrieve the ProcessingSteps from Database, but also ensures
     * that the ProcessingSteps are in a state requiring crash recovery. If at least one is not in such a case
     * a RuntimeException is thrown.
     * @param ids The Ids for which the ProcessingSteps need to be retrieved
     * @return The found ProcessingSteps.
     **/
    private List<ProcessingStep> getProcessingSteps(List<Long> ids) {
        if (!isCrashRecovery()) {
            throw new RuntimeException("The system is not in Crash Recovery")
        }
        return ids.collect {Long id ->
            ProcessingStep step = ProcessingStep.getInstance(id)
            if (step.next) {
                throw new RuntimeException("ProcessingStep ${id} has already been restarted")
            }
            return step
        }
    }

    /**
     * Helper Function to create a new ProcessingStepUpdate.
     * @param step The ProcessingStep for which the Update should be created
     * @param state The ExecutionState for the update
     **/
    private ProcessingStepUpdate createNewProcessingStepUpdate(ProcessingStep step, ExecutionState state) {
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: state,
            previous: step.latestProcessingStepUpdate,
            processingStep: step
            )
        if (!update.validate()) {
            log.fatal("Could not create a ${state} Update for ProcessingStep ${step.id}")
            throw new ProcessingException("Could not create a ${state} Update for ProcessingStep ${step.id}")
        }
        return update.save(flush: true)
    }

    /**
     * Stores the Output parameters for the given processing steps.
     * This method also validates that all output parameters have been provided.
     * @param steps The processing steps for which the output parameters should be stored
     * @param parameters map containing for each processing step the parameter map with key is id of ParameterType and Value the value for the Parameter
     **/
    private void storeParameters(List<ProcessingStep> steps, Map<Long, Map<String, String>> parameters) {
        Parameter.withTransaction {  status ->
            steps.each { ProcessingStep step ->
                parameters[step.id].each { key, value ->
                    ParameterType type = ParameterType.get(key as Long)
                    if (type.jobDefinition.id != step.jobDefinition.id) {
                        status.setRollbackOnly()
                        throw new RuntimeException("ParameterType with id ${key} does not belong to ProcessingStep with id ${step.id}")
                    }
                    if (type.parameterUsage != ParameterUsage.OUTPUT) {
                        status.setRollbackOnly()
                        throw new RuntimeException("ParameterType with id ${key} is not an output Parameter")
                    }
                    Parameter param = new Parameter(type: type, value: value)
                    if (!param.validate()) {
                        status.setRollbackOnly()
                        throw new RuntimeException("Parameter with type id ${key} and value ${value} cannot be stored")
                    }
                    step.addToOutput(param)
                }
                // validate that all output parameters are set
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
                        status.setRollbackOnly()
                        throw new RuntimeException("Parameter for type ${parameterType.id} has not been set")
                    }
                }
                step.save(flush: true)
            }
        }
    }

    private void performMarkProcessingStepAsFailed(ProcessingStep step, String reason) {
        createNewProcessingStepUpdate(step, ExecutionState.FINISHED)
        ProcessingStepUpdate update = createNewProcessingStepUpdate(step, ExecutionState.FAILURE)
        ProcessingError error = new ProcessingError(errorMessage: reason, processingStepUpdate: update)
        error.save()
        update.error = error
        if (!update.save(flush: true)) {
            // TODO: trigger error handling
            log.fatal("Could not create a FAILURE Update for ProcessingStep ${step.id}")
            throw new ProcessingException("Could not create a FAILURE Update for ProcessingStep ${step.id}")
        }
        Process process = Process.get(step.process.id)
        process.finished = true
        if (!process.save(flush: true)) {
            // TODO: trigger error handling
            log.fatal("Could not set Process ${step.process.id} to finished")
            throw new ProcessingException("Could not set Process ${step.process.id} to finished")
        }
    }
}
