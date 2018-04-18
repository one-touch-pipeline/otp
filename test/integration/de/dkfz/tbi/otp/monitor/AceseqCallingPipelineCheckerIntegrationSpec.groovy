package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

class AceseqCallingPipelineCheckerIntegrationSpec extends AbstractVariantCallingPipelineCheckerIntegrationSpec {

    @Override
    AbstractVariantCallingPipelineChecker createVariantCallingPipelineChecker() {
        return new AceseqCallingPipelineChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createAceseqPipelineLazy()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddySnvPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysis(Map properties) {
        return DomainFactory.createAceseqInstanceWithRoddyBamFiles(properties)
    }

    @Override
    BamFilePairAnalysis createAnalysisForCrosschecking(Map properties) {
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(properties)
    }


    void "workflowName, should return ACEseqWorkflow"() {
        expect:
        'ACEseqWorkflow' == createVariantCallingPipelineChecker().getWorkflowName()
    }

    void "processingStateMember, should return aceseqProcessingStatus"() {
        expect:
        'aceseqProcessingStatus' == createVariantCallingPipelineChecker().getProcessingStateMember()
    }

    void "pipelineType, should return Pipeline.Type.ACESEQ"() {
        expect:
        Pipeline.Type.ACESEQ == createVariantCallingPipelineChecker().getPipelineType()
    }

    void "bamFilePairAnalysisClass, should return AceseqInstance.class"() {
        expect:
        AceseqInstance.class == createVariantCallingPipelineChecker().getBamFilePairAnalysisClass()
    }
}
