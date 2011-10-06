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

    static constraints = {
        processingStep(nullable: false)
        previous(nullable: true, validator: { val, obj ->
            if (val) {
                // the previous update needs to belong to the same processing step us this update
                return val.processingStep == obj.processingStep
            }
            // we don't set a previous update, this is only allowed if the processingStep has no updates
            // or if it has one element which is this one
            return !obj.processingStep.updates ||
                obj.processingStep.updates.isEmpty() ||
                (obj.processingStep.updates.size() == 1 && obj.processingStep.updates.asList().first() == obj)
        })
        date(nullable: false, validator: { val, obj ->
            if (!obj.processingStep.updates) {
                // there are no processing step updates yet, so our value is acceptable
                return true
            }
            boolean ok = true
            // compare all the existing update dates with the current one, they all have to be before this one
            obj.processingStep.updates.each {
                if (it.date > val) {
                    ok = false
                }
            }
            return ok
        })
        state(nullable: false, validator: { val, obj ->
            // only first update may be created
            if (!obj.processingStep.updates ||
                    (obj.processingStep.updates.size() == 1 && obj.processingStep.updates.asList().first() == obj)) {
                return val == ExecutionState.CREATED
            } else {
                return val != ExecutionState.CREATED
            }
        })
    }
}
