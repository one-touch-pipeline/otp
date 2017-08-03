package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*

import java.util.concurrent.locks.*


/**
 * This service is able to track the execution of jobs on the cluster job scheduler.
 * It is used by {@link MonitoringJob}s which need to know when a
 * job finished on the cluster. These MonitoringJobs register a job for watching.
 *
 * The service performs a scheduled checking for all registered jobs and
 * notifies the MonitoringJobs which registered one job whenever the job
 * finished.
 */
class PbsMonitorService {
    enum Status {
        COMPLETED, NOT_COMPLETED
    }


    static transactional = false

    ClusterJobService clusterJobService
    PbsService pbsService
    Scheduler scheduler

    /**
     * Map of currently monitored jobs on the cluster (value) ordered
     * by the MonitoringJob which registered it (key).
     */
    protected Map<MonitoringJob, List<ClusterJobIdentifier>> queuedJobs = [:]
    /**
     * Lock to protect the queuedJobs. Needed because registering a
     * Job and checking the cluster job scheduler for whether a Job finished might be
     * performed in different thread.
     */
    private final Lock lock = new ReentrantLock()

    /**
     * Registers the given job ID for monitoring on the given realm.
     * @param jobIdentifier The ClusterJobIdentifier containing the cluster job id and the realm on which the job is running
     * @param monitoringJob The monitoring Job to notify when the job finished on the cluster job scheduler
     */
    void monitor(ClusterJobIdentifier jobIdentifier, MonitoringJob monitoringJob) {
        lock.lock()
        try {
            if (queuedJobs.containsKey(monitoringJob)) {
                boolean append = true
                queuedJobs.get(monitoringJob).each {
                    if (it == jobIdentifier) {
                        append = false
                    }
                }
                if (append) {
                    log.debug("Adding job for already registered MonitoringJob")
                    queuedJobs.get(monitoringJob).add(jobIdentifier)
                }
            } else {
                log.debug("Adding one MonitoringJob")
                queuedJobs.put(monitoringJob, [jobIdentifier])
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Registers the given job IDs for monitoring.
     * @param jobIdentifiers A collection of ClusterJobIdentifiers containing the cluster job id and the realm on which the job is running
     * @param monitoringJob The monitoring Job to notify when the job finished on the cluster
     */
    void monitor(Collection<ClusterJobIdentifier> jobIdentifiers, MonitoringJob monitoringJob) {
        lock.lock()
        try {
            jobIdentifiers.each { ClusterJobIdentifier info ->
                monitor(info, monitoringJob)
            }
        } finally {
            lock.unlock()
        }
    }

    public void check() {
        if (queuedJobs.isEmpty()) {
            return
        }

        // we create a copy of the queuedJobs as a different thread might append elements to
        // queuedJobs which might end up in concurrency issues
        Map<MonitoringJob, List<ClusterJobIdentifier>> copyQueuedJobs
        lock.lock()
        try {
           copyQueuedJobs = new HashMap<MonitoringJob, List<ClusterJobIdentifier>>(queuedJobs)
        } finally {
            lock.unlock()
        }

        // get all jobs the cluster job scheduler knows about
        // if a cluster from a realm is not reachable, save it to a list so we don't assume that jobs on this realm are completed
        Map<ClusterJobIdentifier, Status> jobStates = [:]
        List<RealmAndUser> failedClusterQueries = []

        copyQueuedJobs.values().flatten().unique { a, b -> a.realmId == b.realmId && a.userName == b.userName ? 0 : 1 }.each { ClusterJobIdentifier job ->
            try {
                jobStates.putAll(pbsService.retrieveKnownJobsWithState(job.realm, job.userName))
            } catch (Throwable e) {
                log.error("Retrieving job states for ${job.realm} user ${job.userName} failed:", e)
                failedClusterQueries.add(new RealmAndUser(job.realm, job.userName))
            }
        }

        Map<MonitoringJob, List<ClusterJobIdentifier>> removal = [:]
        copyQueuedJobs.each { MonitoringJob monitoringJob, List<ClusterJobIdentifier> jobIdentifiers ->
            // for each of the queuedJobs we go over the jobIdentifiers and check whether the job
            // is still running on the cluster
            List<ClusterJobIdentifier> finishedJobs = []
            // again a copy for thread safety
            (new ArrayList<ClusterJobIdentifier>(jobIdentifiers)).each { ClusterJobIdentifier jobIdentifier ->
                log.debug("Checking job id ${jobIdentifier.clusterJobId}")
                boolean completed
                // a job is considered complete if it either has status "completed" or it is not known anymore,
                // unless the cluster it runs on couldn't be checked
                Status status = jobStates.getOrDefault(jobIdentifier, Status.COMPLETED)
                completed = (status == Status.COMPLETED && !failedClusterQueries.contains(new RealmAndUser(jobIdentifier.realm, jobIdentifier.userName)))
                log.debug("${jobIdentifier.clusterJobId} still running: ${completed ? 'no' : 'yes'}")
                if (completed) {
                    log.info("${jobIdentifier.clusterJobId} finished on Realm ${jobIdentifier.realm}")
                    try {
                        pbsService.retrieveAndSaveJobStatistics(jobIdentifier)
                    } catch (Throwable e) {
                        log.warn("Failed to fill in runtime statistics for ${jobIdentifier}", e)
                    }
                    notifyJobAboutFinishedClusterJob(monitoringJob, jobIdentifier)
                    finishedJobs.add(jobIdentifier)
                }
            }
            if (!finishedJobs.empty) {
                // we put the finished jobs into a temporary Map of the Monitors and List of finished Jobs
                // same data structure as the queuedJobs
                removal.put(monitoringJob, finishedJobs)
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
                removal.each { MonitoringJob monitoringJob, List<ClusterJobIdentifier> jobIdentifiers ->
                    // we get the list of job identifiers for the monitoring job
                    // and remove the finished job identifiers
                    List<ClusterJobIdentifier> existing = queuedJobs.get(monitoringJob)
                    existing.removeAll(jobIdentifiers)
                    if (existing.empty) {
                        // in case the list is empty after removal of the job identifiers
                        // the monitoring job has served it's purpose and can be scheduled for
                        // complete removal
                        finishedMonitors << monitoringJob
                    }
                }
                if (!finishedMonitors.empty) {
                    // some Monitors don't have any jobs to monitor, so they can be removed from the map
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
