package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*

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
    Pipeline getPipeline() {
        return Pipeline.findByName(Pipeline.Name.RODDY_SNV)
    }

    @Override
    Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass() {
        return AbstractSnvCallingInstance.class
    }
}
