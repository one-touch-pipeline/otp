package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*

class AceseqCallingPipelineChecker extends AbstractVariantCallingPipelineChecker {

    @Override
    String getWorkflowName() {
        return "ACEseqWorkflow"
    }

    @Override
    String getProcessingStateMember() {
        return 'aceseqProcessingStatus'
    }

    @Override
    Pipeline getPipeline() {
        return Pipeline.findByName(Pipeline.Name.RODDY_ACESEQ)
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return AceseqInstance.class
    }
}
