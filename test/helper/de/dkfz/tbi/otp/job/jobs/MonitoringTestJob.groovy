package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static de.dkfz.tbi.TestConstants.ARBITRARY_CLUSTER_JOB_ID
import static de.dkfz.tbi.TestConstants.ARBITRARY_MESSAGE

class MonitoringTestJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {

    SchedulerService schedulerService

    ClusterJobIdentifier jobIdentifier
    boolean fail
    boolean executed = false

    MonitoringTestJob(ProcessingStep processingStep, Collection<Parameter> inputParameters, SchedulerService schedulerService, ClusterJobIdentifier jobIdentifier, boolean fail) {
        super(processingStep, inputParameters)
        this.schedulerService = schedulerService
        this.jobIdentifier = jobIdentifier
        this.fail = fail
    }

    @Override
    void finished(ClusterJobIdentifier finishedClusterJob) {
        assert schedulerService.jobExecutedByCurrentThread == this
        assert LogThreadLocal.threadLog == this.log
        assert finishedClusterJob.clusterJobId == ARBITRARY_CLUSTER_JOB_ID && finishedClusterJob.realm.is(jobIdentifier.realm)
        executed = true
        if (fail) {
            throw new NumberFormatException(ARBITRARY_MESSAGE)
        }
        succeed()
        schedulerService.doEndCheck(this)
    }

    @Override
    void execute() throws Exception {
        assert schedulerService.jobExecutedByCurrentThread == this
        assert LogThreadLocal.threadLog == this.log
    }
}
