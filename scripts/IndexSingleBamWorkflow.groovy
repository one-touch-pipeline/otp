import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("IndexSingleBamWorkflow") {
    start("start", "indexSingleBamStartJob")
    job("createSingleBamMergingLog", "createSingleBamMergingLogJob")
    job("createSingleBamDataFile", "createSingleBamDataFileJob")
    job("createSingleBamDirectory", "createSingleBamDirectoryJob")
    job("linkSingleBamFile", "linkSingleBamFileJob")
    job("checkMergedBamFile", "checkMergedBamFileJob")
    job("sendIndexingBam", "sendIndexingBamJob") {
        outputParameter("pbsIds")
    }
    job("indexingWatchdog", "myPBSWatchdogJob") {
        inputParameter("pbsIds", "sendIndexingBam", "pbsIds")
    }
    job("checkIndexFile", "checkIndexFileJob")
}

