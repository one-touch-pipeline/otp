package de.dkfz.tbi.otp.job.jobs.dataInstallation

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor

import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.TrackingService

class DataInstallationStartJobIntegrationSpec extends IntegrationSpec {

    PersistenceContextInterceptor persistenceInterceptor

    def "execute calls setStartedForSeqTracks"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan(enabled: true)
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket), seqTrack: seqTrack)

        DataInstallationStartJob dataInstallationStartJob = new DataInstallationStartJob()
        dataInstallationStartJob.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> null
            _ * isActive() >> true
        }
        dataInstallationStartJob.optionService = new ProcessingOptionService()

        dataInstallationStartJob.trackingService = new TrackingService()
        dataInstallationStartJob.setJobExecutionPlan(plan)
        dataInstallationStartJob.persistenceInterceptor = persistenceInterceptor
        dataInstallationStartJob.seqTrackService = new SeqTrackService()

        when:
        dataInstallationStartJob.execute()

        then:
        otrsTicket.installationStarted != null
        seqTrack.dataInstallationState == SeqTrack.DataProcessingState.IN_PROGRESS
    }

}
