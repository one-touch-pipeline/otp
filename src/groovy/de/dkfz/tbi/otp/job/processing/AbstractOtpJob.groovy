package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.infrastructure.*

abstract class AbstractOtpJob extends AbstractMaybeSubmitWaitValidateJob{

    @Override
    protected String getLogFilePaths(ClusterJob clusterJob) {
        return getLogFileNames(clusterJob)
    }

    public static String getLogFileNames(ClusterJob clusterJob) {
        return "Log file: ${ClusterJobLoggingService.logDirectory(clusterJob.realm, clusterJob.processingStep)}/${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}"
    }

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {
        return jobStatusLoggingService.failedOrNotFinishedClusterJobs(processingStep, finishedClusterJobs).collectEntries{[(it): "Reason unknown"]}
    }
}
