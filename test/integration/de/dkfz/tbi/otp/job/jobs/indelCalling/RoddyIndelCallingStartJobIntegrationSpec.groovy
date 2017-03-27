package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import grails.test.spock.*
import org.springframework.beans.factory.annotation.Autowired

class RoddyIndelCallingStartJobIntegrationSpec extends IntegrationSpec {

    @Autowired
    RoddyIndelCallingStartJob roddyIndelCallingStartJob

    void setup() {
        DomainFactory.createIndelSeqTypes()
    }

    void "getConfig when config is null, throw an exception"() {
        given:
        DomainFactory.createIndelPipelineLazy()
        SamplePair samplePair = DomainFactory.createSamplePair()

        when:
        roddyIndelCallingStartJob.getConfig(samplePair)

        then:
        RuntimeException e = thrown()
        e.message.contains("No ${RoddyWorkflowConfig.simpleName} found for")
    }


    void "getConfig when call fine"() {
        given:

        SamplePair samplePair = DomainFactory.createSamplePair()
        ConfigPerProject configPerProject = DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                pipeline: DomainFactory.createIndelPipelineLazy(),
                project: samplePair.project,
        )

        expect:
        configPerProject == roddyIndelCallingStartJob.getConfig(samplePair)

    }


    void "findSamplePairToProcess, all fine"() {
        given:
        def map = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair = map.samplePair
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                pipeline: DomainFactory.createIndelPipelineLazy()
        )

        expect:
        samplePair == roddyIndelCallingStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }


    void "prepareCreatingTheProcessAndTriggerTracking, when input is null shall throw an exception"() {
        when:
        roddyIndelCallingStartJob.prepareCreatingTheProcessAndTriggerTracking(null)

        then:
        AssertionError e = thrown()
        e.message.contains("bamFilePairAnalysis must not be null")
    }

    void "prepareCreatingTheProcessAndTriggerTracking, when all fine"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        RunSegment.list().each {
            it.otrsTicket = otrsTicket
            it.save(flush: true)
        }
        assert otrsTicket.indelStarted == null
        assert indelCallingInstance.samplePair.indelProcessingStatus != SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED

        when:
        roddyIndelCallingStartJob.prepareCreatingTheProcessAndTriggerTracking(indelCallingInstance)

        then:
        assert otrsTicket.indelStarted != null
        assert indelCallingInstance.samplePair.indelProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
    }
}
