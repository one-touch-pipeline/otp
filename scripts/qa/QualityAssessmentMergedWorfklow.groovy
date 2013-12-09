import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

// workflow requires processing options to be injected with the script InjectQualityAssessmentMergedWorkflowOptions.groovy

plan("QualityAssessmentMergedWorkflow") {
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