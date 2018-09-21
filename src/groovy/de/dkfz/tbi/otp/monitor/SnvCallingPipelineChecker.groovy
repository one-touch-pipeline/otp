package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

class SnvCallingPipelineChecker extends AbstractVariantCallingPipelineChecker {

    @Override
    String getWorkflowName() {
        return "RoddySnvWorkflow"
    }

    @Override
    String getProcessingStateMember() {
        return 'snvProcessingStatus'
    }

    @Override
    Pipeline.Type getPipelineType() {
        Pipeline.Type.SNV
    }

    @Override
    List<SeqType> getSeqTypes() {
        SeqType.snvPipelineSeqTypes
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return AbstractSnvCallingInstance.class
    }
}
