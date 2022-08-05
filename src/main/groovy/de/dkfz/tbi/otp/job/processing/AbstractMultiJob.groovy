/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.processing

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.scheduler.*

/**
 * Base class for jobs which submit cluster jobs and wait for them to finish and optionally do other
 * things (for example validation).
 */
@Slf4j
abstract class AbstractMultiJob extends AbstractEndStateAwareJobImpl implements SometimesResumableJob, MonitoringJob {

    @Autowired
    OldClusterJobMonitor oldClusterJobMonitor

    @Autowired
    Scheduler scheduler

    @Autowired
    SchedulerService schedulerService

    final Object lockForJobCollections = new Object()

    private final Object lockForResumable = new Object()
    private boolean suspendPlanned = false
    private boolean executing = false

    @Override
    final void execute() throws Exception {
        synchronized (lockForJobCollections) {
            Collection<ClusterJob> monitoredClusterJobs = ClusterJob.findAllByProcessingStepAndValidated(processingStep, false)
            final List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(
                    processingStep, [sort: "id", order: "desc", max: 2])
            assert updates[0].state == ExecutionState.STARTED
            if (monitoredClusterJobs.empty) {
                assert updates[1].state == ExecutionState.CREATED
                assert ClusterJob.findAllByProcessingStep(processingStep).empty
                callExecute()
            } else {
                assert updates[1].state == ExecutionState.RESUMED
                log.info "The job has been resumed."
            }
        }
    }

    private void startMonitoring() {
        synchronized (lockForJobCollections) {
            Collection<ClusterJob> monitoredClusterJobs = ClusterJob.findAllByProcessingStepAndValidated(processingStep, false)
            log.info "Waiting for ${monitoredClusterJobs.size()} cluster jobs to finish: ${monitoredClusterJobs*.clusterJobId.sort()}"
            monitoredClusterJobs*.checkStatus = ClusterJob.CheckStatus.CHECKING
            monitoredClusterJobs*.save(flush: true)
        }
    }

    @Override
    void finished(final ClusterJob finishedClusterJob) {
        final boolean allFinished = ClusterJob.countByProcessingStepAndCheckStatus(processingStep, ClusterJob.CheckStatus.CHECKING) == 0

        if (allFinished) {
            final int finishedCount = ClusterJob.countByProcessingStepAndCheckStatus(processingStep, ClusterJob.CheckStatus.FINISHED)
            log.info "All ${finishedCount} cluster jobs have finished."
            /* The specification of {@link MonitoringJob#finished(String, Realm)} says that this
             * finished() method shall return quickly. So call callExecute() asynchronously.
             */
            callExecute()
        }
    }

    // suppress because it will be removed together with the old workflow system
    @SuppressWarnings("AssertWithinFinallyBlock")
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
            assert ClusterJob.findAllByProcessingStepAndCheckStatus(processingStep, ClusterJob.CheckStatus.CHECKING).empty
            Collection<ClusterJob> clusterJobs =  ClusterJob.findAllByProcessingStepAndCheckStatusAndValidated(
                    processingStep,
                    ClusterJob.CheckStatus.FINISHED,
                    false)

            final NextAction action = execute(ClusterJobIdentifier.asClusterJobIdentifierList(clusterJobs))
            ClusterJob.withTransaction {
                clusterJobs*.refresh().each { ClusterJob finishedClusterJob ->
                    assert finishedClusterJob.processingStep.id == processingStep.id
                    assert !finishedClusterJob.validated
                    finishedClusterJob.validated = true
                    assert finishedClusterJob.save(flush: true)
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
        Collection<ClusterJob> clusterJobs = ClusterJob.findAllByProcessingStepAndValidated(processingStep, false)
        switch (action) {
            case NextAction.WAIT_FOR_CLUSTER_JOBS:
                if (clusterJobs.empty) {
                    throw new OtpRuntimeException("The job requested to wait for cluster jobs, but did not submit any.")
                }
                startMonitoring()
                break
            case NextAction.SUCCEED:
                if (!clusterJobs.empty) {
                    throw new OtpRuntimeException("The job submitted cluster jobs, but requested to succeed instead of waiting for them.")
                }
                succeed()
                schedulerService.doEndCheck(this)
                break
            default:
                throw new UnsupportedOperationException("The job requested to do ${action}, which is not supported.")
        }
    }

    @Override
    void planSuspend() {
        log.info "Suspension of job for ${processingStep?.processParameterObject} is planned."
        synchronized (lockForResumable) {
            suspendPlanned = true
        }
    }

    @Override
    void cancelSuspend() {
        log.info "Suspension of job for ${processingStep?.processParameterObject} is cancelled."
        synchronized (lockForResumable) {
            suspendPlanned = false
            lockForResumable.notifyAll()
        }
    }

    @Override
    boolean isResumable() {
        synchronized (lockForResumable) {
            return !executing
        }
    }

    /**
     * Called when the job is started or when <em>all</em> of the cluster jobs which were returned
     * by the previous call of this method have finished (independent of whether they succeeded or failed).
     *
     * <p>
     * <strong>This method may be called on a different thread, with a different persistence context and on another
     * instance of the implementing class than the instance that the previous {@link #execute(Collection)} call was
     * made on.</strong> So:
     * <ul>
     *     <li>Do not share domain objects between invocations of this method.</li>
     *     <li>Do not rely on instance variables for sharing information between invocations of this method.</li>
     * </ul>
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
