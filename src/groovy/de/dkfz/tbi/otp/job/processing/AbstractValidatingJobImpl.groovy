package de.dkfz.tbi.otp.job.processing

/**
 * Abstract base class for {@link ValidatingJob}s.
 * @see ValidatingJob
 */
abstract public class AbstractValidatingJobImpl extends AbstractEndStateAwareJobImpl implements ValidatingJob {

    /**
     * Dependency injection of pbs service
     */
    def pbsService

    /**
     * Default empty constructor
     */
    public AbstractValidatingJobImpl() {
    }
    public AbstractValidatingJobImpl(ProcessingStep processingStep, Collection<Parameter> inputParameters) {
        super(processingStep, inputParameters)
    }

    @Override
    public List<ProcessingStep> getValidatorFor() {
        return [ProcessingStep.get(processingStep.previous.id)]
    }

    /**
     * Pass-through method accessing Pbshelper's method
     * 
     * @param pbsIds The pbsIds to be validated
     * @return Map indicating if jobs are running identified by their pbsIds
     */
    public Map<String, Boolean> validate(List<String> pbsIds) {
        if(!pbsIds) {
            return [:]
        }
        return pbsService.validate(pbsIds)
    }
}
