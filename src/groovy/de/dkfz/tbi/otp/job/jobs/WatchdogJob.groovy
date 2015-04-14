package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.scheduler.PbsJobInfo
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.JobStatusLoggingService
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ResumableJob
import de.dkfz.tbi.otp.job.scheduler.PbsMonitorService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * A {@link Job} that watches for PBS jobs to finish. It also checks whether the PBS job has logged a message in
 * the job status log file and fails if at least one job was not successful.
 *
 * It requires the input job parameters for PBS IDs and realms to be set.
 *
 * @see JobParameterKeys
 *
 */
@ResumableJob
class WatchdogJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {

    @Autowired JobStatusLoggingService jobStatusLoggingService
    @Autowired PbsMonitorService pbsMonitorService
    @Autowired SchedulerService schedulerService

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

    @PostConstruct
    private void initialize() {
        // This really should be in the constructor so fields can be marked final.
        // But our AST transformations do weird things, such as creating constructors, apparently.
        queuedClusterJobIds = getParameterValueOrClass("${JobParameterKeys.PBS_ID_LIST}").tokenize(',')
        allClusterJobIds = queuedClusterJobIds.clone().asImmutable()
        final ProcessingStep monitoredProcessingStep = this.processingStep.previous
        monitoredProcessingStepId = monitoredProcessingStep.id
        monitoredJobClass = monitoredProcessingStep.nonQualifiedJobClass
        realmIdFromJob = getParameterValueOrClass("${JobParameterKeys.REALM}")
    }

    private ProcessingStep getMonitoredProcessingStep() {
        return ProcessingStep.getInstance(monitoredProcessingStepId)
    }

    @Override
    void execute() throws Exception {
        if ([SKIP_WATCHDOG] == queuedClusterJobIds) {
            log.debug "Skip watchdog"
            succeed()
            schedulerService.doEndCheck(this)
        } else {
            Realm realm = CollectionUtils.exactlyOneElement(Realm.findAllById(Long.parseLong(realmIdFromJob)))
            pbsMonitorService.monitor(queuedClusterJobIds.collect { new PbsJobInfo(realm: realm, pbsId: it) }, this)
        }
    }

    @Override
    void finished(String pbsId, Realm realm) {
        final boolean allFinished
        lock.lock()
        try {
            queuedClusterJobIds.remove(pbsId)
            log.debug "PBS job ${pbsId} finished"
            allFinished = queuedClusterJobIds.empty
        } finally {
            lock.unlock()
        }
        if (allFinished) {
            ProcessingStep.withTransaction {
                def failedClusterJobs = jobStatusLoggingService.failedOrNotFinishedClusterJobs(monitoredProcessingStep, realm, allClusterJobIds)

                // Output and finish
                if (failedClusterJobs.empty) {
                    log.debug 'All PBS jobs of ' + monitoredJobClass + ' were logged, calling succeed()'
                    succeed()
                }
                else {
                    log.error 'Some PBS jobs of ' + monitoredJobClass + ' seem to have failed: ' + failedClusterJobs.collect { it.clusterJobId }
                    throw new ProcessingException("${monitoredProcessingStep} failed. PBS IDs with problems: ${failedClusterJobs.collect { it.clusterJobId }}")
                }
                schedulerService.doEndCheck(this)
            }
        }
    }
}
