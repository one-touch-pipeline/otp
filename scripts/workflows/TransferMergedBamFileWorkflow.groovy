import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.utils.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "transferMergedBamFileWorkflow"

plan(workflowName) {

    start("start", "transferMergedBamFileStartJob")

    job("calculateFileChecksumMD5", "calculateFileChecksumMD5Job") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }

    job("calculateFileChecksumMD5Watchdog", "watchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "calculateFileChecksumMD5", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "calculateFileChecksumMD5", JobParameterKeys.REALM)
    }

    job("transferMergedBamFile", "transferMergedBamFileJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }

    job("transferMergedBamFileWatchdog", "watchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "transferMergedBamFile", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "transferMergedBamFile", JobParameterKeys.REALM)
    }

    job("checkMergedBamFileChecksumMD5", "checkMergedBamFileChecksumMD5Job") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }

    job("checkMergedBamFileChecksumMD5Watchdog", "watchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "checkMergedBamFileChecksumMD5", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "checkMergedBamFileChecksumMD5", JobParameterKeys.REALM)
    }

    job("transferMergedQAResult", "transferMergedQAResultJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }

    job("transferMergedQAResultWatchdog", "watchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "transferMergedQAResult", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "transferMergedQAResult", JobParameterKeys.REALM)
    }

    job("transferSingleLaneQAResult", "transferSingleLaneQAResultJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }

    job("transferSingleLangeQAResultWatchdog", "watchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "transferSingleLaneQAResult", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "transferSingleLaneQAResult", JobParameterKeys.REALM)
    }

    job("checkQaResultsChecksumMD5", "checkQaResultsChecksumMD5Job") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }

    job("checkQaResultsChecksumMD5Watchdog", "watchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "checkQaResultsChecksumMD5", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "checkQaResultsChecksumMD5", JobParameterKeys.REALM)
    }

    job("createQAResultStatisticsFile", "createQAResultStatisticsFileJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }

    job("createQAResultStatisticsFileWatchdog", "watchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "createQAResultStatisticsFile", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "createQAResultStatisticsFile", JobParameterKeys.REALM)
    }

    job("moveFilesToFinalDestination", "moveFilesToFinalDestinationJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }

    job("moveFilesToFinalDestinationWatchdog", "watchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "moveFilesToFinalDestination", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "moveFilesToFinalDestination", JobParameterKeys.REALM)
    }

    job("storeChecksumOfMergedBamFile", "storeChecksumOfMergedBamFileJob")

    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

// number of all TransferMergedBamFile workflows which can be executed in parallel
processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, '60', workflowName)

// number of slots which are reserved only for FastTrack Workflows
processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, '30', workflowName)
