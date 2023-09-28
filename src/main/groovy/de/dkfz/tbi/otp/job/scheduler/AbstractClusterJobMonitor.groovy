/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.job.scheduler

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils

@Component
@Slf4j
abstract class AbstractClusterJobMonitor {

    final String name

    protected AbstractClusterJobMonitor(String name) {
        this.name = name
    }

    /**
     * do all checks for the monitoring.
     */
    protected void doCheck() {
        List<ClusterJob> clusterJobsToCheck = fetchClusterJobsFromDatabase()

        if (clusterJobsToCheck) {
            LogUsedTimeUtils.logUsedTime("${name}: handle cluster jobs") {
                checkClusterAndClusterJobs(clusterJobsToCheck)
            }
        }
    }

    /**
     * Transactional wrapper for the callback {@link #findAllClusterJobsToCheck()}.
     */
    @Transactional
    private List<ClusterJob> fetchClusterJobsFromDatabase() {
        List<ClusterJob> clusterJobsToCheck
        LogUsedTimeUtils.logUsedTime("${name}: fetch cluster jobs from database") {
            clusterJobsToCheck = findAllClusterJobsToCheck()
        }
        log.debug("${name}: Check for finished cluster jobs: ${clusterJobsToCheck.size()}")
        return clusterJobsToCheck
    }

    /**
     * Helper to get the clusterJob states from the cluster and do checks using {@link #checkClusterJobs(List, Map)}
     */
    @SuppressWarnings('CatchThrowable')
    protected void checkClusterAndClusterJobs(List<ClusterJob> clusterJobs) {
        Map<String, JobState> jobStates
        try {
            LogUsedTimeUtils.logUsedTime("${name}: fetch cluster jobs state from cluster") {
                jobStates = retrieveKnownJobsWithState()
            }
            log.debug("${name}: Retrieving job states: ${jobStates.size()}")
        } catch (Throwable e) {
            log.error("${name}: Retrieving job states failed, skip", e)
            return
        }

        LogUsedTimeUtils.logUsedTime("${name}: checking all ${clusterJobs.size()} cluster jobs") {
            checkClusterJobs(clusterJobs, jobStates)
        }
    }

    /**
     * Helper to do the finish checking of each job and handler finished jobs via {@link #handleFinishedClusterJobWrapper(ClusterJob)}
     */
    protected void checkClusterJobs(List<ClusterJob> clusterJobs, Map<String, JobState> jobStates) {
        List<String> finishedClusterJobIds = []
        clusterJobs.each { ClusterJob clusterJob ->
            JobState status = jobStates.get(clusterJob.clusterJobId, JobState.COMPLETED_UNKNOWN)
            log.debug("${name}: Checking cluster job ID ${clusterJob.clusterJobId}: ${status}")

            // .isDummy() and .isStarted() are roddy only states that should not occur
            if (status.isCompleted() || status.isFailed() || status.isDummy() || status.isStarted()) {
                handleFinishedClusterJobWrapper(clusterJob)
                finishedClusterJobIds.add(clusterJob.clusterJobId)
            }
        }
        log.debug("${name}: Finshed ${finishedClusterJobIds.size()} cluster jobs" +
                "${finishedClusterJobIds ? ": ${finishedClusterJobIds.sort().join(', ')}" : ""}")
    }

    /**
     * Transactional wrapper for {@link #saveJobFinishedInformation(ClusterJob)} and the callback {@link #handleFinishedClusterJobs(ClusterJob)}.
     */
    protected void handleFinishedClusterJobWrapper(ClusterJob clusterJob) {
        LogUsedTimeUtils.logUsedTime("${name}: handle finished cluster job ${clusterJob.clusterJobId}") {
            saveJobFinishedInformation(clusterJob)
            handleFinishedClusterJobs(clusterJob)
        }
    }

    /**
     * Retrieves statistic and save it.
     */
    @SuppressWarnings('CatchThrowable')
    @Transactional
    protected void saveJobFinishedInformation(ClusterJob clusterJob) {
        clusterJob.refresh()
        try {
            retrieveAndSaveJobStatisticsAfterJobFinished(clusterJob)
        } catch (Throwable e) {
            log.warn("${name}: Failed to fill in runtime statistics for ${clusterJob}", e)
        }
        clusterJob.checkStatus = ClusterJob.CheckStatus.FINISHED
        clusterJob.save(flush: true)
    }

    /**
     * callback to get all Cluster jobs to check for the monitor
     * @return List of ClusterJobs to check
     */
    abstract protected List<ClusterJob> findAllClusterJobsToCheck()

    /**
     * callback to handle finished cluster jobs for the workflow system this monitor is for
     */
    abstract protected void handleFinishedClusterJobs(final ClusterJob clusterJob)

    abstract protected Map<String, JobState> retrieveKnownJobsWithState()

    abstract protected void retrieveAndSaveJobStatisticsAfterJobFinished(ClusterJob clusterJob)

}
