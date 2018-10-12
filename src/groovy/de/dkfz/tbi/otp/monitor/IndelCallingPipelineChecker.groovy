package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*

class IndelCallingPipelineChecker extends AbstractVariantCallingPipelineChecker {

    @Override
    String getWorkflowName() {
        return "IndelWorkflow"
    }

    @Override
    String getProcessingStateMember() {
        return 'indelProcessingStatus'
    }

    @Override
    Pipeline getPipeline() {
        return Pipeline.findByName(Pipeline.Name.RODDY_INDEL)
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return IndelCallingInstance.class
    }
}
