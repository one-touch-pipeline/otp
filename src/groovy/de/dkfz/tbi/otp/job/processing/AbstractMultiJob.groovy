package de.dkfz.tbi.otp.job.processing

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.scheduler.PbsMonitorService
import de.dkfz.tbi.otp.job.scheduler.Scheduler
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * Base class for jobs which submit cluster jobs and wait for them to finish and optionally do other
 * things (for example validation).
 *
 */
public abstract class AbstractMultiJob extends AbstractEndStateAwareJobImpl implements SometimesResumableJob, MonitoringJob {

    enum NextAction {
        /**
         * Finish the execution of this job and mark it as succeeded.
         */
        SUCCEED,
        /**
         * Wait for the submitted cluster jobs to finish and then notify this job.
         */
        WAIT_FOR_CLUSTER_JOBS,
    }

    @Autowired
    PbsMonitorService pbsMonitorService
    @Autowired
    Scheduler scheduler
    @Autowired
    SchedulerService schedulerService

    final Object lockForJobCollections = new Object()
    Collection<ClusterJob> monitoredClusterJobs = null
    Collection<ClusterJob> finishedClusterJobs = null

    private final Object lockForResumable = new Object()
    private boolean suspendPlanned = false
    private boolean executing = false

    @Override
    public final void execute() throws Exception {
        synchronized (lockForJobCollections) {
            assert monitoredClusterJobs == null
            assert finishedClusterJobs == null
            monitoredClusterJobs = ClusterJob.findAllByProcessingStepAndValidated(processingStep, false)
            final List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(
                    processingStep, [sort: "id", order: "desc", max: 2])
            assert updates[0].state == ExecutionState.STARTED
            if (monitoredClusterJobs.empty) {
                assert updates[1].state == ExecutionState.CREATED
                callExecute()
            } else {
                assert updates[1].state == ExecutionState.RESUMED
                log.info "The job has been resumed."
                finishedClusterJobs = []
                startMonitoring()
            }
        }
    }

    private void startMonitoring() {
        synchronized (lockForJobCollections) {
            log.info "Waiting for ${monitoredClusterJobs.size()} cluster jobs to finish: ${monitoredClusterJobs}"
            monitoredClusterJobs.each {
                pbsMonitorService.monitor(it.clusterJobId, it.realm, this)
            }
        }
    }

    @Override
    public void finished(final String clusterJobId, final Realm realm) {
        final boolean allFinished
        final int finishedCount
        synchronized (lockForJobCollections) {
            final ClusterJob finishedClusterJob = monitoredClusterJobs.find { it.clusterJobId == clusterJobId && it.realm == realm }
            if (finishedClusterJob) {
                monitoredClusterJobs.remove(finishedClusterJob)
                finishedClusterJobs << finishedClusterJob
            } else {
                throw new RuntimeException("Received a notification that cluster job ${clusterJobId}" +
                        " on realm ${realm} has finished although we are not monitoring that cluster job.")
            }
            allFinished = monitoredClusterJobs.empty
            finishedCount = finishedClusterJobs.size()
        }
        if (allFinished) {
            log.info "All ${finishedCount} cluster jobs have finished."
            /* The specification of {@link MonitoringJob#finished(String, Realm)} says that this
             * finished() method shall return quickly. So call callExecute() asynchronously.
             */
            // if (OTP-1024 is resolved) {
            //     TODO: scheduler.doOnOtherThread(this, { callExecute() } )
            // } else {
            //     // Workaround: Hope. Hope that this job does not perform any time-consuming operation.
                   callExecute()
            // }
        }
    }

    private void callExecute() {
        synchronized (lockForResumable) {
            boolean waited = false
            while (suspendPlanned) {
                if (!waited) {
                    log.debug "Not executing further because suspension is planned."
                    waited = true
                }
                lockForResumable.wait()
            }
            if (waited) {
                log.debug "Executing further because suspension has been cancelled."
            }
            assert !executing
            executing = true
        }
        try {
            synchronized (lockForJobCollections) {
                assert monitoredClusterJobs.empty
                assert finishedClusterJobs == null || !finishedClusterJobs.empty
                finishedClusterJobs = finishedClusterJobs?.asImmutable()
            }
            final NextAction action = execute(finishedClusterJobs)
            ClusterJob.withTransaction {
                finishedClusterJobs.each {
                    final ClusterJob finishedClusterJob = ClusterJob.get(it.id)
                    assert !finishedClusterJob.validated
                    finishedClusterJob.validated = true
                    assert finishedClusterJob.save(flush: true)
                }
                final Collection<ClusterJob> submittedClusterJobs = ClusterJob.findAllByProcessingStepAndValidated(processingStep, false)
                synchronized (lockForJobCollections) {
                    monitoredClusterJobs = submittedClusterJobs
                    finishedClusterJobs = []
                }
                performAction(action)
            }
        } finally {
            synchronized (lockForResumable) {
                assert executing
                executing = false
            }
        }
    }

    private void performAction(final NextAction action) {
        switch (action) {
            case NextAction.WAIT_FOR_CLUSTER_JOBS:
                if (monitoredClusterJobs.empty) {
                    throw new RuntimeException("The job requested to wait for cluster jobs, but did not submit any.")
                }
                startMonitoring()
                break
            case NextAction.SUCCEED:
                if (!monitoredClusterJobs.empty) {
                    throw new RuntimeException("The job submitted cluster jobs, but requested to succeed instead of waiting for them.")
                }
                succeed()
                schedulerService.doEndCheck(this)
                break
            default:
                throw new UnsupportedOperationException("The job requested to do ${action}, which is not supported.")
        }
    }

    @Override
    public void planSuspend() {
        log.info "Suspension of this job is planned."
        synchronized (lockForResumable) {
            suspendPlanned = true
        }
    }

    @Override
    public void cancelSuspend() {
        log.info "Suspension of this job is cancelled."
        synchronized (lockForResumable) {
            suspendPlanned = false
            lockForResumable.notifyAll()
        }
    }

    @Override
    public boolean isResumable() {
        synchronized (lockForResumable) {
            return !executing
        }
    }

    /**
     * Called when the job is started or when <em>all</em> of the cluster jobs which were returned
     * by the previous call of this method have finished (independent of whether they succeeded or failed).
     *
     * <p>
     * <strong>This method might be called on a <em>different</em> instance of the implementing
     * class than the instance that the previous {@link #execute(Collection)} call was made on. So
     * you cannot rely on instance variables for transferring information between the method
     * calls.</strong>
     *
     * <p>
     * <strong>TODO: As long as OTP-1026 is not resolved, this method must return quickly if the finishedClusterJobs
     * argument is not null.</strong>
     *
     * @param finishedClusterJobs <code>null</code> if the job has just been started, i.e. the
     * method is called the first time for the job. Otherwise a collection of the cluster
     * jobs that have finished. This collection will contain at least one element.
     *
     * @return What should be done next. If cluster jobs were submitted during the execution of this method, the method
     * must return {@link NextAction#WAIT_FOR_CLUSTER_JOBS}, otherwise it must return {@link NextAction#SUCCEED}.
     * In case of {@link NextAction#WAIT_FOR_CLUSTER_JOBS}, the job system will notify this job about the cluster jobs
     * having finished by calling this method again.
     *
     * @throws Throwable Throwing will make this job fail.
     */
    protected abstract NextAction execute(Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable
}
