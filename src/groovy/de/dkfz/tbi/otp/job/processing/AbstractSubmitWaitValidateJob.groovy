package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

/**
 * Base class for jobs which submit cluster jobs, wait for them to finish, and then validate their results.
 *
 */
abstract class AbstractSubmitWaitValidateJob extends AbstractMaybeSubmitWaitValidateJob {

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        submit()
        return NextAction.WAIT_FOR_CLUSTER_JOBS
    }

    /**
     * Called when the job is started.
     *
     * <p>
     * This method must submit at least one cluster job. If you need more flexibility, subclass
     * {@link AbstractMaybeSubmitWaitValidateJob} or {@link AbstractMultiJob} directly.
     *
     * <p>
     * After this method returns, the job system will wait for the submitted cluster jobs to
     * finish and then call the {@link #validate()} method.
     *
     * @throws Throwable Throwing will make this job fail.
     */
    protected abstract void submit() throws Throwable

    // This method is overridden just for providing slightly different JavaDoc than the superclass.
    /**
     * Called when <em>all</em> of the cluster jobs which were submitted by {@link #submit()} have finished
     * <em>successfully</em>.
     *
     * <p>
     * After this method returns, this job will succeed and finish.
     *
     * <p>
     * This method must not submit any cluster jobs. If you need more flexibility, subclass {@link AbstractMultiJob}
     * directly.
     *
     * <p>
     * <strong>TODO: As long as OTP-1026 is not resolved, this method must return quickly.</strong>
     *
     * <p>
     * <strong>This method might be called on a <em>different</em> instance of the implementing
     * class than the instance that the {@link #submit()} call was made on. So you cannot rely on instance
     * variables for transferring information between the method calls.</strong>
     *
     * @throws Throwable Throwing will make this job fail.
     */
    protected abstract void validate() throws Throwable
}
