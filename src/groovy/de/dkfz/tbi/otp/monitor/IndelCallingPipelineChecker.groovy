package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*

class IndelCallingPipelineChecker extends AbstractVariantCallingPipelineChecker {

    String getWorkflowName() {
        return "IndelWorkflow"
    }

    String getProcessingStateMember() {
        return 'indelProcessingStatus'
    }

    Pipeline.Type getPipelineType() {
        Pipeline.Type.INDEL
    }

    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return IndelCallingInstance.class
    }
}
