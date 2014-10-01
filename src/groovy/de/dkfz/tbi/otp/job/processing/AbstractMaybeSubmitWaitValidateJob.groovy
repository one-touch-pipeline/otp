package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

/**
 * Base class for jobs which maybe submit cluster jobs, wait for them to finish, and then validate their results.
 *
 */
abstract class AbstractMaybeSubmitWaitValidateJob extends AbstractMultiJob {

    JobStatusLoggingService jobStatusLoggingService

    @Override
    protected final NextAction execute(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        if (finishedClusterJobs == null) {
            return maybeSubmit()
        } else {
            def failedClusterJobs = jobStatusLoggingService.failedOrNotFinishedClusterJobs(processingStep, finishedClusterJobs)
            if (failedClusterJobs.empty) {
                log.info "All ${finishedClusterJobs.size()} cluster jobs have finished successfully."
            } else {
                throw new RuntimeException("${failedClusterJobs.size()} of ${finishedClusterJobs.size()} cluster jobs failed: ${failedClusterJobs}")
            }
            validate()
            return NextAction.SUCCEED
        }
    }

    /**
     * Called when the job is started.
     *
     * @return What should be done next. If cluster jobs were submitted during the execution of this method, the method
     * must return {@link NextAction#WAIT_FOR_CLUSTER_JOBS}, otherwise it must return {@link NextAction#SUCCEED}.
     * In case of {@link NextAction#WAIT_FOR_CLUSTER_JOBS}, the job system will notify this job about the cluster jobs
     * having finished by calling the {@link #validate()} method.
     *
     * @throws Throwable Throwing will make this job fail.
     */
    protected abstract NextAction maybeSubmit() throws Throwable

    /**
     * Called when <em>all</em> of the cluster jobs which were submitted by {@link #maybeSubmit()} have finished
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
     * class than the instance that the {@link #maybeSubmit()} call was made on. So you cannot rely on instance
     * variables for transferring information between the method calls.</strong>
     *
     * @throws Throwable Throwing will make this job fail.
     */
    protected abstract void validate() throws Throwable
}
