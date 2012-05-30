package de.dkfz.tbi.otp.job.processing

/**
 * Abstract base class for {@link ValidatingJob}s.
 * @see ValidatingJob
 */
abstract public class AbstractValidatingJobImpl extends AbstractEndStateAwareJobImpl implements ValidatingJob {
    private ProcessingStep validatedStep
    private Boolean validatedStepSucceeded = null

    /**
     * Default empty constructor
     */
    public AbstractValidatingJobImpl() {
    }

    /**
     * Constructor used by the factory method. Each implementing sub-class gets a matching Constructor injected.
     * @param processingStep The processing step for this Job
     * @param inputParameters The input parameters for this Job
     */
    protected AbstractValidatingJobImpl(ProcessingStep processingStep, Collection<Parameter> inputParameters) {
        super(processingStep, inputParameters)
    }

    /**
     * Implementing sub-classes can use this method to mark the validated job as succeeded or failed.
     * @param succeeded true for success, false for failure
     **/
    protected void setValidatedSucceeded(boolean succeeded) {
        validatedStepSucceeded = succeeded
    }

    @Override
    public ProcessingStep getValidatorFor() {
        if (!validatedStep) {
            throw new RuntimeException("Validated Step accessed before set")
        }
        return validatedStep
    }

    @Override
    public void setValidatorFor(ProcessingStep step) {
        validatedStep = step;
    }

    @Override
    public boolean hasValidatedJobSucceeded() {
        if (validatedStepSucceeded == null) {
            throw new RuntimeException("Step not yet marked as validated")
        }
        return validatedStepSucceeded
    }
}
