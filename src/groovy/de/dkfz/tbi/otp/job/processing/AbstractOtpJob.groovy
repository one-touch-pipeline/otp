package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier

abstract class AbstractOtpJob extends AbstractMaybeSubmitWaitValidateJob{

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        return jobStatusLoggingService.failedOrNotFinishedClusterJobs(processingStep, finishedClusterJobs).collectEntries{[(it): "Reason Unknown."]}
    }
}
