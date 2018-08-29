package de.dkfz.tbi.otp.job.processing

/**
 * A RestartedProcessingStep is a special ProcessingStep for keeping track of restarted ProcessingSteps.
 *
 * When a ProcessingStep gets restarted an instance of this domain class needs to be created.
 * This class holds a back reference to the original ProcessingStep, so that the complete history
 * of Processing is still available.
 *
 * By using a RestartedProcessingStep for the restart of a ProcessingStep the link between the
 * various ProcessingSteps of a Process are still correct. The original ProcessingStep has it's
 * previous and next links unchanged. The RestartedProcessingStep points to the same previous step
 * as the original one and the next points to the next step run after this one. The previous step's
 * next pointer is adjusted to this instance, so that the link between all previous/next points to
 * the latest executed branch. Nevertheless the connection to the restarted jobs is still present
 * and completely browsable through the original link. Even the information that a branching exists
 * is implicitly available through the instance of this class.
 *
 * @see ProcessingStep
 */
class RestartedProcessingStep extends ProcessingStep {
    /**
     * The original ProcessingStep from which this ProcessingStep had been created as a restart.
     */
    ProcessingStep original

    static constraints = {
        original(nullable: false)
    }

    /**
     * Creates a RestartedProcessingStep from given ProcessingStep.
     * @param step The original to be restarted ProcessingStep
     * @return The RestartedProcessingStep
     */
    public static RestartedProcessingStep create(ProcessingStep step) {
        RestartedProcessingStep restartedStep = new RestartedProcessingStep()
        restartedStep.jobDefinition = step.jobDefinition
        restartedStep.process = step.process
        restartedStep.previous = step.previous
        restartedStep.original = step
        assert(restartedStep.validate())
        return restartedStep
    }
}
