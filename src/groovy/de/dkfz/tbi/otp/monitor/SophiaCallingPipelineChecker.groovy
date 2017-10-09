package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*

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

    List<SeqType> getSeqTypes() {
        SeqType.sophiaPipelineSeqTypes
    }

    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return SophiaInstance.class
    }
}
