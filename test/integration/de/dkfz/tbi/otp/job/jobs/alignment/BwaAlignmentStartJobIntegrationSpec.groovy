package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.codehaus.groovy.grails.support.*


class BwaAlignmentStartJobIntegrationSpec extends IntegrationSpec {

    PersistenceContextInterceptor persistenceInterceptor

    def "execute calls setStartedForSeqTracks"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack(
                run: DomainFactory.createRun(
                        seqPlatform: DomainFactory.createSeqPlatform(
                                seqPlatformGroup: DomainFactory.createSeqPlatformGroup())))
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass([seqTrack: seqTrack])
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


    void "test method restart"() {
        given:
        AlignmentPass failedInstance = DomainFactory.createAlignmentPass()
        DomainFactory.createRealmDataManagement(name: failedInstance.project.realmName)
        DomainFactory.createRealmDataProcessing(name: failedInstance.project.realmName)

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        BwaAlignmentStartJob bwaAlignmentStartJob = new BwaAlignmentStartJob()
        bwaAlignmentStartJob.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> { StartJob startJob, List<Parameter> input, ProcessParameter processParameter2 ->
                Process process2 = DomainFactory.createProcess(
                        jobExecutionPlan: failedProcess.jobExecutionPlan
                )
                processParameter2.process = process2
                assert processParameter2.save(flush: true)
                return process2
            }
        }

        when:
        Process process = bwaAlignmentStartJob.restart(failedProcess)
        AlignmentPass restartedInstance = (AlignmentPass)(process.getProcessParameterObject())

        then:
        AlignmentPass.list().size() == 2
        restartedInstance.workPackage == failedInstance.workPackage
        restartedInstance.seqTrack == failedInstance.seqTrack
        restartedInstance.identifier > failedInstance.identifier
        restartedInstance.alignmentState == AlignmentPass.AlignmentState.IN_PROGRESS
    }
}
