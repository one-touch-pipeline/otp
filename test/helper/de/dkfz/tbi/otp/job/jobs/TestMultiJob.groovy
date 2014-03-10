package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

class TestMultiJob extends AbstractMultiJob {

    @Override
    protected NextAction execute(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        return executeImpl(finishedClusterJobs)
    }

    public NextAction executeImpl(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        throw new Error("This method must be overridden.")
    }
}
