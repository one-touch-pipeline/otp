package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*

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
    Pipeline.Type getPipelineType() {
        Pipeline.Type.SOPHIA
    }

    @Override
    List<SeqType> getSeqTypes() {
        SeqType.sophiaPipelineSeqTypes
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return SophiaInstance.class
    }
}
