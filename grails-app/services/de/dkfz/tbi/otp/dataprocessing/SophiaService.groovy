package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class SophiaService extends BamFileAnalysisService {

    static final String PROCESSING_OPTION_REFERENCE_KEY = 'sophia reference genome'

    ProcessingOptionService processingOptionService

    @Override
    protected String getProcessingStateCheck() {
        return "sp.sophiaProcessingStatus = :needsProcessing "
    }

    @Override
    protected Class<SophiaInstance> getAnalysisClass() {
        return SophiaInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.SOPHIA
    }

    @Override
    protected String checkReferenceGenome() {
        return 'AND sp.mergingWorkPackage1.referenceGenome in (:referenceGenomes)'
    }

    @Override
    protected String pipelineSpecificBamFileChecks(String number) {
        return "AND ambf${number}.class = de.dkfz.tbi.otp.dataprocessing.RoddyBamFile"
    }

    @Override
    public Map<String, Object> checkReferenceGenomeMap() {
        String referenceNamesString = processingOptionService.findOptionAssure(PROCESSING_OPTION_REFERENCE_KEY, null, null)
        List<String> referenceGenomeNames = referenceNamesString.split(',')*.trim()
        return [referenceGenomes: ReferenceGenome.findAllByNameInList(referenceGenomeNames)]
    }

}
