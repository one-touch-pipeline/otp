package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDecision
import de.dkfz.tbi.otp.job.plan.DecisionMapping

/**
 * Abstract base class for {@link DecisionJob}s.
 * @see DecisionJob
 */
abstract public class AbstractDecisionJobImpl extends AbstractJobImpl implements DecisionJob {
    private JobDecision decision
    private ExecutionState endState = null

    /**
     * Default empty constructor
     */
    public AbstractDecisionJobImpl() {
    }
    public AbstractDecisionJobImpl(ProcessingStep processingStep,
            Collection<Parameter> inputParameters) {
        super(processingStep, inputParameters)
    }

    /**
     * Can be used by an implementing Job to set the Job as failed.
     * @see succeed
     */
    protected final void fail() {
        this.endState = ExecutionState.FAILURE
    }

    /**
     * Can be used by an implementing Job to set the Job as succeeded.
     * @see fail
     */
    protected final void succeed() {
        this.endState = ExecutionState.SUCCESS
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
        // TODO: verify that decision belongs to current Job Definition
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
    public final ExecutionState getEndState() throws InvalidStateException {
        if (!endState) {
            throw new InvalidStateException("EndState accessed without end state being set")
        }
        if (getState() != ExecutionState.FINISHED && getState() != ExecutionState.SUCCESS) {
            throw new InvalidStateException("EndState accessed but not in finished state")
        }
        return endState
    }

    @Override
    public final JobDecision getDecision() throws InvalidStateException {
        if (getState() != ExecutionState.FINISHED && endState != ExecutionState.SUCCESS) {
            throw new InvalidStateException("Decision accessed, but not in succeeded state")
        }
        return decision
    }
}
