package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.utils.logging.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.TestConstants.*

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
