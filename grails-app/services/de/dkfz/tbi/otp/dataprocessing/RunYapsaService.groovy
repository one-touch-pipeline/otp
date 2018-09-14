package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.ngsdata.*

class RunYapsaService extends BamFileAnalysisService implements WithReferenceGenomeRestriction {

    @Override
    protected String getProcessingStateCheck() {
        return "sp.runYapsaProcessingStatus = :needsProcessing AND " +
                "sp.snvProcessingStatus = 'NO_PROCESSING_NEEDED' AND " +
                "EXISTS (FROM RoddySnvCallingInstance sci " +
                "  WHERE sci.samplePair = sp AND " +
                "  sci.processingState = 'FINISHED' AND " +
                "  sci.withdrawn = false " +
                ") AND " +
                "NOT EXISTS (FROM RoddySnvCallingInstance sci " +
                "  WHERE sci.samplePair = sp AND " +
                "  sci.processingState = 'IN_PROGRESS' AND " +
                "  sci.withdrawn = false " +
                ") "
    }

    @Override
    Class<RunYapsaInstance> getAnalysisClass() {
        return RunYapsaInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.MUTATIONAL_SIGNATURE
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RUN_YAPSA
    }

    @Override
    protected List<SeqType> getSeqTypes() {
        return SeqType.runYapsaPipelineSeqTypes
    }

    @Override
    List<String> getReferenceGenomes() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME)
    }

    @Override
    String getConfigName() {
        RunYapsaConfig.name
    }
}
