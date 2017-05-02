package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*

class SophiaCallingPipelineChecker extends AbstractVariantCallingPipelineChecker {

    String getWorkflowName() {
        return "SophiaWorkflow"
    }

    String getProcessingStateMember() {
        return 'sophiaProcessingStatus'
    }

    Pipeline.Type getPipelineType() {
        Pipeline.Type.SOPHIA
    }

    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return SophiaInstance.class
    }
}
