package de.dkfz.tbi.otp.job.processing

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier

abstract class AbstractOtpJob extends AbstractMaybeSubmitWaitValidateJob{

    @Autowired
    JobStatusLoggingService jobStatusLoggingService

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        return jobStatusLoggingService.failedOrNotFinishedClusterJobs(processingStep, finishedClusterJobs).collectEntries{[(it): "Reason Unknown."]}
    }
}
