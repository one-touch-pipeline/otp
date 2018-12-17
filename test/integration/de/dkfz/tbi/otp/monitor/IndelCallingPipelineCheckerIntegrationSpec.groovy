package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class IndelCallingPipelineCheckerIntegrationSpec extends AbstractVariantCallingPipelineCheckerIntegrationSpec {

    @Override
    AbstractVariantCallingPipelineChecker createVariantCallingPipelineChecker() {
        return new IndelCallingPipelineChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createIndelPipelineLazy()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddySnvPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysis(Map properties) {
        return DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(properties)
    }

    @Override
    BamFilePairAnalysis createAnalysisForCrosschecking(Map properties) {
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(properties)
    }


    void "workflowName, should return IndelWorkflow"() {
        expect:
        'IndelWorkflow' == new IndelCallingPipelineChecker().getWorkflowName()
    }

    void "processingStateMember, should return indelProcessingStatus"() {
        expect:
        'indelProcessingStatus' == new IndelCallingPipelineChecker().getProcessingStateMember()
    }

    void "pipelineType, should return Pipeline.Type.INDEL"() {
        given:
        createPipeLine()

        expect:
        Pipeline.Type.INDEL == new IndelCallingPipelineChecker().getPipeline().type
    }

    void "bamFilePairAnalysisClass, should return IndelCallingInstance.class"() {
        expect:
        IndelCallingInstance.class == new IndelCallingPipelineChecker().getBamFilePairAnalysisClass()
    }

}
