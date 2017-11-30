package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*


@Mock([Realm])
class ClusterJobMonitoringServiceSpec extends Specification {

    ClusterJobMonitoringService clusterJobMonitoringService
    ClusterJobIdentifier clusterJob
    MonitoringJob monitoringJob

    void setup() {
        Realm realm1 = DomainFactory.createRealmDataProcessing()
        realm1.unixUser = "unx"
        clusterJob = new ClusterJobIdentifier(realm1, "JOB-ID", realm1.unixUser)
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
