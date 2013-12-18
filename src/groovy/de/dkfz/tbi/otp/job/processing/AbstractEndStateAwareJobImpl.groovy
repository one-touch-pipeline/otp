package de.dkfz.tbi.otp.job.processing

/**
 * Abstract base class for {@link EndStateAwareJob}s.
 * @see EndStateAwareJob
 */
abstract public class AbstractEndStateAwareJobImpl extends AbstractJobImpl implements EndStateAwareJob {

   private ExecutionState endState = null

   /**
    * Empty default constructor. Required by Spring.
    */
   protected AbstractEndStateAwareJobImpl() {
   }

   /**
    * Constructor used by the factory method. Each implementing sub-class gets a matching Constructor injected.
    * @param processingStep The processing step for this Job
    * @param inputParameters The input parameters for this Job
    */
   protected AbstractEndStateAwareJobImpl(ProcessingStep processingStep, Collection<Parameter> inputParameters) {
       super(processingStep, inputParameters)
   }

    /**
     * Can be used by an implementing Job to set the Job as failed.
     * @see succeed
     * @deprecated Throw an exception with a meaningful message instead.
     */
    @Deprecated
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
}
