package de.dkfz.tbi.otp.job.jobs.examplePBS

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.job.processing.ResumableJob
import de.dkfz.tbi.otp.job.scheduler.PbsMonitorService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * @deprecated This watchdog is deprecated in favor of {@link WatchdogJob} which also provides logging capabilities.
 *             It was meant only as an example.
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
        String jobIds = getParameterValueOrClass("__pbsIds")
        queuedJobIds = parseInputString(jobIds)
        pbsMonitorService.monitor(queuedJobIds, this)
    }

    void finished(String pbsId, Realm realm) {
        lock.lock()
        try {
            queuedJobIds.remove(pbsId)
            log.debug("${pbsId} finished")
        } finally {
            lock.unlock()
        }
        if (queuedJobIds.empty) {
            succeed()
            schedulerService.doEndCheck(this)
        }
    }

    private List<String> parseInputString(String jobIds) {
        List<String> pbsIds = jobIds.tokenize(",")
        return pbsIds
    }
}
