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

package de.dkfz.tbi.otp.job.jobs.examplePBS

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.jobs.WatchdogJob
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ClusterJobMonitoringService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * @deprecated Do not use a separate watchdog job.
 * Instead create/use a subclass of {@link AbstractMultiJob}, so restarting the job will resubmit the cluster jobs.
 */
@Component
@Scope("prototype")
@Deprecated @ResumableJob
@UseJobLog
class MyPBSWatchdogJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {
    @Autowired
    ClusterJobMonitoringService clusterJobMonitoringService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    ConfigService configService

    final int defaultTimeout = 60

    private List<String> queuedJobIds = []
    private final Lock lock = new ReentrantLock()

    @Override
    void execute() throws Exception {
        List<Realm> realms = []
        String jobIds = getParameterValueOrClass(JobParameterKeys.JOB_ID_LIST)
        String realmIds = getParameterValueOrClass(JobParameterKeys.REALM)

        queuedJobIds = parseInputString(jobIds)
        if ([WatchdogJob.SKIP_WATCHDOG] == queuedJobIds) {
            log.debug "Skip watchdog"
            succeed()
            schedulerService.doEndCheck(this)
        } else {
            assert realmIds != WatchdogJob.SKIP_WATCHDOG
            realms = parseInputString(realmIds).collect( { CollectionUtils.exactlyOneElement(Realm.findAllById(Long.parseLong(it))) } )
            if (realms.size() == 1) {
                Realm realm = CollectionUtils.exactlyOneElement(realms)
                clusterJobMonitoringService.monitor(queuedJobIds.collect { new ClusterJobIdentifier(realm, it, configService.getSshUser()) }, this)
            } else {
                Collection<ClusterJobIdentifier> jobIdentifiers = [realms, queuedJobIds].transpose().collect {
                    new ClusterJobIdentifier(it[0], it[1], configService.getSshUser())
                }
                clusterJobMonitoringService.monitor(jobIdentifiers, this)
            }
        }
    }

    @Override
    void finished(ClusterJobIdentifier finishedClusterJob) {
        final boolean allFinished
        lock.lock()
        try {
            queuedJobIds.remove(finishedClusterJob.clusterJobId)
            log.debug("${finishedClusterJob} finished")
            allFinished = queuedJobIds.empty
        } finally {
            lock.unlock()
        }
        if (allFinished) {
            succeed()
            schedulerService.doEndCheck(this)
        }
    }

    private List<String> parseInputString(String jobIds) {
        return jobIds.tokenize(",")
    }
}
