package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

class OtpSnvCallingPipelineCheckerIntegrationSpec extends AbstractSnvCallingPipelineCheckerIntegrationSpec {

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createOtpSnvPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysis(Map properties) {
        return DomainFactory.createSnvInstanceWithRoddyBamFiles(properties)
    }

    ConfigPerProject createConfig(SamplePair samplePair, Map properties = [:]) {
        DomainFactory.createSnvConfig([
                pipeline: createPipeLine(),
                seqType : samplePair.seqType,
                project : samplePair.project,
        ] + properties)
    }


    void "workflowName, should return SnvWorkflow"() {
        expect:
        'SnvWorkflow' == new SnvCallingPipelineChecker().getWorkflowName()
    }

    void "processingStateMember, should return snvProcessingStatus"() {
        expect:
        'snvProcessingStatus' == new SnvCallingPipelineChecker().getProcessingStateMember()
    }

    void "pipelineType, should return Pipeline.Type.SNV"() {
        expect:
        Pipeline.Type.SNV == new SnvCallingPipelineChecker().getPipelineType()
    }

    void "bamFilePairAnalysisClass, should return SnvCallingInstance.class"() {
        expect:
        SnvCallingInstance.class == new SnvCallingPipelineChecker().getBamFilePairAnalysisClass()
    }


    void "displayRunning, check that running is called with expected parameters"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AbstractVariantCallingPipelineChecker pipelineChecker = createVariantCallingPipelineChecker()

        SamplePair samplePair = DomainFactory.createSamplePairPanCan([
                (pipelineChecker.getProcessingStateMember()): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        BamFilePairAnalysis runningAnalysis = createAnalysis([
                samplePair: samplePair,
        ])

        when:
        pipelineChecker.displayRunning([runningAnalysis], output)

        then:
        1 * output.showRunning('SnvWorkflow', [runningAnalysis])
        1 * output.showRunning('RoddySnvWorkflow', null)
    }
}
