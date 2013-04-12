import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("MergingWorkflow") {
    start("start", "MergingStartJob")
    job("createOutputDirectory", "mergingCreateOutputDirectoryJob")
    job("checkCreateOutputDirectory", "mergingCheckCreateOutputDirectoryJob")
    job("merging", "mergingJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("mergingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "merging", "__pbsIds")
        inputParameter("__pbsRealm", "merging", "__pbsRealm")
    }
    job("mergingValidation", "mergingValidationJob")
    job("mergingIndexing", "mergingIndexingJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("mergingIndexingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "mergingIndexing", "__pbsIds")
        inputParameter("__pbsRealm", "mergingIndexing", "__pbsRealm")
    }
    job("mergingIndexValidation", "mergingIndexValidationJob")
    job("mergingComplete", "mergingCompleteJob")
}
