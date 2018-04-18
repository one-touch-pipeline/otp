package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*

class SnvCallingService extends BamFileAnalysisService {

    protected String getProcessingStateCheck() {
        return "sp.snvProcessingStatus = :needsProcessing "
    }

    protected Class<RoddySnvCallingInstance> getAnalysisClass() {
        return RoddySnvCallingInstance.class
    }

    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.SNV
    }
}
