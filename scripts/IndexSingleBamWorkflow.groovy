import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

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

