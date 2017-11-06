package de.dkfz.tbi.otp.job.jobs.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*

class RoddyAceseqStartJobIntegrationSpec extends IntegrationSpec {

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

    private static SamplePair setupSamplePair() {
        Map map = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair = map.samplePair
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                pipeline: DomainFactory.createAceseqPipelineLazy()
        )
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                type: null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])
        return samplePair
    }

    void "findSamplePairToProcess, Sophia finished"() {
        given:
        SamplePair samplePair = setupSamplePair()
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)
        DomainFactory.createSophiaInstance(samplePair)

        expect:
        samplePair == roddyAceseqStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "findSamplePairToProcess, Sophia not started"() {
        given:
        SamplePair samplePair = setupSamplePair()
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NEEDS_PROCESSING
        samplePair.save(flush: true)

        expect:
        null == roddyAceseqStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "findSamplePairToProcess, one Sophia finished and one running"() {
        given:
        SamplePair samplePair = setupSamplePair()
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)
        DomainFactory.createSophiaInstance(samplePair)
        SophiaInstance si = DomainFactory.createSophiaInstance(samplePair)
        si.processingState = AnalysisProcessingStates.IN_PROGRESS
        si.save(flush: true)

        expect:
        null == roddyAceseqStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
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
