package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

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
    Pipeline.Type getPipelineType() {
        Pipeline.Type.ACESEQ
    }

    @Override
    List<SeqType> getSeqTypes() {
        SeqType.aceseqPipelineSeqTypes
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return AceseqInstance.class
    }
}
