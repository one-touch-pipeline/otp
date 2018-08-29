package de.dkfz.tbi.otp.job.processing

/**
 * Interface for jobs that are resumable in certain phases of execution, but not in all.
 *
 * @see ResumableJob
 */
interface SometimesResumableJob extends Job {

    /**
     * Requests this job to go into a resumable state.
     *
     * <p>
     * This method must return as quickly as possible. It must <em>not</em> wait until the job
     * becomes resumable.
     */
    void planSuspend()

    /**
     * Allows this job to exit the resumable state.
     *
     * @see #isResumable()
     */
    void cancelSuspend()

    /**
     * Returns whether this job is currently in a resumable state.
     *
     * <p>
     * <strong>If {@link #planSuspend()} has been called before and this method returns
     * <code>true</code>, then this job must stay in the resumable state until
     * {@link #cancelSuspend()} is called.</strong>
     *
     * @return Whether this job is currently in a resumable state.
     */
    boolean isResumable()
}
