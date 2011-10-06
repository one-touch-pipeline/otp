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
}
