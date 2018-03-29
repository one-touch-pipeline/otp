package de.dkfz.tbi.otp.dataprocessing


class IndelCallingService extends BamFileAnalysisService {

    protected String getProcessingStateCheck() {
        return "sp.indelProcessingStatus = :needsProcessing "
    }

    protected Class<IndelCallingInstance> getAnalysisClass() {
        return IndelCallingInstance.class
    }

    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.INDEL
    }

    @Override
    protected String pipelineSpecificBamFileChecks(String number) {
        return "AND ambf${number}.class in (de.dkfz.tbi.otp.dataprocessing.RoddyBamFile, de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile)"
    }
}
