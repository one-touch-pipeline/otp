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
package de.dkfz.tbi.otp.job.scheduler

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.ClusterJobSchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils

@Component
@Slf4j
abstract class AbstractClusterJobMonitor {

    final String name

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    protected AbstractClusterJobMonitor(String name) {
        this.name = name
    }

    /**
     * do all checks for the monitoring.
     */
    protected void doCheck() {
        List<ClusterJob> clusterJobsToCheck = fetchClusterJobsFromDatabase()

        clusterJobsToCheck.groupBy { ClusterJob clusterJob ->
            clusterJob.realm
        }.each { Realm realm, List<ClusterJob> clusterJobs ->
            LogUsedTimeUtils.logUsedTime("${name}: handle cluster jobs for realm ${realm.name}") {
                checkClusterAndClusterJobs(realm, clusterJobs)
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
        clusterJobsToCheck*.realm.unique()*.name //init realm, since it is needed later outside the transaction
        log.debug("${name}: Check for finished cluster jobs: ${clusterJobsToCheck.size()}")
        return clusterJobsToCheck
    }

    /**
     * Helper to get the clusterJob states from the cluster and do checks using {@link #checkClusterJobs(Realm, List, Map)}
     */
    @SuppressWarnings('CatchThrowable')
    protected void checkClusterAndClusterJobs(Realm realm, List<ClusterJob> clusterJobs) {
        Map<ClusterJobIdentifier, ClusterJobStatus> jobStates
        try {
            LogUsedTimeUtils.logUsedTime("${name}: fetch cluster jobs state from cluster on realm ${realm.name}") {
                jobStates = clusterJobSchedulerService.retrieveKnownJobsWithState(realm)
            }
            log.debug("${name}: Retrieving job states for ${realm}: ${jobStates.size()}")
        } catch (Throwable e) {
            log.error("${name}: Retrieving job states for ${realm} failed, skip", e)
            return
        }

        LogUsedTimeUtils.logUsedTime("${name}: checking all ${clusterJobs.size()} cluster jobs") {
            checkClusterJobs(realm, clusterJobs, jobStates)
        }
    }

    /**
     * Helper to do the finish checking of each job and handler finished jobs via {@link #handleFinishedClusterJobWrapper(ClusterJob)}
     */
    protected void checkClusterJobs(Realm realm, List<ClusterJob> clusterJobs, Map<ClusterJobIdentifier, ClusterJobStatus> jobStates) {
        List<String> finishedClusterJobIds = []
        clusterJobs.each { ClusterJob clusterJob ->
            ClusterJobStatus status = jobStates.get(new ClusterJobIdentifier(clusterJob), ClusterJobStatus.COMPLETED)
            boolean completed = (status == ClusterJobStatus.COMPLETED)
            boolean unknown = (status == ClusterJobStatus.UNKNOWN)
            log.debug("${name}: Checking cluster job ID ${clusterJob.clusterJobId}: " +
                    "${completed ? 'finished' : unknown ? 'state UNKNOWN' : 'still running'}")

            if (completed) {
                handleFinishedClusterJobWrapper(clusterJob)
                finishedClusterJobIds.add(clusterJob.clusterJobId)
            }
        }
        log.debug("${name}: Finshed ${finishedClusterJobIds.size()} cluster jobs on ${realm}" +
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
            clusterJobSchedulerService.retrieveAndSaveJobStatisticsAfterJobFinished(clusterJob)
        } catch (Throwable e) {
            log.warn("${name}: Failed to fill in runtime statistics for ${clusterJob}", e)
        }
        clusterJob.checkStatus = ClusterJob.CheckStatus.FINISHED
        clusterJob.save()
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

}
