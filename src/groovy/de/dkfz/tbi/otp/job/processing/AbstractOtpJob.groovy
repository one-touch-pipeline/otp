package de.dkfz.tbi.otp.job.processing

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier

abstract class AbstractOtpJob extends AbstractMaybeSubmitWaitValidateJob{

    @Autowired
    JobStatusLoggingService jobStatusLoggingService

    @Override
    protected String getLogFilePaths(ClusterJob clusterJob) {
        return getLogFileNames(clusterJob)
    }

    public static String getLogFileNames(ClusterJob clusterJob) {
        return "Log file: ${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}"
    }

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        return jobStatusLoggingService.failedOrNotFinishedClusterJobs(processingStep, finishedClusterJobs).collectEntries{[(it): "Reason unknown"]}
    }
}
