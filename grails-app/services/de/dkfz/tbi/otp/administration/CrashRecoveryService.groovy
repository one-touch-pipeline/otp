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
import org.springframework.security.access.prepost.PreAuthorize

class CrashRecoveryService {
    /**
     * Dependency Injection of Scheduler Service.
     * Required to figure out whether there is a crash recovery and to restart the scheduler.
     **/
    def schedulerService

    /**
     * @return Whether there is currently a crash recovery in process
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    boolean isCrashRecovery() {
        return !schedulerService.isStartupOk()
    }

    /**
     * Marks the ProcessingStep identified by id as finished.
     * After the ProcessingStep has ben set to finished the scheduler is invoked to plan the
     * next ProcessingStep.
     * @param id The Id of the ProcessingStep
     * @param parameter Key, value pair of the manually set parameters
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void markJobAsFinished(Long id, Map<String, String> parameters) {
        ProcessingStep step = getProcessingStep(id)
        storeParameters(step, parameters)
        // finished update
        createNewProcessingStepUpdate(step, ExecutionState.FINISHED)
        // and schedule
        schedulerService.createNextProcessingStep(step)
    }

    /**
     * Marks the ProcessingStep identified by id as finished and succeeded.
     * After the ProcessingStep has ben set to succeeded the scheduler is invoked to plan the
     * next ProcessingStep.
     * @param id The Id of the ProcessingStep
     * @param parameter Key, value pair of the manually set parameters
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void markJobAsSucceeded(Long id, Map<String, String> parameters) {
        ProcessingStep step = getProcessingStep(id)
        storeParameters(step, parameters)
        // finished and success update
        createNewProcessingStepUpdate(step, ExecutionState.FINISHED)
        createNewProcessingStepUpdate(step, ExecutionState.SUCCESS)
        // and schedule
        schedulerService.createNextProcessingStep(step)
    }

    /**
     * Marks the ProcessingStep identified by id as failed and ends the step's process.
     * @param id The id of the ProcessingStep which failed
     * @param reason A reason why the ProcessingStep failed
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void markJobAsFailed(Long id, String reason) {
        ProcessingStep step = getProcessingStep(id)
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

    /**
     * Restarts the ProcessingStep identified by id.
     * This puts the ProcessingStep into a failed state first and reschedules the Job afterwards.
     * @param id The id of the ProcessingStep to restart
     * @param reason Why the Job needs to be restarted.
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void restartJob(Long id, String reason) {
        ProcessingStep step = getProcessingStep(id)
        // TODO: how to restart?
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
        List<ProcessingStep> crashed = []
        List<Process> process = Process.findAllByFinished(false)
        List<ProcessingStep> lastProcessingSteps = ProcessingStep.findAllByProcessInListAndNextIsNull(process)
        lastProcessingSteps.each { ProcessingStep step ->
            List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step)
            if (updates.isEmpty()) {
                return
            }
            ProcessingStepUpdate last = updates.sort { it.id }.last()
            if (last.state == ExecutionState.STARTED || last.state == ExecutionState.RESTARTED || last.state == ExecutionState.RESUMED) {
                crashed << step
            }
        }
        return crashed
    }

    /**
     * Retrieves all Output Parameter Types for the given ProcessingStep.
     * @param id The id of the ProcessingStep for which the output parameters should be retrieved
     * @return List of output parameters used by the given processing step
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<ParameterType> getOutputParametersOfJob(Long id) {
        ProcessingStep step = getProcessingStep(id)
        return ParameterType.findAllByJobDefinitionAndParameterUsage(step.jobDefinition, ParameterUsage.OUTPUT)
    }

    /**
     * Helper function to retrieve the ProcessingStep for the given Id.
     * The method does not only retrieve the ProcessingStep from Database, but also ensures
     * that the ProcessingStep is in a state requiring crash recovery. If not in such a case
     * a RuntimeException is thrown.
     * @param id The Id for which the ProcessingStep needs to be retrieved
     * @return The found ProcessingStep.
     **/
    private ProcessingStep getProcessingStep(Long id) {
        if (!isCrashRecovery()) {
            // TODO throw proper exception
            throw new RuntimeException("Not in Crash Recovery")
        }
        ProcessingStep step = ProcessingStep.get(id)
        if (!step) {
            // TODO: throw proper exception
            throw new RuntimeException("ProcessingStep not found")
        }
        if (step.next) {
            // TODO: throw proper exception
            throw new RuntimeException("ProcessingStep has already been restarted")
        }
        // TODO: check last update
        return step
    }

    /**
     * Helper function to retrieve the last ProcessingStepUpdate for the given ProcessingStep.
     * @param step The ProcessingStep whose last ProcessingStepUpdate needs to be retrieved
     * @return The last ProcessingStepUpdate
     **/
    private ProcessingStepUpdate getLastUpdate(ProcessingStep step) {
        List<ProcessingStepUpdate> existingUpdates = ProcessingStepUpdate.findAllByProcessingStep(step)
        return existingUpdates.sort { it.date }.last()
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
            previous: getLastUpdate(step),
            processingStep: step
            )
        if (!update.save(flush: true)) {
            log.fatal("Could not create a ${state} Update for ProcessingStep ${step.id}")
            throw new ProcessingException("Could not create a ${state} Update for ProcessingStep ${step.id}")
        }
        return update
    }

    /**
     * Stores the Output parameters for the given processing step.
     * This method also validates that all output parameters have been provided.
     * @param step The processing step for which the output parameters should be stored
     * @param parameters Key/Value pair of Parameters, key is id of ParameterType, Value the value for the Parameter
     **/
    private void storeParameters(ProcessingStep step, Map<String, String> parameters) {
        Parameter.withTransaction {  status ->
            parameters.each { key, value ->
                println key + "/" + value
                ParameterType type = ParameterType.get(key as Long)
                if (type.jobDefinition.id != step.jobDefinition.id) {
                    status.setRollbackOnly()
                    // TODO: throw proper exception
                    throw new RuntimeException("ParameterType with id ${key} does not belong to ProcessingStep with id ${step.id}")
                }
                if (type.parameterUsage != ParameterUsage.OUTPUT) {
                    status.setRollbackOnly()
                    // TODO: throw proper exception
                    throw new RuntimeException("ParameterType with id ${key} is not an output Parameter")
                }
                Parameter param = new Parameter(type: type, value: value)
                if (!param.validate()) {
                    status.setRollbackOnly()
                    // TODO: throw proper exception
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
                    // TODO: throw proper exception
                    throw new RuntimeException("Parameter for type ${parameterType.id} has not been set")
                }
            }
            step.save(flush: true)
        }
    }
}
