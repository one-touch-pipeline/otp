package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.codehaus.groovy.grails.support.*

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
            isActive() >> true
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

    void "test method restart"() {
        given:
        SeqTrack failedInstance = DomainFactory.createSeqTrack()
        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        FastqcStartJob fastqcStartJob = new FastqcStartJob()
        fastqcStartJob.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> { StartJob startJob, List<Parameter> input, ProcessParameter processParameterSecond ->
                Process processSecond = DomainFactory.createProcess(
                    jobExecutionPlan: failedProcess.jobExecutionPlan,
                )
                processParameterSecond.process = processSecond
                assert processParameterSecond.save(flush: true)
                return processSecond
            }
        }

        when:
        Process process = fastqcStartJob.restart(failedProcess)
        SeqTrack restartedInstance = (SeqTrack)(process.getProcessParameterObject())

        then:
        SeqTrack.list().size() == 1
        restartedInstance == failedInstance
        restartedInstance.fastqcState == SeqTrack.DataProcessingState.IN_PROGRESS
    }
}
