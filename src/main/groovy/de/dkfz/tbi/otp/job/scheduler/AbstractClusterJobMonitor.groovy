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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.ClusterJobSchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm

@Component
@Slf4j
abstract class AbstractClusterJobMonitor {

    final String name

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    protected AbstractClusterJobMonitor(String name) {
        this.name = name
    }

    @SuppressWarnings('CatchThrowable')
    protected void doCheck() {
        List<ClusterJob> clusterJobsToCheck = findAllClusterJobsToCheck()
        log.debug("${name}: Check for finished cluster jobs: ${clusterJobsToCheck.size()}")

        clusterJobsToCheck.groupBy { ClusterJob clusterJob ->
            clusterJob.realm
        }.each { Realm realm, List<ClusterJob> clusterJobs ->
            Map<ClusterJobIdentifier, ClusterJobStatus> jobStates = [:]
            List<String> finishedClusterJobIds = []
            try {
                jobStates = clusterJobSchedulerService.retrieveKnownJobsWithState(realm)
                log.debug("${name}: Retrieving job states for ${realm}")
            } catch (Throwable e) {
                log.error("${name}: Retrieving job states for ${realm} failed, skip", e)
                return
            }

            clusterJobs.each { ClusterJob clusterJob ->
                ClusterJobStatus status = jobStates.get(new ClusterJobIdentifier(clusterJob), ClusterJobStatus.COMPLETED)
                boolean completed = (status == ClusterJobStatus.COMPLETED)
                boolean unknown = (status == ClusterJobStatus.UNKNOWN)
                log.debug("${name}: Checking cluster job ID ${clusterJob.clusterJobId}: " +
                        "${completed ? 'finished' : unknown ? 'state UNKNOWN' : 'still running'}")
                if (completed) {
                    handleFinishedClusterJob(clusterJob)
                    finishedClusterJobIds.add(clusterJob.clusterJobId)
                }
            }
            log.debug("${name}: Finshed ${finishedClusterJobIds.size()} cluster jobs on ${realm}" +
                    "${finishedClusterJobIds ? ": ${finishedClusterJobIds.sort().join(', ')}" : ""}")
        }
    }

    protected void handleFinishedClusterJob(ClusterJob clusterJob) {
        saveJobFinishedInformation(clusterJob)
        handleFinishedClusterJobs(clusterJob)
    }

    @SuppressWarnings('CatchThrowable')
    private void saveJobFinishedInformation(ClusterJob clusterJob) {
        ClusterJob.withTransaction {
            clusterJob.refresh()
            try {
                clusterJobSchedulerService.retrieveAndSaveJobStatisticsAfterJobFinished(clusterJob)
            } catch (Throwable e) {
                log.warn("${name}: Failed to fill in runtime statistics for ${clusterJob}", e)
            }
            clusterJob.checkStatus = ClusterJob.CheckStatus.FINISHED
            clusterJob.save(flush: true)
        }
    }

    abstract protected List<ClusterJob> findAllClusterJobsToCheck()

    abstract protected void handleFinishedClusterJobs(final ClusterJob clusterJob)
}
