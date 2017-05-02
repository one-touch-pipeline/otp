package de.dkfz.tbi.otp.job.jobs.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*

class RoddySophiaStartJobIntegrationSpec extends IntegrationSpec{

    @Autowired
    RoddySophiaStartJob roddySophiaStartJob


    void "getConfig when config is null, throw an exception"() {
        given:
        DomainFactory.createSophiaPipelineLazy()
        SamplePair samplePair = DomainFactory.createSamplePair()

        when:
        roddySophiaStartJob.getConfig(samplePair)

        then:
        RuntimeException e = thrown()
        e.message.contains("No ${RoddyWorkflowConfig.simpleName} found for")
    }


    void "getConfig when call fine"() {
        given:

        SamplePair samplePair = DomainFactory.createSamplePair()
        ConfigPerProject configPerProject = DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                pipeline: DomainFactory.createSophiaPipelineLazy(),
                project: samplePair.project,
        )

        expect:
        configPerProject == roddySophiaStartJob.getConfig(samplePair)

    }


    void "findSamplePairToProcess, all fine"() {
        given:
        def map = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair = map.samplePair
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                pipeline: DomainFactory.createSophiaPipelineLazy()
        )
        DomainFactory.createProcessingOption([
                name: SophiaService.PROCESSING_OPTION_REFERENCE_KEY,
                type: null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])

        expect:
        samplePair == roddySophiaStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "findSamplePairToProcess, wrong seqType should return null"() {
        given:
        SeqType notWgsSeqType = DomainFactory.createExomeSeqType()
        def map = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair = map.samplePair
        samplePair.mergingWorkPackage1.seqType = notWgsSeqType
        assert samplePair.mergingWorkPackage1.save(flush: true)
        samplePair.mergingWorkPackage2.seqType = notWgsSeqType
        assert samplePair.mergingWorkPackage2.save(flush: true)
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                pipeline: DomainFactory.createSophiaPipelineLazy()
        )
        DomainFactory.createProcessingOption([
                name: SophiaService.PROCESSING_OPTION_REFERENCE_KEY,
                type: null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])


        expect:
        null == roddySophiaStartJob.findSamplePairToProcess(ProcessingPriority.NORMAL_PRIORITY)
    }


    void "prepareCreatingTheProcessAndTriggerTracking, when input is null shall throw an exception"() {
        when:
        roddySophiaStartJob.prepareCreatingTheProcessAndTriggerTracking(null)

        then:
        AssertionError e = thrown()
        e.message.contains("bamFilePairAnalysis must not be null")
    }

    void "prepareCreatingTheProcessAndTriggerTracking, when all fine"() {
        given:
        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        RunSegment.list().each {
            it.otrsTicket = otrsTicket
            it.save(flush: true)
        }
        assert otrsTicket.sophiaStarted == null
        assert sophiaInstance.samplePair.sophiaProcessingStatus != SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED

        when:
        roddySophiaStartJob.prepareCreatingTheProcessAndTriggerTracking(sophiaInstance)

        then:
        assert otrsTicket.sophiaStarted != null
        assert sophiaInstance.samplePair.sophiaProcessingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
    }
}
