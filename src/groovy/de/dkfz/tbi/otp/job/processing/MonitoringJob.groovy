package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.scheduler.ClusterJobMonitoringService
import de.dkfz.tbi.otp.job.scheduler.Scheduler

/**
 * Interface for {@link Job}s which are long running. An example is to
 * keep track of jobs running on a cluster. The interface is needed
 * in communication with the {@link ClusterJobMonitoringService} as this service
 * invokes methods on this interface to notify the Job about state changes
 * of the tracked job on the cluster.
 *
 * A Job implementing this interface behaves different in the Scheduler.
 * When the execute() method finishes the end check is NOT performed.
 * Instead the Job is supposed to invoke the end check itself on the
 * SchedulerService.
 */
interface MonitoringJob extends EndStateAwareJob {

    /**
     * Callback to inform that the tracked cluster job finished on the Realm.
     *
     * <p>
     * An implementing class should use this method to have custom code
     * to handle the finishing of one cluster job.
     *
     * <p>
     * <strong>This method shall return quickly. If it triggers a time consuming operation, it shall
     * perform that operation asynchronously on a different thread by calling
     * {@link Scheduler#doOnOtherThread(Job, Closure)}.</strong>
     *
     * <p>
     * <strong>This method may be called on a different thread and with a different persistence context than the
     * {@link #execute()} method or other invocations of this method.</strong> So do not share domain objects between
     * method invocations.
     *
     * <p>
     * If this method throws an exception, this MonitoringJob will be marked as failed and finished.
     */
    void finished(ClusterJobIdentifier finishedClusterJob)
}
