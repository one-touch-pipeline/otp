package de.dkfz.tbi.otp.dataprocessing


class AceseqService extends BamFileAnalysisService {

    protected String getProcessingStateCheck() {
        return "sp.aceseqProcessingStatus = :needsProcessing "
    }

    protected Class<AceseqInstance> getAnalysisClass() {
        return AceseqInstance.class
    }

    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.ACESEQ
    }
}

