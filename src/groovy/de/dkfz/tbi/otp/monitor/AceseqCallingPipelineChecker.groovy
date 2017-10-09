package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

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

    List<SeqType> getSeqTypes() {
        SeqType.aceseqPipelineSeqTypes
    }

    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return AceseqInstance.class
    }
}
