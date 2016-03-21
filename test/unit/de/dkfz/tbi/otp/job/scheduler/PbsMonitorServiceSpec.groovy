package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.jobs.TestMultiJob
import de.dkfz.tbi.otp.job.processing.MonitoringJob
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.job.processing.PbsService.ClusterJobStatus
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.test.mixin.Mock
import spock.lang.Specification


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

    void "test check, queued job found on cluster"(ClusterJobStatus status, boolean noneQueued, int notifyCalls) {
        setup:
        pbsMonitorService.pbsService = [
                retrieveKnownJobsWithState: { realm, user ->
                    Map<ClusterJobIdentifier, ClusterJobStatus> result = [:]
                    result.put(clusterJob, status)
                    return result
                }
        ] as PbsService

        when:
        pbsMonitorService.check()

        then:
        noneQueued == pbsMonitorService.queuedJobs.isEmpty()
        notifyCalls * pbsMonitorService.clusterJobService.completeClusterJob(clusterJob)
        notifyCalls * pbsMonitorService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }

        where:
        status                       | noneQueued | notifyCalls
        ClusterJobStatus.COMPLETED   | true       | 1
        ClusterJobStatus.EXITED      | false      | 0
        ClusterJobStatus.HELD        | false      | 0
        ClusterJobStatus.QUEUED      | false      | 0
        ClusterJobStatus.RUNNING     | false      | 0
        ClusterJobStatus.BEING_MOVED | false      | 0
        ClusterJobStatus.WAITING     | false      | 0
        ClusterJobStatus.SUSPENDED   | false      | 0
    }


    void "test check, cluster failure"() {
        setup:
        pbsMonitorService.pbsService = [
                retrieveKnownJobsWithState: { Realm realm, user ->
                    throw new IllegalStateException()
                }
        ] as PbsService

        when:
        pbsMonitorService.check()

        then:
        pbsMonitorService.queuedJobs.containsValue([clusterJob])
        0 * pbsMonitorService.clusterJobService.completeClusterJob(clusterJob)
        0 * pbsMonitorService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }
    }


    void "test check, no jobs running"() {
        setup:
        pbsMonitorService.pbsService = [
                retrieveKnownJobsWithState: { Realm realm, user ->
                    return [:]
                }
        ] as PbsService

        when:
        pbsMonitorService.check()

        then:
        pbsMonitorService.queuedJobs.isEmpty()
        1 * pbsMonitorService.clusterJobService.completeClusterJob(clusterJob)
        1 * pbsMonitorService.notifyJobAboutFinishedClusterJob(monitoringJob, clusterJob) >> { }
    }
}
