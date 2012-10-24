package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.scheduler.PbsMonitorService
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * Interface for {@link Job}s which are long running. An example is to
 * keep track of jobs running on a PBS system. The interface is needed
 * in communication with the {@link PbsMonitorService} as this service
 * invokes methods on this interface to notify the Job about state changes
 * of the tracked job on the PBS.
 *
 * A Job implementing this interface behaves different in the Scheduler.
 * When the execute() method finishes the end check is NOT performed.
 * Instead the Job is supposed to invoke the end check itself on the
 * SchedulerService.
 */
interface MonitoringJob extends EndStateAwareJob {

    /**
     * Callback to inform that the tracked PBS job finished on the Realm.
     * An implementing class should use this method to have custom code
     * to handle the finishing of one job.
     * @param pbsId The ID of the job on the PBS system
     * @param realm The Realm where the Job finished, may be {@code null} if Realm is unknown
     */
    void finished(String pbsId, Realm realm)
}
