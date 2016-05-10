package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.codehaus.groovy.grails.support.*
import spock.lang.*

class BwaAlignmentStartJobSpec extends IntegrationSpec {

    PersistenceContextInterceptor persistenceInterceptor

    def "execute calls setStartedForSeqTracks"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqPlatform: DomainFactory.createSeqPlatform(seqPlatformGroup: DomainFactory.createSeqPlatformGroup()))
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        AlignmentPass alignmentPass = TestData.createAndSaveAlignmentPass([seqTrack: seqTrack])
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan(enabled: true)
        DomainFactory.createDataFile(runSegment: DomainFactory.createRunSegment(otrsTicket: otrsTicket), seqTrack: seqTrack)

        BwaAlignmentStartJob bwaAlignmentStartJob = new BwaAlignmentStartJob()
        bwaAlignmentStartJob.schedulerService = Stub(SchedulerService) {
            createProcess(_,_,_) >> null
        }
        bwaAlignmentStartJob.optionService = new ProcessingOptionService()
        bwaAlignmentStartJob.alignmentPassService = Stub(AlignmentPassService) {
            findAlignmentPassForProcessing(_) >> alignmentPass
        }
        bwaAlignmentStartJob.trackingService = new TrackingService()
        bwaAlignmentStartJob.setJobExecutionPlan(plan)
        bwaAlignmentStartJob.persistenceInterceptor = persistenceInterceptor

        when:
        bwaAlignmentStartJob.execute()

        then:
        assert otrsTicket.alignmentStarted != null
   }
}
