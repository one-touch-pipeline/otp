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

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.jobs.TestMultiJob
import de.dkfz.tbi.otp.job.processing.ClusterJobSchedulerService
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm


@Mock([Realm])
class ClusterJobMonitoringServiceSpec extends Specification {

    ClusterJobMonitoringService clusterJobMonitoringService
    ClusterJobIdentifier clusterJob
    MonitoringJob monitoringJob

    void setup() {
        Realm realm1 = DomainFactory.createRealm()
        clusterJob = new ClusterJobIdentifier(realm1, "JOB-ID", "unx")
        monitoringJob = new TestMultiJob()

        clusterJobMonitoringService = Spy(ClusterJobMonitoringService)
        clusterJobMonitoringService.clusterJobService = Mock(ClusterJobService)

        clusterJobMonitoringService.queuedJobs.put(monitoringJob, [clusterJob])
    }


    void "test check, queued job found on cluster" (ClusterJobMonitoringService.Status status, boolean noneQueued, int notifyCalls) {
        setup:
        clusterJobMonitoringService.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * retrieveKnownJobsWithState(_, _) >> [(clusterJob): status]
            notifyCalls * retrieveAndSaveJobStatisticsAfterJobFinished(clusterJob) >> null
        }

        when:
        clusterJobMonitoringService.check()

        then:
        noneQueued == clusterJobMonitoringService.queuedJobs.isEmpty()
        notifyCalls * clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }

        where:
        status                                           | noneQueued | notifyCalls
        ClusterJobMonitoringService.Status.COMPLETED     | true       | 1
        ClusterJobMonitoringService.Status.NOT_COMPLETED | false      | 0
    }


    void "test check, cluster failure"() {
        setup:
        clusterJobMonitoringService.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * retrieveKnownJobsWithState(_, _) >> { throw new IllegalStateException() }
            0 * retrieveAndSaveJobStatisticsAfterJobFinished(clusterJob)
        }

        when:
        clusterJobMonitoringService.check()

        then:
        clusterJobMonitoringService.queuedJobs.containsValue([clusterJob])
        0 * clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }
    }


    void "test check, no jobs running"() {
        setup:
        clusterJobMonitoringService.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * retrieveKnownJobsWithState(_, _) >> [:]
            1 * retrieveAndSaveJobStatisticsAfterJobFinished(clusterJob) >> null
        }

        when:
        clusterJobMonitoringService.check()

        then:
        clusterJobMonitoringService.queuedJobs.isEmpty()
        1 * clusterJobMonitoringService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }
    }
}
