package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.codehaus.groovy.grails.support.*
import spock.lang.*

class DataInstallationStartJobSpec extends IntegrationSpec {

    PersistenceContextInterceptor persistenceInterceptor

    def "execute calls setStartedForSeqTracks"() {
        given:
        Run run = DomainFactory.createRun()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(run: run)
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan(enabled: true)
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket), seqTrack: seqTrack)

        DataInstallationStartJob dataInstallationStartJob = new DataInstallationStartJob()
        dataInstallationStartJob.schedulerService = Stub(SchedulerService) {
            createProcess(_,_,_) >> null
        }
        dataInstallationStartJob.optionService = new ProcessingOptionService()

        dataInstallationStartJob.runProcessingService = Stub(RunProcessingService) {
            runReadyToInstall(_) >> run
        }
        dataInstallationStartJob.trackingService = new TrackingService()
        dataInstallationStartJob.setJobExecutionPlan(plan)
        dataInstallationStartJob.persistenceInterceptor = persistenceInterceptor

        when:
        dataInstallationStartJob.execute()

        then:
        assert otrsTicket.installationStarted != null
   }
}
