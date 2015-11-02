package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.scheduler.PbsJobInfo
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static de.dkfz.tbi.TestConstants.ARBITRARY_CLUSTER_JOB_ID
import static de.dkfz.tbi.TestConstants.ARBITRARY_MESSAGE

class MonitoringTestJob extends AbstractEndStateAwareJobImpl implements MonitoringJob {

    SchedulerService schedulerService

    PbsJobInfo pbsJobInfo
    boolean fail
    boolean executed = false

    MonitoringTestJob(ProcessingStep processingStep, Collection<Parameter> inputParameters, SchedulerService schedulerService, PbsJobInfo pbsJobInfo, boolean fail) {
        super(processingStep, inputParameters)
        this.schedulerService = schedulerService
        this.pbsJobInfo = pbsJobInfo
        this.fail = fail
    }

    @Override
    void finished(String pbsId, Realm realm) {
        assert schedulerService.jobExecutedByCurrentThread == this
        assert LogThreadLocal.threadLog == this.log
        assert pbsId == ARBITRARY_CLUSTER_JOB_ID && realm.is(pbsJobInfo.realm)
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
