package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*

trait RoddyBamFileAnalysis implements BamFileAnalysisServiceTrait {

    @Override
    String getConfigName() {
        RoddyWorkflowConfig.name
    }
}
