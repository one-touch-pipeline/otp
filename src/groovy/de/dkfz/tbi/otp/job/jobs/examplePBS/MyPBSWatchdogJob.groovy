package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import java.util.concurrent.locks.*

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

    final int defaultTimeout = 60

    private List<String> queuedJobIds = []
    private final Lock lock = new ReentrantLock()

    public void execute() throws Exception {
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
                clusterJobMonitoringService.monitor(queuedJobIds.collect { new ClusterJobIdentifier(realm, it, realm.unixUser) }, this)
            } else {
                Collection<ClusterJobIdentifier> jobIdentifiers = [realms, queuedJobIds].transpose().collect {
                    new ClusterJobIdentifier(it[0], it[1], it[0].unixUser)
                }
                clusterJobMonitoringService.monitor(jobIdentifiers, this)
            }
        }
    }

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
