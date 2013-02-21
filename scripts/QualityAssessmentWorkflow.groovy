import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("QualityAssessmentWorkflow") {
    start("start", "qualityAssessmentStartJob")
    job("createQaOutputDirectory", "createQaOutputDirectoryJob")
    job("executeBamFileQaAnalysis", "executeBamFileQaAnalysisJob") {
        outputParameter("__pbsIds")
    }
    job("executeBamFileQaAnalysisWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "executeBamFileQaAnalysis", "__pbsIds")
    }
    job("qaOutputFileValidation", "qaOutopuFileValidationJob")
    job("parseQaStatistics", "parseQaStatisticsJob")
    job("createInsertSizePlot","createInsertSizePlotJob")
    job("insertSizePlotValidation", "insertSizePlotValidationJob")
    job("reorderCoverageTable", "reorderCoverageTableJob")
    job("createCoveragePlot", "createCoveragePlotJob")
    job("coveragePlotValidation", "coveragePlotValidationJob")
    job("assignQaFlag", "assignQaFlagJob")
}
