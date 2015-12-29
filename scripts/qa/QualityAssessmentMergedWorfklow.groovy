import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.plan
import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.getSLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME
import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.getTOTAL_SLOTS_OPTION_NAME

// workflow requires processing options to be injected with the script InjectQualityAssessmentMergedWorkflowOptions.groovy

String workflowName = "QualityAssessmentMergedWorkflow"

plan(workflowName) {
    start("start", "qualityAssessmentMergedStartJob")
    job("createMergedQaOutputDirectory", "createMergedQaOutputDirectoryJob")
    job("executeMergedBamFileQaAnalysis", "executeMergedBamFileQaAnalysisJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("executeMergedBamFileQaAnalysisWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "executeMergedBamFileQaAnalysis", "__pbsIds")
        inputParameter("__pbsRealm", "executeMergedBamFileQaAnalysis", "__pbsRealm")
    }
    job("mergedQaOutputFileValidation", "mergedQaOutputFileValidationJob")
    job("parseMergedQaStatistics", "parseMergedQaStatisticsJob")
    job("createMergedChromosomeMappingFileJob", "createMergedChromosomeMappingFileJob")
    job("executeMergedMappingFilteringSortingToCoverageTable", "executeMergedMappingFilteringSortingToCoverageTableJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("executeMergedMappingFilteringSortingToCoverageTableWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "executeMergedMappingFilteringSortingToCoverageTable", "__pbsIds")
        inputParameter("__pbsRealm", "executeMergedMappingFilteringSortingToCoverageTable", "__pbsRealm")
    }
    job("mergedMappingFilteringSortingOutputFileValidation", "mergedMappingFilteringSortingOutputFileValidationJob")

    job("createMergedCoveragePlot", "createMergedCoveragePlotJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createMergedCoveragePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createMergedCoveragePlot", "__pbsIds")
        inputParameter("__pbsRealm", "createMergedCoveragePlot", "__pbsRealm")
    }
    job("mergedCoveragePlotValidation", "mergedCoveragePlotValidationJob")
    job("createMergedInsertSizePlot", "createMergedInsertSizePlotJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createMergedInsertSizePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createMergedInsertSizePlot", "__pbsIds")
        inputParameter("__pbsRealm", "createMergedInsertSizePlot", "__pbsRealm")
    }
    job("mergedInsertSizePlotValidation", "mergedInsertSizePlotValidationJob")
    job("assignMergedQaFlag", "assignMergedQaFlagJob")
}

// number of all MergedQA workflows which can be executed in parallel
println ctx.processingOptionService.createOrUpdate(TOTAL_SLOTS_OPTION_NAME, workflowName, null, '60', '')

// number of slots which are reserved only for FastTrack Workflows
println ctx.processingOptionService.createOrUpdate(SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflowName, null, '30', '')