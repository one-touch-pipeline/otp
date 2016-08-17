package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

@Component
@Scope("prototype")
@UseJobLog
class TestMultiJob extends AbstractMultiJob {

    @Override
    protected NextAction execute(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        return executeImpl(finishedClusterJobs)
    }

    public NextAction executeImpl(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        throw new Error("This method must be overridden.")
    }
}
