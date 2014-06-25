package de.dkfz.tbi.otp.job.scheduler

import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.*

import de.dkfz.tbi.otp.job.jobs.MonitoringTestJob
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import org.junit.Test

class PbsMonitorServiceTests extends AbstractIntegrationTest {

    static final String ARBITRARY_CLUSTER_JOB_ID = '5678'

    PbsMonitorService pbsMonitorService
    Scheduler scheduler
    SchedulerService schedulerService

    @Test
    void testNotifyJobAboutFinishedClusterJob_success() {
        assertSucceeded(notifyJobAboutFinishedClusterJob(false))
    }

    @Test
    void testNotifyJobAboutFinishedClusterJob_failure() {
        assertFailed(notifyJobAboutFinishedClusterJob(true), ARBITRARY_MESSAGE)
    }

    private MonitoringJob notifyJobAboutFinishedClusterJob(final boolean fail) {
        final boolean executed = false
        final PbsJobInfo pbsJobInfo = new PbsJobInfo([realm: new Realm(), pbsId: ARBITRARY_CLUSTER_JOB_ID])
        final Job testJob = new MonitoringTestJob(createTestProcessingStep(), null) {
            @Override
            void execute() throws Exception {
                assert schedulerService.jobExecutedByCurrentThread == this
                assert LogThreadLocal.threadLog == this.log
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
        }
        testJob.log = log
        scheduler.executeJob(testJob)
        assert schedulerService.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
        pbsMonitorService.notifyJobAboutFinishedClusterJob(testJob, pbsJobInfo)
        assert schedulerService.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
        assert executed
        return testJob
    }
}
