/*
 * Copyright 2011-2020 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.workflowExecution.cluster

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.slf4j.event.Level

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.utils.logging.SimpleLogger
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.cluster.logs.ClusterLogQueryResultFileService

/**
 * A service to fill the {@link ClusterJob}s statistics.
 */
@GrailsCompileStatic
@Transactional
class ClusterStatisticService {

    static final int WAITING_TIME_FOR_SECOND_TRY_IN_MILLISECONDS = (Environment.current == Environment.TEST) ? 0 : 10000

    ClusterJobManagerFactoryService clusterJobManagerFactoryService

    ClusterLogQueryResultFileService clusterLogQueryResultFileService

    ClusterJobService clusterJobService

    FileService fileService

    LogService logService

    /**
     * Returns a map of jobs the cluster job scheduler knows about
     *
     * @param realm The realm to connect to
     * @param userName The name of the user whose jobs should be checked
     * @return A map containing job identifiers and their status
     */
    Map<ClusterJobIdentifier, JobState> retrieveKnownJobsWithState(Realm realm) throws Exception {
        assert realm: "No realm specified."
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(realm)

        Map<BEJobID, JobState> jobStates = queryAndLogAllClusterJobs(jobManager)

        return jobStates.collectEntries { BEJobID jobId, JobState state ->
            [
                    new ClusterJobIdentifier(realm, jobId.id),
                    state,
            ]
        } as Map<ClusterJobIdentifier, JobState>
    }

    private Map<BEJobID, JobState> queryAndLogAllClusterJobs(BatchEuphoriaJobManager jobManager) {
        Map<BEJobID, JobState> jobStates
        StringBuilder logStringBuilder = new StringBuilder()
        LogThreadLocal.withThreadLog(logStringBuilder) {
            ((SimpleLogger) LogThreadLocal.threadLog).level = Level.DEBUG
            jobStates = jobManager.queryJobStatusAll()
        }

        fileService.createFileWithContentOnDefaultRealm(clusterLogQueryResultFileService.logFileWithCreatingDirectory(), logStringBuilder.toString())

        return jobStates
    }

    @SuppressWarnings("CatchThrowable")
    void retrieveAndSaveJobInformationAfterJobStarted(ClusterJob clusterJob) throws Exception {
        BEJobID beJobID = new BEJobID(clusterJob.clusterJobId)
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(clusterJob.realm)
        GenericJobInfo jobInfo

        try {
            jobInfo = jobManager.queryExtendedJobStateById([beJobID]).get(beJobID)
            if (jobInfo?.jobState == JobState.UNKNOWN) {
                throw new ClusterJobException("Jobstate is ${JobState.UNKNOWN} for ${clusterJob.clusterJobId} (${clusterJob.id})")
            }
            clusterJobService.amendClusterJob(clusterJob, jobInfo)
        } catch (Throwable ignored) {
            logService.addSimpleLogEntry(clusterJob.workflowStep,
                    "Failed to fill in runtime statistics after start for ${clusterJob.clusterJobId}, try again")
            Thread.sleep(WAITING_TIME_FOR_SECOND_TRY_IN_MILLISECONDS)
            try {
                jobInfo = jobManager.queryExtendedJobStateById([beJobID]).get(beJobID)
                clusterJobService.amendClusterJob(clusterJob, jobInfo)
            } catch (Throwable ignored2) {
                logService.addSimpleLogEntry(clusterJob.workflowStep,
                        "Failed to fill in runtime statistics after start for ${clusterJob.clusterJobId} the second time")
            }
        }
    }

    void retrieveAndSaveJobStatisticsAfterJobFinished(ClusterJob clusterJob) throws Exception {
        BEJobID beJobId = new BEJobID(clusterJob.clusterJobId)
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(clusterJob.realm)
        GenericJobInfo jobInfo = jobManager.queryExtendedJobStateById([beJobId]).get(beJobId)

        if (jobInfo) {
            ClusterJob.Status status = null
            if (jobInfo.jobState && jobInfo.exitCode != null) {
                status = jobInfo.jobState.isCompleted() && jobInfo.exitCode == 0 ? ClusterJob.Status.COMPLETED : ClusterJob.Status.FAILED
            }
            clusterJobService.completeClusterJob(clusterJob, status, jobInfo)
        }
    }
}
