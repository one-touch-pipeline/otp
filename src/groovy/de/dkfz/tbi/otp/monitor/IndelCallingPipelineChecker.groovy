package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

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

    List<SeqType> getSeqTypes() {
        SeqType.indelPipelineSeqTypes
    }

    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return IndelCallingInstance.class
    }
}
