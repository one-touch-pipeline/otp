package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.job.jobs.MonitoringTestJob
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.junit.Test

import static de.dkfz.tbi.TestConstants.ARBITRARY_CLUSTER_JOB_ID
import static de.dkfz.tbi.TestConstants.ARBITRARY_MESSAGE
import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.assertFailed
import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.assertSucceeded
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.createAndSaveProcessingStep

class PbsMonitorServiceTests extends AbstractIntegrationTest {

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
        final PbsJobInfo pbsJobInfo = new PbsJobInfo([realm: new Realm(), pbsId: ARBITRARY_CLUSTER_JOB_ID])
        final Job testJob = new MonitoringTestJob(createAndSaveProcessingStep(), null, schedulerService, pbsJobInfo, fail)

        testJob.log = log
        scheduler.executeJob(testJob)
        assert schedulerService.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
        pbsMonitorService.notifyJobAboutFinishedClusterJob(testJob, pbsJobInfo)
        assert schedulerService.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
        assert testJob.executed
        return testJob
    }
}
