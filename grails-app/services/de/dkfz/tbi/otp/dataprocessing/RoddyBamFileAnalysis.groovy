package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*

trait RoddyBamFileAnalysis implements BamFileAnalysisServiceTrait {

    @Override
    String additionalConfigParameters() {
        return "   AND cps.seqType = sp.mergingWorkPackage1.seqType "
    }

    @Override
    String getConfigName() {
        RoddyWorkflowConfig.name
    }
}
