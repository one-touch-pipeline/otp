import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
// Is not used anymore
plan("IndexSingleBamWorkflow") {
    start("start", "indexSingleBamStartJob")
    job("sendIndexingBam", "sendIndexingBamJob") {
        outputParameter("pbsIds")
    }
    job("indexingWatchdog", "myPBSWatchdogJob") {
        inputParameter("pbsIds", "sendIndexingBam", "pbsIds")
    }
    job("checkIndexFile", "checkIndexFileJob")
}

