package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.codehaus.groovy.grails.support.*

class DataInstallationStartJobSpec extends IntegrationSpec {

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
