import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.getSLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME
import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.getTOTAL_SLOTS_OPTION_NAME
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "transferMergedBamFileWorkflow"

plan(workflowName) {

    start("start", "transferMergedBamFileStartJob")

    job("calculateFileChecksumMD5", "calculateFileChecksumMD5Job") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }

    job("calculateFileChecksumMD5Watchdog", "watchdogJob") {
        inputParameter("__pbsIds", "calculateFileChecksumMD5", "__pbsIds")
        inputParameter("__pbsRealm", "calculateFileChecksumMD5", "__pbsRealm")
    }

    job("transferMergedBamFile", "transferMergedBamFileJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }

    job("transferMergedBamFileWatchdog", "watchdogJob") {
        inputParameter("__pbsIds", "transferMergedBamFile", "__pbsIds")
        inputParameter("__pbsRealm", "transferMergedBamFile", "__pbsRealm")
    }

    job("checkMergedBamFileChecksumMD5", "checkMergedBamFileChecksumMD5Job") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }

    job("checkMergedBamFileChecksumMD5Watchdog", "watchdogJob") {
        inputParameter("__pbsIds", "checkMergedBamFileChecksumMD5", "__pbsIds")
        inputParameter("__pbsRealm", "checkMergedBamFileChecksumMD5", "__pbsRealm")
    }

    job("transferMergedQAResult", "transferMergedQAResultJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }

    job("transferMergedQAResultWatchdog", "watchdogJob") {
        inputParameter("__pbsIds", "transferMergedQAResult", "__pbsIds")
        inputParameter("__pbsRealm", "transferMergedQAResult", "__pbsRealm")
    }

    job("transferSingleLaneQAResult", "transferSingleLaneQAResultJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }

    job("transferSingleLangeQAResultWatchdog", "watchdogJob") {
        inputParameter("__pbsIds", "transferSingleLaneQAResult", "__pbsIds")
        inputParameter("__pbsRealm", "transferSingleLaneQAResult", "__pbsRealm")
    }

    job("checkQaResultsChecksumMD5", "checkQaResultsChecksumMD5Job") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }

    job("checkQaResultsChecksumMD5Watchdog", "watchdogJob") {
        inputParameter("__pbsIds", "checkQaResultsChecksumMD5", "__pbsIds")
        inputParameter("__pbsRealm", "checkQaResultsChecksumMD5", "__pbsRealm")
    }

    job("createQAResultStatisticsFile", "createQAResultStatisticsFileJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }

    job("createQAResultStatisticsFileWatchdog", "watchdogJob") {
        inputParameter("__pbsIds", "createQAResultStatisticsFile", "__pbsIds")
        inputParameter("__pbsRealm", "createQAResultStatisticsFile", "__pbsRealm")
    }

    job("moveFilesToFinalDestination", "moveFilesToFinalDestinationJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }

    job("moveFilesToFinalDestinationWatchdog", "watchdogJob") {
        inputParameter("__pbsIds", "moveFilesToFinalDestination", "__pbsIds")
        inputParameter("__pbsRealm", "moveFilesToFinalDestination", "__pbsRealm")
    }

    job("storeChecksumOfMergedBamFile", "storeChecksumOfMergedBamFileJob")
}

// number of all TransferMergedBamFile workflows which can be executed in parallel
println ctx.processingOptionService.createOrUpdate(TOTAL_SLOTS_OPTION_NAME, workflowName, null, '60', '')

// number of slots which are reserved only for FastTrack Workflows
println ctx.processingOptionService.createOrUpdate(SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflowName, null, '30', '')