package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*

class SophiaCallingPipelineCheckerIntegrationSpec extends AbstractVariantCallingPipelineCheckerIntegrationSpec {

    @Override
    AbstractVariantCallingPipelineChecker createVariantCallingPipelineChecker() {
        return new SophiaCallingPipelineChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createSophiaPipelineLazy()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddySnvPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysis(Map properties) {
        return DomainFactory.createSophiaInstanceWithRoddyBamFiles(properties)
    }

    @Override
    BamFilePairAnalysis createAnalysisForCrosschecking(Map properties) {
        return DomainFactory.createSnvInstanceWithRoddyBamFiles(properties)
    }


    void "workflowName, should return SophiaWorkflow"() {
        expect:
        'SophiaWorkflow' == createVariantCallingPipelineChecker().getWorkflowName()
    }

    void "processingStateMember, should return sophiaProcessingStatus"() {
        expect:
        'sophiaProcessingStatus' == createVariantCallingPipelineChecker().getProcessingStateMember()
    }

    void "pipelineType, should return Pipeline.Type.SOPHIA"() {
        expect:
        Pipeline.Type.SOPHIA == createVariantCallingPipelineChecker().getPipelineType()
    }

    void "bamFilePairAnalysisClass, should return SophiaInstance.class"() {
        expect:
        SophiaInstance.class == createVariantCallingPipelineChecker().getBamFilePairAnalysisClass()
    }
}
