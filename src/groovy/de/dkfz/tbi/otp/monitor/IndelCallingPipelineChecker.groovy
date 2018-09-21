package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

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
    Pipeline.Type getPipelineType() {
        Pipeline.Type.INDEL
    }

    @Override
    List<SeqType> getSeqTypes() {
        SeqType.indelPipelineSeqTypes
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return IndelCallingInstance.class
    }
}
