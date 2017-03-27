package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.AceseqService
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.RunSegment
import de.dkfz.tbi.otp.tracking.OtrsTicket
import grails.test.spock.IntegrationSpec
import org.springframework.beans.factory.annotation.Autowired

class RoddyAceseqStartJobIntegrationSpec extends IntegrationSpec{

    @Autowired
    RoddyAceseqStartJob roddyAceseqStartJob


    void "getConfig when config is null, throw an exception"() {
        given:
        DomainFactory.createAceseqPipelineLazy()
        SamplePair samplePair = DomainFactory.createSamplePair()

        when:
        roddyAceseqStartJob.getConfig(samplePair)

        then:
        RuntimeException e = thrown()
        e.message.contains("No ${RoddyWorkflowConfig.simpleName} found for")
    }


    void "getConfig when call fine"() {
        given:

        SamplePair samplePair = DomainFactory.createSamplePair()
        ConfigPerProject configPerProject = DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                pipeline: DomainFactory.createAceseqPipelineLazy(),
                project: samplePair.project,
        )

        expect:
        configPerProject == roddyAceseqStartJob.getConfig(samplePair)

    }


    void "findSamplePairToProcess, all fine"() {
        given:
        def map = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair = map.samplePair
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                pipeline: DomainFactory.createAceseqPipelineLazy()
        )
        DomainFactory.createProcessingOption([
                name: AceseqService.PROCESSING_OPTION_REFERENCE_KEY,
                type: null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])

        expect:
        samplePair == roddyAceseqStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }


    void "prepareCreatingTheProcessAndTriggerTracking, when input is null shall throw an exception"() {
        when:
        roddyAceseqStartJob.prepareCreatingTheProcessAndTriggerTracking(null)

        then:
        AssertionError e = thrown()
        e.message.contains("bamFilePairAnalysis must not be null")
    }

    void "prepareCreatingTheProcessAndTriggerTracking, when all fine"() {
        given:
        AceseqInstance aceseqInstance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        RunSegment.list().each {
            it.otrsTicket = otrsTicket
            it.save(flush: true)
        }
        assert otrsTicket.aceseqStarted == null
        assert aceseqInstance.samplePair.aceseqProcessingStatus != SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED

        when:
        roddyAceseqStartJob.prepareCreatingTheProcessAndTriggerTracking(aceseqInstance)

        then:
        assert otrsTicket.aceseqStarted != null
        assert aceseqInstance.samplePair.aceseqProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
    }
}
