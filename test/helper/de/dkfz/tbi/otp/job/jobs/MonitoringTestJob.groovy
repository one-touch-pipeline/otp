package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Component
@Scope("prototype")
@UseJobLog
class MonitoringTestJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {

    SchedulerService schedulerService

    ClusterJobIdentifier jobIdentifier
    boolean fail
    boolean executed = false

    MonitoringTestJob(ProcessingStep processingStep, Collection<Parameter> inputParameters, SchedulerService schedulerService, ClusterJobIdentifier jobIdentifier, boolean fail) {
        this.processingStep = processingStep
        this.schedulerService = schedulerService
        this.jobIdentifier = jobIdentifier
        this.fail = fail
    }

    @Override
    void finished(ClusterJobIdentifier finishedClusterJob) {
        assert schedulerService.jobExecutedByCurrentThread == this
        assert LogThreadLocal.threadLog == this.log
        assert finishedClusterJob.clusterJobId == TestConstants.ARBITRARY_CLUSTER_JOB_ID && finishedClusterJob.realm.is(jobIdentifier.realm)
        executed = true
        if (fail) {
            throw new NumberFormatException(TestConstants.ARBITRARY_MESSAGE)
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
