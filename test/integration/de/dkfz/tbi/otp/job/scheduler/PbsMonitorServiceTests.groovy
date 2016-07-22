package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.restarting.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.logging.*
import org.apache.commons.logging.impl.*
import org.junit.*

import static de.dkfz.tbi.TestConstants.*
import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.*
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.*

class PbsMonitorServiceTests extends AbstractIntegrationTest {

    PbsMonitorService pbsMonitorService
    RestartCheckerService restartCheckerService
    Scheduler scheduler
    SchedulerService schedulerService

    @Before
    void before() {
        restartCheckerService.metaClass.canWorkflowBeRestarted = { ProcessingStep step -> false }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(RestartCheckerService, restartCheckerService)
    }


    @Test
    void testNotifyJobAboutFinishedClusterJob_success() {
        assertSucceeded(notifyJobAboutFinishedClusterJob(false))
    }

    @Test
    void testNotifyJobAboutFinishedClusterJob_failure() {
        assertFailed(notifyJobAboutFinishedClusterJob(true), ARBITRARY_MESSAGE)
    }

    private MonitoringJob notifyJobAboutFinishedClusterJob(final boolean fail) {
        Realm realm = new Realm()
        final ClusterJobIdentifier pbsJobInfo = new ClusterJobIdentifier(realm, ARBITRARY_CLUSTER_JOB_ID, "user name")
        final Job testJob = new MonitoringTestJob(createAndSaveProcessingStep(), null, schedulerService, pbsJobInfo, fail)

        testJob.log = new NoOpLog()
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
