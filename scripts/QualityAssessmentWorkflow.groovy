import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("QualityAssessmentWorkflow") {
    start("start", "qualityAssessmentStartJob")
    job("createQaOutputDirectory", "createQaOutputDirectoryJob")
    job("executeBamFileQaAnalysis", "executeBamFileQaAnalysisJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("executeBamFileQaAnalysisWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "executeBamFileQaAnalysis", "__pbsIds")
        inputParameter("__pbsRealm", "executeBamFileQaAnalysis", "__pbsRealm")
    }
    job("qaOutputFileValidation", "qaOutputFileValidationJob")

    job("parseQaStatistics", "parseQaStatisticsJob")

    job("reorderCoverageTable", "reorderCoverageTableJob")
    job("createCoveragePlot", "createCoveragePlotJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createCoveragePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createCoveragePlot", "__pbsIds")
        inputParameter("__pbsRealm", "createCoveragePlot", "__pbsRealm")
    }
    job("coveragePlotValidation", "coveragePlotValidationJob")
    job("createInsertSizePlot", "createInsertSizePlotJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createInsertSizePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createInsertSizePlot", "__pbsIds")
        inputParameter("__pbsRealm", "createInsertSizePlot", "__pbsRealm")
    }
    job("insertSizePlotValidation", "insertSizePlotValidationJob")
    job("assignQaFlag", "assignQaFlagJob")
}
