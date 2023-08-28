/*
 * Copyright 2011-2019 The OTP authors
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
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterStatisticService

/**
 * This service is able to track the execution of jobs on the cluster.
 * It is used by {@link MonitoringJob}s which need to know when a
 * job finished on the cluster. These MonitoringJobs register a collection of cluster job for watching.
 *
 * The service performs a scheduled checking for all registered cluster jobs and
 * notifies the MonitoringJobs the cluster job belongs to.
 *
 * @Deprecated Old job system
 */
@CompileDynamic
@Deprecated
@Component
@Slf4j
class OldClusterJobMonitor extends AbstractClusterJobMonitor {

    @Autowired
    Scheduler scheduler

    @Autowired
    SchedulerService schedulerService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ClusterStatisticService clusterStatisticService

    OldClusterJobMonitor() {
        super('Old system')
    }

    @Scheduled(fixedDelay = 30000L)
    void check() {
        if (!schedulerService.isActive()) {
            return // job system is inactive
        }

        doCheck()
    }

    @Override
    protected List<ClusterJob> findAllClusterJobsToCheck() {
        return ClusterJob.findAllByCheckStatusAndOldSystem(ClusterJob.CheckStatus.CHECKING, true)
    }

    @Override
    @Transactional
    protected void handleFinishedClusterJobs(final ClusterJob clusterJob) {
        MonitoringJob monitoringJob = schedulerService.getJobForProcessingStep(clusterJob.processingStep)
        scheduler.doWithErrorHandling(monitoringJob, {
            assert monitoringJob: """\n\n-----------------------------------------------
No monitor job found for ${clusterJob.processingStep}
Only following monitors available:
${schedulerService.running.collect { "    ${it}  ${it.processingStep}" }.join('\n')}

"""

            boolean jobHasFinished
            ExecutionState jobEndState
            try {
                jobEndState = monitoringJob.endState
                jobHasFinished = true
            } catch (final InvalidStateException e) {
                // MonitoringJob.endState is specified to throw an InvalidStateException if the
                // job has not finished yet.
                jobHasFinished = false
            }
            if (jobHasFinished) {
                if (jobEndState == ExecutionState.FAILURE) {
                    log.info("NOT notifying ${monitoringJob} that cluster job ${clusterJob.clusterJobId}" +
                            " has finished on realm ${clusterJob.realm}, because that job has already failed.")
                } else if (jobEndState == ExecutionState.SUCCESS) {
                    log.debug("Cluster Job finished successfully but is still monitoring" +
                            " ${clusterJob.clusterJobId} on realm ${clusterJob.realm}.")
                } else {
                    throw new OtpRuntimeException("${monitoringJob} is still monitoring cluster job" +
                            " ${clusterJob.clusterJobId} on realm ${clusterJob.realm}, although it has" +
                            " already finished with end state ${jobEndState}.")
                }
            } else {
                log.info("Notifying ${monitoringJob} that cluster job ${clusterJob.clusterJobId}" +
                        " has finished on realm ${clusterJob.realm}.")
                scheduler.doInJobContext(monitoringJob) {
                    monitoringJob.finished(clusterJob)
                }
            }
        }, false)
    }

    @Override
    protected Map<ClusterJobIdentifier, JobState> retrieveKnownJobsWithState(Realm realm) {
        return clusterJobSchedulerService.retrieveKnownJobsWithState(realm)
    }

    @Override
    protected void retrieveAndSaveJobStatisticsAfterJobFinished(ClusterJob clusterJob) {
        clusterStatisticService.retrieveAndSaveJobStatisticsAfterJobFinished(clusterJob)
    }
}
