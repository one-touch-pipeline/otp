package de.dkfz.tbi.otp.job.jobs

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

    /** a reference to the job that is monitored */
    private ProcessingStep monitoredJob

    /** the (non-qualified) class name of the monitored job */
    private String monitoredJobClass

    /** a read-only list of cluster job IDs, for further reference */
    private List<String> allClusterJobIds

    /** the list of queued cluster jobs that are monitored */
    private List<String> queuedClusterJobIds = []

    /** a lock to prevent race conditions when modifying {@link queuedClusterJobIds} */
    private final Lock lock = new ReentrantLock()

    @PostConstruct
    private void initialize() {
        // This really should be in the constructor so fields can be marked final.
        // But our AST transformations do weird things, such as creating constructors, apparently.
        queuedClusterJobIds = getParameterValueOrClass("${JobParameterKeys.PBS_ID_LIST}").tokenize(',')
        allClusterJobIds = queuedClusterJobIds.clone().asImmutable()
        monitoredJob = this.processingStep.previous
        monitoredJobClass = monitoredJob.nonQualifiedJobClass
    }

    @Override
    void execute() throws Exception {
        pbsMonitorService.monitor(queuedClusterJobIds, this)
    }

    @Override
    void finished(String pbsId, Realm realm) {
        lock.lock()
        try {
            queuedClusterJobIds.remove(pbsId)
            log.debug "PBS job ${pbsId} finished"
        } finally {
            lock.unlock()
        }
        if (queuedClusterJobIds.empty) {
            // Since the monitoring service does not always have the correct realm (it's just "guessing"), the value
            // provided in the callback might be null. Passing the realm via job parameters is required, so we fetch
            // the value from the input parameter.
            Realm realmFromJob = Realm.findById(Long.parseLong(getParameterValueOrClass("${JobParameterKeys.REALM}")))

            String statusLogFile = jobStatusLoggingService.logFileLocation(realmFromJob, monitoredJob)
            String logFile = new File(statusLogFile).text

            // A Job can submit lots of cluster jobs, we need to check them all. Returns list of successful jobs.
            def successfulClusterJobIds = allClusterJobIds.findAll { pbsbId ->
                String expectedLogMessage = jobStatusLoggingService.constructMessage(monitoredJob, pbsbId)
                logFile.contains(expectedLogMessage)
            }

            def failedClusterJobIds = allClusterJobIds - successfulClusterJobIds

            // Output and finish
            if (failedClusterJobIds.empty) {
                log.debug 'All PBS jobs of ' + monitoredJobClass + ' were logged, calling succeed()'
                succeed()
            }
            else {
                log.error 'Some PBS jobs of ' + monitoredJobClass + ' seem to have failed: ' + failedClusterJobIds
                throw new ProcessingException("${monitoredJob} failed. PBS IDs with problems: ${failedClusterJobIds}")
            }
            schedulerService.doEndCheck(this)
        }
    }
}
