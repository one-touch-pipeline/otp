package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.sophia.*

class SophiaService extends BamFileAnalysisService implements RoddyBamFileAnalysis, WithReferenceGenomeRestriction {

    @Override
    protected String getProcessingStateCheck() {
        return "sp.sophiaProcessingStatus = :needsProcessing "
    }

    @Override
    Class<SophiaInstance> getAnalysisClass() {
        return SophiaInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.SOPHIA
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_SOPHIA
    }

    @Override
    protected String pipelineSpecificBamFileChecks(String number) {
        //TODO: After solving OTP-2950 re-enable external bam files which has all required values:
        // insertSizeFile, maximumReadLength, controlMedianIsize, tumorMedianIsize, controlStdIsizePercentage,
        // tumorStdIsizePercentage, controlProperPairPercentage, tumorProperPairPercentage,
        return """
        AND (
            ambf${number}.class = de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
        )
        """.replaceAll('\n', ' ')
    }

    @Override
    List<String> getReferenceGenomes() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME)
    }
}
