package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.ngsdata.*

class RunYapsaPipelineChecker extends AbstractVariantCallingPipelineChecker {

    @Override
    String getWorkflowName() {
        return "RunYapsaWorkflow"
    }

    @Override
    String getProcessingStateMember() {
        return 'runYapsaProcessingStatus'
    }

    @Override
    Pipeline.Type getPipelineType() {
        Pipeline.Type.MUTATIONAL_SIGNATURE
    }

    @Override
    List<SeqType> getSeqTypes() {
        SeqType.runYapsaPipelineSeqTypes
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return RunYapsaInstance.class
    }
}
