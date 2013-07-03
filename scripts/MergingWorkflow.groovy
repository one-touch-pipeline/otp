import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("MergingWorkflow") {
    start("start", "mergingStartJob")
    job("createOutputDirectory", "mergingCreateOutputDirectoryJob")
    job("merging", "mergingJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("mergingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "merging", "__pbsIds")
        inputParameter("__pbsRealm", "merging", "__pbsRealm")
    }
    job("mergingValidation", "mergingValidationJob")
    job("mergingFileIndexing", "mergingFileIndexingJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("mergingFileIndexingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "mergingFileIndexing", "__pbsIds")
        inputParameter("__pbsRealm", "mergingFileIndexing", "__pbsRealm")
    }
    job("mergingFileIndexValidation", "mergingFileIndexValidationJob")
    job("mergingComplete", "mergingCompleteJob")
}
