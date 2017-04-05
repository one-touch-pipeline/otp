package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*

class AceseqCallingPipelineChecker extends AbstractVariantCallingPipelineChecker {

    String getWorkflowName() {
        return "ACEseqWorkflow"
    }

    String getProcessingStateMember() {
        return 'aceseqProcessingStatus'
    }

    Pipeline.Type getPipelineType() {
        Pipeline.Type.ACESEQ
    }

    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return AceseqInstance.class
    }
}
