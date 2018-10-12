package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*

class SnvCallingService extends BamFileAnalysisService implements RoddyBamFileAnalysis {

    @Override
    protected String getProcessingStateCheck() {
        return "sp.snvProcessingStatus = :needsProcessing "
    }

    @Override
    Class<RoddySnvCallingInstance> getAnalysisClass() {
        return RoddySnvCallingInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.SNV
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_SNV
    }
}
