package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

/**
 * Base class for jobs which maybe submit cluster jobs, wait for them to finish, and then validate their results.
 */
abstract class AbstractMaybeSubmitWaitValidateJob extends AbstractMultiJob {

    @Override
    protected final NextAction execute(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        if (finishedClusterJobs == null) {
            return maybeSubmit()
        } else {
            Map<ClusterJobIdentifier, String> failedClusterJobs = failedOrNotFinishedClusterJobs(finishedClusterJobs)
            if (failedClusterJobs.isEmpty()) {
                log.info "All ${finishedClusterJobs.size()} cluster jobs have finished successfully."
            } else {
                throw new RuntimeException(createExceptionString(failedClusterJobs, finishedClusterJobs))
            }
            validate()
            return NextAction.SUCCEED
        }
    }

    public String createExceptionString(Map<ClusterJobIdentifier,
            String> failedClusterJobs, Collection<? extends ClusterJobIdentifier> finishedClusterJobs) {
        """
${failedClusterJobs.size()} of ${finishedClusterJobs.size()} cluster jobs failed:
${
    failedClusterJobs.collect { ClusterJobIdentifier clusterJobIdentifier, String reason ->
        "${clusterJobIdentifier}: ${reason}\n${ClusterJob.findByClusterJobId(clusterJobIdentifier.clusterJobId).getLogFileNames()}\n"
    }.join("\n")
}
"""
    }


    /**
     * Returns all failed or not finished ClusterJobs
     */
    protected abstract Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable

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
     * <strong>This method may be called on a different thread, with a different persistence context and on another
     * instance of the implementing class than the {@link #maybeSubmit()} method.</strong> So:
     * <ul>
     *     <li>Do not share domain objects between the methods.</li>
     *     <li>Do not rely on instance variables for sharing information between the methods.</li>
     * </ul>
     *
     * @throws Throwable Throwing will make this job fail.
     */
    protected abstract void validate() throws Throwable
}
