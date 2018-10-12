package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*

class SophiaCallingPipelineChecker extends AbstractVariantCallingPipelineChecker {

    @Override
    String getWorkflowName() {
        return "SophiaWorkflow"
    }

    @Override
    String getProcessingStateMember() {
        return 'sophiaProcessingStatus'
    }

    @Override
    Pipeline getPipeline() {
        return Pipeline.findByName(Pipeline.Name.RODDY_SOPHIA)
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return SophiaInstance.class
    }
}
