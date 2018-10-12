package de.dkfz.tbi.otp.dataprocessing

class IndelCallingService extends BamFileAnalysisService implements RoddyBamFileAnalysis {

    @Override
    protected String getProcessingStateCheck() {
        return "sp.indelProcessingStatus = :needsProcessing "
    }

    @Override
    Class<IndelCallingInstance> getAnalysisClass() {
        return IndelCallingInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.INDEL
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_INDEL
    }

    @Override
    protected String pipelineSpecificBamFileChecks(String number) {
        return "AND ambf${number}.class in (de.dkfz.tbi.otp.dataprocessing.RoddyBamFile, de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile)"
    }
}
