package de.dkfz.tbi.otp.job.processing

/**
 * This interface can be used for Jobs which are able to decide whether they succeeded or failed.
 *
 * Not all Jobs are able to decide whether their operation succeeded or failed. For Jobs which are
 * able to decide whether they succeeded or not this interface can be used instead of the more
 * generic Job interface.
 *
 * The interface can be used by the framework to directly determine whether the job succeeded.
 * This interface should only be used by short jobs. A job should not perform own error handling
 * in order to determine whether it succeeded. Such tasks are better passed to a {@link ValidatingJob}.
 */
public interface EndStateAwareJob extends Job {
    /**
     * @return The ExecutionState after the method finished as determined by the Job itself.
     * @throws InvalidStateException If the Job execution has not yet finished.
     */
    public ExecutionState getEndState() throws InvalidStateException;
}
