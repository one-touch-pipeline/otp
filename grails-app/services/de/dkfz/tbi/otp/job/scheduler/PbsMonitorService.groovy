package de.dkfz.tbi.otp.job.scheduler

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * This service is able to track the execution of jobs on the PBS.
 * It is used by {@link MonitoringJob}s which need to know when a
 * job finished on the PBS. These MonitoringJobs register a job for watching.
 *
 * The service performs a scheduled checking for all registered jobs and
 * notifies the MonitoringJobs which registered one job whenever the job
 * finished.
 */
class PbsMonitorService {
    static transactional = false
    /**
     * Dependency Injection of executionService.
     **/
    def executionService

    /**
     * Map of currently monitored jobs on the PBS (value) ordered
     * by the MonitoringJob which registered it (key).
     */
    private Map<MonitoringJob, List<PbsJobInfo>> queuedJobs = [:]
    /**
     * Lock to protect the queuedJobs. Needed because registering a
     * Job and checking the PBS for whether a Job finished might be
     * performed in different thread.
     */
    private final Lock lock = new ReentrantLock()

    /**
     * Registers the given PBS ID for monitoring.
     * Will check all PBS systems for the given ID.
     * @param pbsId The ID on the PBS system to monitor
     * @param  The monitoring Job to notify when the job finished on the PBS
     */
    void monitor(String pbsId, MonitoringJob pbsMonitor) {
        monitor(pbsId, null, pbsMonitor)
    }

    /**
     * Registers the given PBS ID for monitoring on the given realm.
     * @param pbsId The ID on the PBS system to monitor
     * @param realm The Realm on which the PBS job is running
     * @param pbsMonitor The monitoring Job to notify when the job finished on the PBS
     */
    void monitor(String pbsId, Realm realm, MonitoringJob pbsMonitor) {
        lock.lock()
        try {
            PbsJobInfo info = new PbsJobInfo(pbsId: pbsId, realm: realm)
            if (queuedJobs.containsKey(pbsMonitor)) {
                boolean append = true
                queuedJobs.get(pbsMonitor).each {
                    if (it.pbsId == pbsId) {
                        if (it.realm == realm || !it.realm) {
                            append = false
                        }
                    }
                }
                if (append) {
                    log.debug("Adding job for already registered MonitoringJob")
                    queuedJobs.get(pbsMonitor).add(info)
                }
            } else {
                log.debug("Adding one MonitoringJob")
                queuedJobs.put(pbsMonitor, [info])
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Registers the given PBS IDs for monitoring.
     * Will check all PBS systems for the given ID.
     * @param pbsIds The IDs on the PBS system to monitor
     * @param pbsMonitor The monitoring Job to notify when the job finished on the PBS
     */
    void monitor(List<String> pbsIds, MonitoringJob pbsMonitor) {
        monitor(pbsIds, null, pbsMonitor)
    }

    /**
     * Registers the given PBS IDs for monitoring on the given realm.
     * @param pbsIds The IDs on the PBS system to monitor
     * @param realm The Realm on which the PBS job is running
     * @param pbsMonitor The monitoring Job to notify when the job finished on the PBS
     */
    void monitor(List<String> pbsIds, Realm realm, MonitoringJob pbsMonitor) {
        lock.lock()
        try {
            pbsIds.each { String pbsId ->
                monitor(pbsId, realm, pbsMonitor)
            }
        } finally {
            lock.unlock()
        }
    }

    public void check() {
        if (queuedJobs.empty) {
            return
        }

        List<Realm> realms = Realm.list()

        Map<MonitoringJob, List<PbsJobInfo>> removal = [:]
        (new HashMap(queuedJobs)).each { MonitoringJob pbsMonitor, List<PbsJobInfo> pbsInfos ->
            List<PbsJobInfo> finishedJobs = []
            (new ArrayList(pbsInfos)).each { PbsJobInfo info ->
                log.debug("Checking pbs id ${info.pbsId}")
                boolean running = false
                if (info.realm) {
                    running = executionService.checkRunningJob(info.pbsId, info.realm)
                } else {
                    for (Realm realm in realms) {
                        if (executionService.checkRunningJob(info.pbsId, realm)) {
                            running = true
                            break
                        }
                    }
                }
                log.debug("${info.pbsId} still running: ${running ? 'yes' : 'no'}")
                if (!running) {
                    log.info("${info.pbsId} finished on Realm ${info.realm}")
                    try {
                        pbsMonitor.finished(info.pbsId, info.realm)
                    } catch (Exception e) {
                        // catching all exception thrown in the Job to not have it influence our own code
                        log.error(e.message, e)
                    }
                    finishedJobs.add(info)
                }
            }
            if (!finishedJobs.empty) {
                removal.put(pbsMonitor, finishedJobs)
            }
        }

        if (!removal.empty) {
            List<MonitoringJob> finishedMonitors = []
            lock.lock()
            try {
                removal.each { MonitoringJob pbsMonitor, List<PbsJobInfo> pbsInfos ->
                    List<PbsJobInfo> existing = queuedJobs.get(pbsMonitor)
                    existing.removeAll(pbsInfos)
                    finishedMonitors << pbsMonitor
                }
                if (!finishedMonitors.empty) {
                    finishedMonitors.each {
                        log.debug("Finished one MonitoringJob")
                        queuedJobs.remove(it)
                    }
                }
            } finally {
                lock.unlock()
            }
        }
    }
}
