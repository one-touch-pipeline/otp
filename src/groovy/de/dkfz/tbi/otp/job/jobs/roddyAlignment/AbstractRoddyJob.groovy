package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFiles
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.AbstractMaybeSubmitWaitValidateJob

/**
 * class for roddy jobs that handle failed or not finished cluster jobs, analyse them and provide
 * information about their failure for {@AbstractMaybeSubmitWaitValidateJob}
 */
abstract class AbstractRoddyJob extends AbstractMaybeSubmitWaitValidateJob{

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {

        RoddyBamFile roddyBamFile = getProcessParameterObject()
        assert roddyBamFile

        JobStateLogFiles jobStateLogFiles = JobStateLogFiles.create(roddyBamFile.getPathToJobStateLogFiles())

        return analyseFinishedClusterJobs(finishedClusterJobs, jobStateLogFiles)
    }

    public Map<ClusterJobIdentifier, String> analyseFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs, JobStateLogFiles jobStateLogFiles) {

        Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs = [:]

        finishedClusterJobs.each {
            if (!jobStateLogFiles.containsPbsId(it.clusterJobId)) {
                failedOrNotFinishedClusterJobs.put(it, "JobStateLogFile contains no information for ${it}")
            } else {
                if (jobStateLogFiles.isClusterJobInProgress(it.clusterJobId)) {
                    failedOrNotFinishedClusterJobs.put(it, "${it} is not finished.")
                } else if (!jobStateLogFiles.isClusterJobFinishedSuccessfully(it.clusterJobId)) {
                    failedOrNotFinishedClusterJobs.put(it, "${it} failed processing. ExitCode: ${jobStateLogFiles.getPropertyFromLatestLogFileEntry(it.clusterJobId, "exitCode")}")
                }
            }
        }
        return failedOrNotFinishedClusterJobs
    }
}
