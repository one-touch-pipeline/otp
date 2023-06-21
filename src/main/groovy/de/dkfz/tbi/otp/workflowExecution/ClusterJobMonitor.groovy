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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.scheduler.AbstractClusterJobMonitor
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterStatisticService

/**
 * The monitor checks periodically the cluster jobs for ending ones and in case all cluster jbs
 * of an otp job finish it creates the next job.
 */
@Component
@Slf4j
class ClusterJobMonitor extends AbstractClusterJobMonitor {

    @Autowired
    WorkflowSystemService workflowSystemService

    @Autowired
    JobService jobService

    @Autowired
    ClusterStatisticService clusterStatisticService

    ClusterJobMonitor() {
        super('New system')
    }

    @Scheduled(fixedDelay = 30000L)
    void check() {
        if (!workflowSystemService.enabled) {
            return //job system is inactive
        }

        doCheck()
    }

    @CompileDynamic
    @Override
    protected List<ClusterJob> findAllClusterJobsToCheck() {
        return ClusterJob.findAllByCheckStatusAndOldSystem(ClusterJob.CheckStatus.CHECKING, false)
    }

    @Override
    @Transactional
    protected void handleFinishedClusterJobs(ClusterJob clusterJob) {
        clusterJob.refresh()
        if (clusterJob.workflowStep.clusterJobs.every { it.checkStatus == ClusterJob.CheckStatus.FINISHED }) {
            jobService.createNextJob(clusterJob.workflowStep.workflowRun)
        }
    }

    @Override
    protected Map<ClusterJobIdentifier, JobState> retrieveKnownJobsWithState(Realm realm) {
        return clusterStatisticService.retrieveKnownJobsWithState(realm)
    }

    @Override
    protected void retrieveAndSaveJobStatisticsAfterJobFinished(ClusterJob clusterJob) {
        clusterStatisticService.retrieveAndSaveJobStatisticsAfterJobFinished(clusterJob)
    }
}
