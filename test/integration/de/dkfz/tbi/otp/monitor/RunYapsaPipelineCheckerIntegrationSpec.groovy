package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*


class RunYapsaPipelineCheckerIntegrationSpec extends AbstractVariantCallingPipelineCheckerIntegrationSpec {

    @Override
    AbstractVariantCallingPipelineChecker createVariantCallingPipelineChecker() {
        return new RunYapsaPipelineChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createRunYapsaPipelineLazy()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddySnvPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysis(Map properties) {
        return DomainFactory.createRunYapsaInstanceWithRoddyBamFiles(properties)
    }

    @Override
    BamFilePairAnalysis createAnalysisForCrosschecking(Map properties) {
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(properties)
    }


    // RunYapsa is not a Roddy workflow, so the default doesn't work...
    @Override
    ConfigPerProjectAndSeqType createConfig(SamplePair samplePair, Map properties = [:]) {
        DomainFactory.createRunYapsaConfigLazy([
                pipeline: createPipeLine(),
                seqType : samplePair.seqType,
                project : samplePair.project,
        ] + properties)
    }


    void "workflowName, should return RunYapsaWorkflow"() {
        expect:
        'RunYapsaWorkflow' == createVariantCallingPipelineChecker().getWorkflowName()
    }

    void "processingStateMember, should return runYapsaProcessingStatus"() {
        expect:
        'runYapsaProcessingStatus' == createVariantCallingPipelineChecker().getProcessingStateMember()
    }

    void "pipelineType, should return Pipeline.Type.MUTATIONAL_SIGNATURE"() {
        expect:
        Pipeline.Type.MUTATIONAL_SIGNATURE == createVariantCallingPipelineChecker().getPipelineType()
    }

    void "bamFilePairAnalysisClass, should return RunYapsaInstance.class"() {
        expect:
        RunYapsaInstance.class == createVariantCallingPipelineChecker().getBamFilePairAnalysisClass()
    }
}
