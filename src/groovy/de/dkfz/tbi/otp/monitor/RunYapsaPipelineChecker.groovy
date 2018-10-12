package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*

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
    Pipeline getPipeline() {
        return Pipeline.findByName(Pipeline.Name.RUN_YAPSA)
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return RunYapsaInstance.class
    }
}
