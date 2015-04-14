package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.infrastructure.ClusterJobService

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.util.Environment

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

    @Autowired
    ClusterJobService clusterJobService

    @Autowired
    ExecutionService executionService

    @Autowired
    Scheduler scheduler

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
     * Registers the given PBS ID for monitoring on the given realm.
     * @param info The PbsJobInfo containing the PBS id of the job and the realm on which the PBS job is running
     * @param pbsMonitor The monitoring Job to notify when the job finished on the PBS
     */
    void monitor(PbsJobInfo info, MonitoringJob pbsMonitor) {
        lock.lock()
        try {
            if (queuedJobs.containsKey(pbsMonitor)) {
                boolean append = true
                queuedJobs.get(pbsMonitor).each {
                    if (it.pbsId == info.pbsId) {
                        if (it.realm == info.realm || !it.realm) {
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
     * @param pbsJobInfos A collection of PbsJobInfos containing the PBS id of the job and the realm on which the PBS job is running
     * @param pbsMonitor The monitoring Job to notify when the job finished on the PBS
     */
    void monitor(Collection<PbsJobInfo> pbsJobInfos, MonitoringJob pbsMonitor) {
        lock.lock()
        try {
            pbsJobInfos.each { PbsJobInfo info ->
                monitor(info, pbsMonitor)
            }
        } finally {
            lock.unlock()
        }
    }

    public void check() {
        if (queuedJobs.empty) {
            return
        }

        List<Realm> realms = Realm.findAllByEnv(Environment.current.name)

        Map<MonitoringJob, List<PbsJobInfo>> removal = [:]
        // we create a copy of the queuedJobs as a different thread might append elements to
        // queuedJobs which might end up in concurrency issues
        (new HashMap(queuedJobs)).each { MonitoringJob pbsMonitor, List<PbsJobInfo> pbsInfos ->
            // for each of the queuedJobs we go over the pbsInfos and check whether the job on the
            // PBS systems is still running
            List<PbsJobInfo> finishedJobs = []
            // again a copy for thread safety
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
                        clusterJobService.completeClusterJob(info)
                    } catch (Throwable e) {
                        log.warn("Failed to fill in runtime statistics for ${info}", e)
                    }
                    notifyJobAboutFinishedClusterJob(pbsMonitor, info)
                    finishedJobs.add(info)
                }
            }
            if (!finishedJobs.empty) {
                // we put the finished jobs into a temporary Map of the Monitors and List of finished Jobs
                // same data structure as the queuedJobs
                removal.put(pbsMonitor, finishedJobs)
            }
        }

        if (!removal.empty) {
            // in case that some Jobs finished we need to remove them from our queue
            // for thread safety reasons we have operated on a copy of the list, so we
            // need to transfer it on the real object
            // this needs to be thread safe (other threads might add new elements),
            // therefore it is in a locked area
            List<MonitoringJob> finishedMonitors = []
            lock.lock()
            try {
                removal.each { MonitoringJob pbsMonitor, List<PbsJobInfo> pbsInfos ->
                    // we get the list of PbsJobs for the monitor
                    // and remove the finished pbsJob Info
                    List<PbsJobInfo> existing = queuedJobs.get(pbsMonitor)
                    existing.removeAll(pbsInfos)
                    if (existing.empty) {
                        // in case the list is after removal of the pbsJob Info empty
                        // the Monitor has served it's purpose and can be scheduled for
                        // complete removal
                        finishedMonitors << pbsMonitor
                    }
                }
                if (!finishedMonitors.empty) {
                    // some Monitors don't have any pbsJobs to monitor, so they can be removed
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

    private void notifyJobAboutFinishedClusterJob(final MonitoringJob monitoringJob, final PbsJobInfo clusterJob) {
        scheduler.doWithErrorHandling(monitoringJob, {
            boolean jobHasFinished
            ExecutionState jobEndState
            try {
                jobEndState = monitoringJob.getEndState()
                jobHasFinished = true
            } catch (final InvalidStateException e) {
                // MonitoringJob.getEndState() is specified to throw an InvalidStateException if the
                // job has not finished yet.
                jobHasFinished = false
            }
            if (jobHasFinished) {
                if (jobEndState == ExecutionState.FAILURE) {
                    log.info("NOT notifying ${monitoringJob} that cluster job ${clusterJob.pbsId}" +
                            " has finished on realm ${clusterJob.realm}, because that job has already failed.")
                } else {
                    throw new RuntimeException("${monitoringJob} is still monitoring cluster job" +
                            " ${clusterJob.pbsId} on realm ${clusterJob.realm}, although it has" +
                            " already finished with end state ${jobEndState}.")
                }
            } else {
                log.info("Notifying ${monitoringJob} that cluster job ${clusterJob.pbsId}" +
                        " has finished on realm ${clusterJob.realm}.")
                scheduler.doInJobContext(monitoringJob, {
                    monitoringJob.finished(clusterJob.pbsId, clusterJob.realm)
                })
            }
        }, false)
    }
}
