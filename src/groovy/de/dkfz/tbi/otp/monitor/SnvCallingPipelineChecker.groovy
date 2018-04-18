package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

class SnvCallingPipelineChecker extends AbstractVariantCallingPipelineChecker {

    String getWorkflowName() {
        return "RoddySnvWorkflow"
    }

    String getProcessingStateMember() {
        return 'snvProcessingStatus'
    }

    Pipeline.Type getPipelineType() {
        Pipeline.Type.SNV
    }

    List<SeqType> getSeqTypes() {
        SeqType.snvPipelineSeqTypes
    }

    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return AbstractSnvCallingInstance.class
    }
}
