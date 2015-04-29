package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.job.jobs.WatchdogJob
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.scheduler.PbsJobInfo
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.job.processing.ResumableJob
import de.dkfz.tbi.otp.job.scheduler.PbsMonitorService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * @deprecated Do not use a separate watchdog job.
 * Instead create/use a subclass of {@link AbstractMultiJob}, so restarting the job will resubmit the cluster jobs.
 */
@Deprecated @ResumableJob
class MyPBSWatchdogJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {
    @Autowired
    PbsMonitorService pbsMonitorService

    @Autowired
    SchedulerService schedulerService

    final int defaultTimeout = 60

    private List<String> queuedJobIds = []
    private final Lock lock = new ReentrantLock()

    public void execute() throws Exception {
        List<Realm> realms = []
        String jobIds = getParameterValueOrClass(JobParameterKeys.PBS_ID_LIST)
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
                pbsMonitorService.monitor(queuedJobIds.collect { new PbsJobInfo(realm: realms.first(), pbsId: it) }, this)
            } else {
                Collection<PbsJobInfo> pbsJobInfos = [queuedJobIds, realms].transpose().collect {
                    new PbsJobInfo(pbsId: it[0], realm: it[1])
                }
                pbsMonitorService.monitor(pbsJobInfos, this)
            }
        }
    }

    void finished(String pbsId, Realm realm) {
        final boolean allFinished
        lock.lock()
        try {
            queuedJobIds.remove(pbsId)
            log.debug("${pbsId} on realm ${realm} finished")
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
        List<String> pbsIds = jobIds.tokenize(",")
        return pbsIds
    }
}
