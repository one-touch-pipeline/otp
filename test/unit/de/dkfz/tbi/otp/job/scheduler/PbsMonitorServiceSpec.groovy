package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*


@Mock([Realm])
class PbsMonitorServiceSpec extends Specification {

    PbsMonitorService pbsMonitorService
    ClusterJobIdentifier clusterJob
    MonitoringJob monitoringJob

    void setup() {
        Realm realm1 = DomainFactory.createRealmDataProcessing()
        realm1.unixUser = "unx"
        realm1.roddyUser = "rdy"
        clusterJob = new ClusterJobIdentifier(realm1, "JOB-ID", realm1.unixUser)
        monitoringJob = new TestMultiJob()

        pbsMonitorService = Spy(PbsMonitorService)
        pbsMonitorService.clusterJobService = Mock(ClusterJobService)

        pbsMonitorService.queuedJobs.put(monitoringJob, [clusterJob])
    }


    void "test check, queued job found on cluster" (PbsMonitorService.Status status, boolean noneQueued, int notifyCalls) {
        setup:
        pbsMonitorService.pbsService = Mock(PbsService) {
            1 * retrieveKnownJobsWithState(_, _) >> [(clusterJob): status]
            notifyCalls * retrieveAndSaveJobStatistics(clusterJob) >> null
        }

        when:
        pbsMonitorService.check()

        then:
        noneQueued == pbsMonitorService.queuedJobs.isEmpty()
        notifyCalls * pbsMonitorService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }

        where:
        status                                 | noneQueued | notifyCalls
        PbsMonitorService.Status.COMPLETED     | true       | 1
        PbsMonitorService.Status.NOT_COMPLETED | false      | 0
    }


    void "test check, cluster failure"() {
        setup:
        pbsMonitorService.pbsService = Mock(PbsService) {
            1 * retrieveKnownJobsWithState(_, _) >> { throw new IllegalStateException() }
            0 * retrieveAndSaveJobStatistics(clusterJob)
        }

        when:
        pbsMonitorService.check()

        then:
        pbsMonitorService.queuedJobs.containsValue([clusterJob])
        0 * pbsMonitorService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }
    }


    void "test check, no jobs running"() {
        setup:
        pbsMonitorService.pbsService = Mock(PbsService) {
            1 * retrieveKnownJobsWithState(_, _) >> [:]
            1 * retrieveAndSaveJobStatistics(clusterJob) >> null
        }

        when:
        pbsMonitorService.check()

        then:
        pbsMonitorService.queuedJobs.isEmpty()
        1 * pbsMonitorService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }
    }
}
