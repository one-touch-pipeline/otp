package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome


class AceseqService extends BamFileAnalysisService {

    static final String PROCESSING_OPTION_REFERENCE_KEY = 'aceseq reference genome'

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
    protected Class<AceseqInstance> getAnalysisClass() {
        return AceseqInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.ACESEQ
    }

    @Override
    protected String checkReferenceGenome(){
        return 'AND sp.mergingWorkPackage1.referenceGenome in (:referenceGenomes)'
    }

    @Override
    public Map<String, Object> checkReferenceGenomeMap() {
        String referenceNamesString = processingOptionService.findOptionAssure(PROCESSING_OPTION_REFERENCE_KEY, null, null)
        List<String> referenceGenomeNames = referenceNamesString.split(',')*.trim()
        return [referenceGenomes: ReferenceGenome.findAllByNameInList(referenceGenomeNames)]
    }

}

