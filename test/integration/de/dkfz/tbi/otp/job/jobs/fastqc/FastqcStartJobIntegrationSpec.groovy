package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.codehaus.groovy.grails.support.*
import spock.lang.*

class FastqcStartJobIntegrationSpec extends IntegrationSpec {

    PersistenceContextInterceptor persistenceInterceptor

    def "execute calls setStartedForSeqTracks"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan(enabled: true)
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket), seqTrack: seqTrack)

        FastqcStartJob fastqcStartJob = new FastqcStartJob()
        fastqcStartJob.schedulerService = Stub(SchedulerService) {
            createProcess(_,_,_) >> null
        }
        fastqcStartJob.optionService = new ProcessingOptionService()
        fastqcStartJob.seqTrackService = Stub(SeqTrackService) {
            getSeqTrackReadyForFastqcProcessing(_) >> seqTrack
        }
        fastqcStartJob.trackingService = new TrackingService()
        fastqcStartJob.setJobExecutionPlan(plan)
        fastqcStartJob.persistenceInterceptor = persistenceInterceptor

        when:
        fastqcStartJob.execute()

        then:
        assert otrsTicket.fastqcStarted != null
   }
}
