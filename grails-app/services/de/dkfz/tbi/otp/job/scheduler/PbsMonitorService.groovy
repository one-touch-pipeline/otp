package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.job.processing.PbsService.ClusterJobStatus
import grails.util.Pair

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

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

    ClusterJobService clusterJobService
    PbsService pbsService
    Scheduler scheduler

    /**
     * Map of currently monitored jobs on the PBS (value) ordered
     * by the MonitoringJob which registered it (key).
     */
    protected Map<MonitoringJob, List<ClusterJobIdentifier>> queuedJobs = [:]
    /**
     * Lock to protect the queuedJobs. Needed because registering a
     * Job and checking the PBS for whether a Job finished might be
     * performed in different thread.
     */
    private final Lock lock = new ReentrantLock()

    /**
     * Registers the given PBS ID for monitoring on the given realm.
     * @param info The ClusterJobIdentifier containing the PBS id of the job and the realm on which the PBS job is running
     * @param pbsMonitor The monitoring Job to notify when the job finished on the PBS
     */
    void monitor(ClusterJobIdentifier info, MonitoringJob pbsMonitor) {
        lock.lock()
        try {
            if (queuedJobs.containsKey(pbsMonitor)) {
                boolean append = true
                queuedJobs.get(pbsMonitor).each {
                    if (it == info) {
                        append = false
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
     * @param pbsJobInfos A collection of ClusterJobIdentifiers containing the PBS id of the job and the realm on which the PBS job is running
     * @param pbsMonitor The monitoring Job to notify when the job finished on the PBS
     */
    void monitor(Collection<ClusterJobIdentifier> pbsJobInfos, MonitoringJob pbsMonitor) {
        lock.lock()
        try {
            pbsJobInfos.each { ClusterJobIdentifier info ->
                monitor(info, pbsMonitor)
            }
        } finally {
            lock.unlock()
        }
    }

    public void check() {
        if (queuedJobs.isEmpty()) {
            return
        }

        // get all jobs the PBS cluster knows about
        // if a cluster from a realm is not reachable, save it to a list so we don't assume that jobs on this realm are completed
        Map<ClusterJobIdentifier, ClusterJobStatus> jobStates = [:]
        List<Pair<String, String>> failedClusterQueries = []

        queuedJobs.values().sum().unique { a, b -> a.realmId == b.realmId && a.userName == b.userName ? 0 : 1 }.each { ClusterJobIdentifier job ->
            try {
                jobStates.putAll(pbsService.knownJobsWithState(job.realm, job.userName))
            } catch (Throwable e) {
                failedClusterQueries.add(new Pair(job.realm, job.userName))
            }
        }

        Map<MonitoringJob, List<ClusterJobIdentifier>> removal = [:]
        // we create a copy of the queuedJobs as a different thread might append elements to
        // queuedJobs which might end up in concurrency issues
        (new HashMap<MonitoringJob, List<ClusterJobIdentifier>>(queuedJobs)).each { MonitoringJob pbsMonitor, List<ClusterJobIdentifier> pbsInfos ->
            // for each of the queuedJobs we go over the pbsInfos and check whether the job on the
            // PBS systems is still running
            List<ClusterJobIdentifier> finishedJobs = []
            // again a copy for thread safety
            (new ArrayList<ClusterJobIdentifier>(pbsInfos)).each { ClusterJobIdentifier info ->
                log.debug("Checking pbs id ${info.clusterJobId}")
                boolean completed
                // a job is considered complete if it either has status "completed" or it is not known anymore,
                // unless the cluster it runs on couldn't be checked
                ClusterJobStatus status = jobStates.getOrDefault(info, ClusterJobStatus.COMPLETED)
                completed = (status == ClusterJobStatus.COMPLETED && !failedClusterQueries.contains(new Pair(info.realm, info.userName)))
                log.debug("${info.clusterJobId} still running: ${completed ? 'no' : 'yes'}")
                if (completed) {
                    log.info("${info.clusterJobId} finished on Realm ${info.realm}")
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
                removal.each { MonitoringJob pbsMonitor, List<ClusterJobIdentifier> pbsInfos ->
                    // we get the list of PbsJobs for the monitor
                    // and remove the finished pbsJob Info
                    List<ClusterJobIdentifier> existing = queuedJobs.get(pbsMonitor)
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

    protected void notifyJobAboutFinishedClusterJob(final MonitoringJob monitoringJob, final ClusterJobIdentifier clusterJob) {
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
                    log.info("NOT notifying ${monitoringJob} that cluster job ${clusterJob.clusterJobId}" +
                            " has finished on realm ${clusterJob.realm}, because that job has already failed.")
                } else {
                    throw new RuntimeException("${monitoringJob} is still monitoring cluster job" +
                            " ${clusterJob.clusterJobId} on realm ${clusterJob.realm}, although it has" +
                            " already finished with end state ${jobEndState}.")
                }
            } else {
                log.info("Notifying ${monitoringJob} that cluster job ${clusterJob.clusterJobId}" +
                        " has finished on realm ${clusterJob.realm}.")
                scheduler.doInJobContext(monitoringJob, {
                    monitoringJob.finished(clusterJob)
                })
            }
        }, false)
    }
}
