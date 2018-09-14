package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class AceseqService extends BamFileAnalysisService implements RoddyBamFileAnalysis, WithReferenceGenomeRestriction {

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
    List<String> getReferenceGenomes() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME)
    }
}

