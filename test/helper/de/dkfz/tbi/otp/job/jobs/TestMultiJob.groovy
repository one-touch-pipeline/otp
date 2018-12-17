package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.processing.NextAction

@Component
@Scope("prototype")
@UseJobLog
class TestMultiJob extends AbstractMultiJob {

    @Override
    protected NextAction execute(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        return executeImpl(finishedClusterJobs)
    }

    NextAction executeImpl(final Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        throw new Error("This method must be overridden.")
    }
}
