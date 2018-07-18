package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.*

class AceseqService extends RoddyBamFileAnalysisService {

    ProcessingOptionService processingOptionService

    @Override
    protected String getProcessingStateCheck() {
        return "sp.aceseqProcessingStatus = :needsProcessing AND " +
                "sp.sophiaProcessingStatus = 'NO_PROCESSING_NEEDED' AND " +
                "EXISTS (FROM SophiaInstance si " +
                "  WHERE si.samplePair = sp AND " +
                "  si.processingState = 'FINISHED' AND " +
                "  si.withdrawn = false " +
                ") AND " +
                "NOT EXISTS (FROM SophiaInstance si " +
                "  WHERE si.samplePair = sp AND " +
                "  si.processingState = 'IN_PROGRESS' AND " +
                "  si.withdrawn = false " +
                ") "
    }

    @Override
    Class<AceseqInstance> getAnalysisClass() {
        return AceseqInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.ACESEQ
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_ACESEQ
    }

    @Override
    protected List<SeqType> getSeqTypes() {
        return SeqType.aceseqPipelineSeqTypes
    }

    @Override
    protected String checkReferenceGenome(){
        return 'AND sp.mergingWorkPackage1.referenceGenome in (:referenceGenomes)'
    }

    @Override
    Map<String, Object> checkReferenceGenomeMap() {
        String referenceNamesString = processingOptionService.findOptionAssure(OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME, null, null)
        List<String> referenceGenomeNames = referenceNamesString.split(',')*.trim()
        return [referenceGenomes: ReferenceGenome.findAllByNameInList(referenceGenomeNames)]
    }
}

