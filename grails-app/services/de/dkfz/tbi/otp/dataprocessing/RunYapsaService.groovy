package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.ngsdata.*

class RunYapsaService extends BamFileAnalysisService {

    ProcessingOptionService processingOptionService

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
    protected String checkReferenceGenome() {
        return 'AND sp.mergingWorkPackage1.referenceGenome in (:referenceGenomes)'
    }

    @Override
    public Map<String, Object> checkReferenceGenomeMap() {
        String referenceNamesString = processingOptionService.findOptionAssure(ProcessingOption.OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME, null, null)
        List<String> referenceGenomeNames = referenceNamesString.split(',')*.trim()
        return [referenceGenomes: ReferenceGenome.findAllByNameInList(referenceGenomeNames)]
    }

    @Override
    String getConfigName() {
        RunYapsaConfig.name
    }
}