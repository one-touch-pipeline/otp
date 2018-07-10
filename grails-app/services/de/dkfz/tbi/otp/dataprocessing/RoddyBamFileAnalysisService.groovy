package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig

abstract class RoddyBamFileAnalysisService extends BamFileAnalysisService {

    @Override
    String additionalConfigParameters() {
        return "   AND cps.seqType = sp.mergingWorkPackage1.seqType "
    }

    @Override
    String getConfigName() {
        RoddyWorkflowConfig.name
    }
}
