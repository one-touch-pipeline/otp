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
        return """
        AND (
            ambf${number}.class = de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
            OR (
                ambf${number}.class = de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
                AND ambf${number}.insertSizeFile IS NOT NULL
                AND ambf${number}.maximumReadLength IS NOT NULL
            )
        )
        """.replaceAll('\n', ' ')
    }

    @Override
    List<String> getReferenceGenomes() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME)
    }
}
