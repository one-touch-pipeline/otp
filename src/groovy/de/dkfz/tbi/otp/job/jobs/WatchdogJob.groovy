package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
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
 * A {@link Job} that watches for cluster jobs to finish. It also checks whether the job has logged a message in
 * the job status log file and fails if at least one job was not successful.
 *
 * It requires the input job parameters for cluster job IDs and realms to be set.
 *
 * @see JobParameterKeys
 *
 *
 * @deprecated Do not use a separate watchdog job.
 * Instead create/use a subclass of {@link AbstractMultiJob}, so restarting the job will resubmit the cluster jobs.
 */
@Component
@Scope("prototype")
@Deprecated
@ResumableJob
@UseJobLog
class WatchdogJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {

    @Autowired ClusterJobMonitoringService clusterJobMonitoringService
    @Autowired SchedulerService schedulerService
    @Autowired ConfigService configService

    /**
     * Constant to indicate that no cluster job has executed such that the watchdog should not wait.
     */
    static final SKIP_WATCHDOG = "SKIP_WATCHDOG"

    // See comment on AbstractJobImpl.processingStepId for explanation why we store the ID instead of a reference to the
    // ProcessingStep instance here.
    /** ID of the processing step that is monitored */
    private long monitoredProcessingStepId

    /** the (non-qualified) class name of the monitored job */
    private String monitoredJobClass

    /** a read-only list of cluster job IDs, for further reference */
    private List<String> allClusterJobIds

    /** the list of queued cluster jobs that are monitored */
    private List<String> queuedClusterJobIds = []

    /** a lock to prevent race conditions when modifying {@link #queuedClusterJobIds} */
    private final Lock lock = new ReentrantLock()

    String realmIdFromJob

    private ProcessingStep getMonitoredProcessingStep() {
        return ProcessingStep.getInstance(monitoredProcessingStepId)
    }

    @Override
    void execute() throws Exception {
        queuedClusterJobIds = getParameterValueOrClass(JobParameterKeys.JOB_ID_LIST).tokenize(',')
        allClusterJobIds = queuedClusterJobIds.clone().asImmutable()
        final ProcessingStep monitoredProcessingStep = this.processingStep.previous
        monitoredProcessingStepId = monitoredProcessingStep.id
        monitoredJobClass = monitoredProcessingStep.nonQualifiedJobClass
        realmIdFromJob = getParameterValueOrClass(JobParameterKeys.REALM)
        if ([SKIP_WATCHDOG] == queuedClusterJobIds) {
            log.debug "Skip watchdog"
            succeed()
            schedulerService.doEndCheck(this)
        } else {
            Realm realm = CollectionUtils.exactlyOneElement(Realm.findAllById(Long.parseLong(realmIdFromJob)))
            clusterJobMonitoringService.monitor(queuedClusterJobIds.collect { new ClusterJobIdentifier(realm, it, configService.getSshUser()) }, this)
        }
    }

    @Override
    void finished(ClusterJobIdentifier finishedClusterJob) {
        final boolean allFinished
        lock.lock()
        try {
            queuedClusterJobIds.remove(finishedClusterJob.clusterJobId)
            log.debug "Cluster job ${finishedClusterJob.clusterJobId} finished"
            allFinished = queuedClusterJobIds.empty
        } finally {
            lock.unlock()
        }
        if (allFinished) {
            ProcessingStep.withTransaction {
                List<ClusterJobIdentifier> allClusterJobs = allClusterJobIds.collect { new ClusterJobIdentifier(finishedClusterJob.realm, it, finishedClusterJob.userName) }

                def failedClusterJobs = jobStatusLoggingService.failedOrNotFinishedClusterJobs(monitoredProcessingStep, allClusterJobs)

                // Output and finish
                if (failedClusterJobs.empty) {
                    log.debug 'All cluster jobs of ' + monitoredJobClass + ' were logged, calling succeed()'
                    succeed()
                }
                else {
                    log.error 'Some cluster jobs of ' + monitoredJobClass + ' seem to have failed: ' + failedClusterJobs.collect { it.clusterJobId }
                    throw new ProcessingException("${monitoredProcessingStep} failed. Job IDs with problems: ${failedClusterJobs.collect { it.clusterJobId }}")
                }
                schedulerService.doEndCheck(this)
            }
        }
    }
}
