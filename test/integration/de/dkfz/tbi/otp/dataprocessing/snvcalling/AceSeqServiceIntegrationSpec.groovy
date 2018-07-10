package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*

class AceSeqServiceIntegrationSpec extends IntegrationSpec {

    SamplePair samplePair1
    ConfigPerProject roddyConfig1
    AbstractMergedBamFile bamFile1_1
    AbstractMergedBamFile bamFile2_1

    AceseqService aceseqService

    def setup() {
        def map = DomainFactory.createProcessableSamplePair()

        samplePair1 = map.samplePair
        bamFile1_1 = map.bamFile1
        bamFile2_1 = map.bamFile2
        roddyConfig1 = map.roddyConfig

        DomainFactory.createAllAnalysableSeqTypes()
    }

    void "samplePairForProcessing, for Aceseq pipeline, when sophia has not run, should not return SamplePair"() {
        given:
        prepareSophiaForAceseqBase()

        expect:
        null == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "samplePairForProcessing, for Aceseq pipeline, when last sophia instance is running and not withdrawn and an older finish exist, should not return SamplePair"() {
        given:
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: false], [processingState: AnalysisProcessingStates.IN_PROGRESS, withdrawn: false])

        expect:
        null == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "samplePairForProcessing, for Aceseq pipeline, when last sophia instance is running and withdrawn and an older finish exist, should return SamplePair"() {
        given:
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: false], [processingState: AnalysisProcessingStates.IN_PROGRESS, withdrawn: true])

        expect:
        samplePair1 == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }



    void "samplePairForProcessing, for Aceseq pipeline, when all sophia instances are withdrawn, should not return SamplePair"() {
        given:
        prepareSophiaForAceseq([processingState: AnalysisProcessingStates.FINISHED, withdrawn: true], [processingState: AnalysisProcessingStates.FINISHED, withdrawn: true])

        expect:
        null == aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    void "samplePairForProcessing, for ACEseq pipeline, coverage is not high enough, should not return SamplePair"() {
        given:
        prepareSophiaForAceseq([:], [:])
        DomainFactory.createProcessingOptionLazy([
                name: ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE,
                type: Pipeline.Type.ACESEQ.toString(),
                project: null,
                value: "40",
        ])

        expect:
        !aceseqService.samplePairForProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }


    private void prepareSophiaForAceseqBase() {
        samplePair1.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair1.save(flush: true)
        DomainFactory.createProcessingOptionLazy([
                name   : ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                type   : null,
                project: null,
                value  : samplePair1.mergingWorkPackage1.referenceGenome.name,
        ])
        DomainFactory.createRoddyWorkflowConfig(
                seqType: samplePair1.seqType,
                project: samplePair1.project,
                pipeline: DomainFactory.createAceseqPipelineLazy(),
        )
    }

    private void prepareSophiaForAceseq(Map propertiesSophia1, Map propertiesSophia2) {
        prepareSophiaForAceseqBase()

        Map defaultMap = [
                processingState: AnalysisProcessingStates.FINISHED,
                withdrawn: false,
                sampleType1BamFile: bamFile1_1,
                sampleType2BamFile: bamFile2_1,
        ]

        DomainFactory.createSophiaInstance(samplePair1, defaultMap + propertiesSophia1)
        DomainFactory.createSophiaInstance(samplePair1, defaultMap + propertiesSophia2)
    }


}
