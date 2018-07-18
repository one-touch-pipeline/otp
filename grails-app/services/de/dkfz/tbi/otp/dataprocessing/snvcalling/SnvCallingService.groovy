package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class SnvCallingService extends RoddyBamFileAnalysisService {

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

    @Override
    protected List<SeqType> getSeqTypes() {
        return SeqType.snvPipelineSeqTypes
    }
}
