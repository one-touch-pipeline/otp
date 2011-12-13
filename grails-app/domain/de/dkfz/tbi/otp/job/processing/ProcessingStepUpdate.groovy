package de.dkfz.tbi.otp.job.processing

public class ProcessingStepUpdate implements Serializable {
    static belongsTo = [processingStep: ProcessingStep]
    /**
     * Link to the ProcessingStepUpdate before this one, may be null.
     **/
    ProcessingStepUpdate previous
    /**
     * The new state of the ProcessingStep set with this update.
     **/
    ExecutionState state
    /**
     * The date when this update was performed.
     **/
    Date date
    /**
     * The Processing Step this update belongs to.
     */
    ProcessingStep processingStep
    /**
     * The error object in case this update is a failure.
     */
    ProcessingError error

    static constraints = {
        processingStep(nullable: false)
        previous(nullable: true, validator: { val, obj ->
            if (obj.id) {
                // this obj had already been validated, so no need to do it again
                return true
            }
            if (val) {
                // the previous update needs to belong to the same processing step us this update
                return val.processingStep.id == obj.processingStep.id
            }
            // we don't set a previous update, this is only allowed if the processingStep has no updates
            // or if it has one element which is this one
            List<ProcessingStepUpdate> stepUpdates = ProcessingStepUpdate.findAllByProcessingStep(obj.processingStep)
            return stepUpdates.isEmpty() ||
                (stepUpdates.size() == 1 && stepUpdates.first() == obj)
        })
        date(nullable: false, validator: { val, obj ->
            if (obj.id) {
                // this obj had already been validated, so no need to do it again
                return true
            }
            List<ProcessingStepUpdate> stepUpdates = ProcessingStepUpdate.findAllByProcessingStep(obj.processingStep)
            if (stepUpdates.isEmpty() ||
                    (stepUpdates.size() == 1 && stepUpdates.first() == obj)) {
                // there are no processing step updates yet, so our value is acceptable
                return true
            }
            boolean ok = true
            // compare all the existing update dates with the current one, they all have to be before this one
            stepUpdates.sort { it.id }.each {
                if (it == obj) {
                    return
                }
                if (it.date > val) {
                    ok = false
                }
            }
            return ok
        })
        state(nullable: false, validator: { val, obj ->
            if (obj.id) {
                // this obj had already been validated, so no need to do it again
                return true
            }
            // only first update may be created
            List<ProcessingStepUpdate> stepUpdates = ProcessingStepUpdate.findAllByProcessingStep(obj.processingStep)
            if (stepUpdates.isEmpty() ||
                    (stepUpdates.size() == 1 && stepUpdates.first() == obj)) {
                return val == ExecutionState.CREATED
            } else {
                return val != ExecutionState.CREATED
            }
        })
        error(nullable: true, validator: {val, obj ->
            if (!val) {
                return true
            }
            return obj.state == ExecutionState.FAILURE
        })
    }
}
