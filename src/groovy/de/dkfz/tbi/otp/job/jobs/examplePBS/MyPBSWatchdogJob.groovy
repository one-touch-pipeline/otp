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
