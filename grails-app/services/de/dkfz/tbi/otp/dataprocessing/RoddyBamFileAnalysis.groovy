package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig

trait RoddyBamFileAnalysis implements BamFileAnalysisServiceTrait {

    @Override
    String getConfigName() {
        RoddyWorkflowConfig.name
    }
}
