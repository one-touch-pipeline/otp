package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDecision

/**
 * Abstract base class for {@link DecisionJob}s.
 * @see DecisionJob
 */
abstract public class AbstractDecisionJobImpl extends AbstractEndStateAwareJobImpl implements DecisionJob {
    private JobDecision decision

    /**
     * Default empty constructor
     */
    public AbstractDecisionJobImpl() {
    }
    public AbstractDecisionJobImpl(ProcessingStep processingStep, Collection<Parameter> inputParameters) {
        super(processingStep, inputParameters)
    }

    /**
     * Has to be called by an implementing Job to set the decision the
     * Job decided on. Automatically sets the Job as succeeded. If {@code decision}
     * is {@code null} the Job is set to failed.
     *
     * This method can be called multiple times.
     * @param decision The decision taken by the Job.
     * @see succeed
     * @see fail
     */
    protected final void setDecision(JobDecision decision) {
        if (decision) {
            succeed()
        } else {
            fail()
        }
        if (decision.jobDefinition != getProcessingStep().jobDefinition) {
            throw new IncorrectProcessingException("Decision does not belong to current JobDefinition")
        }
        this.decision = decision
    }

    /**
     *
     * @return List of available decisions the Job can take ordered by ID.
     */
    protected final List<JobDecision> getAvailableDecisions() {
        return JobDecision.findAllByJobDefinition(getProcessingStep().jobDefinition).sort { it.id }
    }

    @Override
    public final JobDecision getDecision() throws InvalidStateException {
        if (getState() != ExecutionState.FINISHED && endState != ExecutionState.SUCCESS) {
            throw new InvalidStateException("Decision accessed, but not in succeeded state")
        }
        return decision
    }
}
