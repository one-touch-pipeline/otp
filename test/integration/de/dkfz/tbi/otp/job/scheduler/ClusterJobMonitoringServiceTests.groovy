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

package de.dkfz.tbi.otp.job.scheduler

import org.apache.commons.logging.impl.NoOpLog
import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.integration.AbstractIntegrationTest
import de.dkfz.tbi.otp.job.jobs.MonitoringTestJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.restarting.RestartCheckerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.assertFailed
import static de.dkfz.tbi.otp.job.scheduler.SchedulerTests.assertSucceeded
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.createAndSaveProcessingStep

class ClusterJobMonitoringServiceTests extends AbstractIntegrationTest {

    ClusterJobMonitoringService clusterJobMonitoringService
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
        assertFailed(notifyJobAboutFinishedClusterJob(true), TestConstants.ARBITRARY_MESSAGE)
    }

    private MonitoringJob notifyJobAboutFinishedClusterJob(final boolean fail) {
        Realm realm = new Realm()
        final ClusterJobIdentifier jobIdentifier = new ClusterJobIdentifier(realm, TestConstants.ARBITRARY_CLUSTER_JOB_ID, "user name")
        final Job testJob = new MonitoringTestJob(createAndSaveProcessingStep(), null, schedulerService, jobIdentifier, fail)

        testJob.log = new NoOpLog()
        scheduler.executeJob(testJob)
        assert schedulerService.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
        clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(testJob, jobIdentifier)
        assert schedulerService.jobExecutedByCurrentThread == null
        assert LogThreadLocal.threadLog == null
        assert testJob.executed
        return testJob
    }
}
