package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.DomainFactory

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
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(properties)
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
        given:
        createPipeLine()

        expect:
        Pipeline.Type.SOPHIA == createVariantCallingPipelineChecker().getPipeline().type
    }

    void "bamFilePairAnalysisClass, should return SophiaInstance.class"() {
        expect:
        SophiaInstance.class == createVariantCallingPipelineChecker().getBamFilePairAnalysisClass()
    }
}
