import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys

plan('transferMergedBamFileWorkflow') {

    start('start', 'transferMergedBamFileStartJob')

    job('calculateFileChecksumMD5', 'calculateFileChecksumMD5Job') {
        outputParameter("${JobParameterKeys.PBS_ID_LIST}")
        outputParameter("${JobParameterKeys.REALM}")
    }

    job('calculateFileChecksumMD5Watchdog', 'watchdogJob') {
        inputParameter("${JobParameterKeys.PBS_ID_LIST}", 'calculateFileChecksumMD5', "${JobParameterKeys.PBS_ID_LIST}")
        inputParameter("${JobParameterKeys.REALM}", 'calculateFileChecksumMD5', "${JobParameterKeys.REALM}")
    }

    job('transferMergedBamFile', 'transferMergedBamFileJob') {
        outputParameter("${JobParameterKeys.PBS_ID_LIST}")
        outputParameter("${JobParameterKeys.REALM}")
    }

    job('transferMergedBamFileWatchdog', 'watchdogJob') {
        inputParameter("${JobParameterKeys.PBS_ID_LIST}", 'transferMergedBamFile', "${JobParameterKeys.PBS_ID_LIST}")
        inputParameter("${JobParameterKeys.REALM}", 'transferMergedBamFile', "${JobParameterKeys.REALM}")
    }

    job('checkMergedBamFileChecksumMD5', 'checkMergedBamFileChecksumMD5Job') {
        outputParameter("${JobParameterKeys.PBS_ID_LIST}")
        outputParameter("${JobParameterKeys.REALM}")
    }

    job('checkMergedBamFileChecksumMD5Watchdog', 'watchdogJob') {
        inputParameter("${JobParameterKeys.PBS_ID_LIST}", 'checkMergedBamFileChecksumMD5', "${JobParameterKeys.PBS_ID_LIST}")
        inputParameter("${JobParameterKeys.REALM}", 'checkMergedBamFileChecksumMD5', "${JobParameterKeys.REALM}")
    }

    job('transferMergedQAResult', 'transferMergedQAResultJob') {
        outputParameter("${JobParameterKeys.PBS_ID_LIST}")
        outputParameter("${JobParameterKeys.REALM}")
    }

    job('transferMergedQAResultWatchdog', 'watchdogJob') {
        inputParameter("${JobParameterKeys.PBS_ID_LIST}", 'transferMergedQAResult', "${JobParameterKeys.PBS_ID_LIST}")
        inputParameter("${JobParameterKeys.REALM}", 'transferMergedQAResult', "${JobParameterKeys.REALM}")
    }

    job('transferSingleLaneQAResult', 'transferSingleLaneQAResultJob') {
        outputParameter("${JobParameterKeys.PBS_ID_LIST}")
        outputParameter("${JobParameterKeys.REALM}")
    }

    job('transferSingleLangeQAResultWatchdog', 'watchdogJob') {
        inputParameter("${JobParameterKeys.PBS_ID_LIST}", 'transferSingleLaneQAResult', "${JobParameterKeys.PBS_ID_LIST}")
        inputParameter("${JobParameterKeys.REALM}", 'transferSingleLaneQAResult', "${JobParameterKeys.REALM}")
    }

    job('checkQaResultsChecksumMD5', 'checkQaResultsChecksumMD5Job') {
        outputParameter("${JobParameterKeys.PBS_ID_LIST}")
        outputParameter("${JobParameterKeys.REALM}")
    }

    job('checkQaResultsChecksumMD5Watchdog', 'watchdogJob') {
        inputParameter("${JobParameterKeys.PBS_ID_LIST}", 'checkQaResultsChecksumMD5', "${JobParameterKeys.PBS_ID_LIST}")
        inputParameter("${JobParameterKeys.REALM}", 'checkQaResultsChecksumMD5', "${JobParameterKeys.REALM}")
    }

    job('createQAResultStatisticsFile', 'createQAResultStatisticsFileJob') {
        outputParameter("${JobParameterKeys.PBS_ID_LIST}")
        outputParameter("${JobParameterKeys.REALM}")
    }

    job('createQAResultStatisticsFileWatchdog', 'watchdogJob') {
        inputParameter("${JobParameterKeys.PBS_ID_LIST}", 'createQAResultStatisticsFile', "${JobParameterKeys.PBS_ID_LIST}")
        inputParameter("${JobParameterKeys.REALM}", 'createQAResultStatisticsFile', "${JobParameterKeys.REALM}")
    }

    job('moveFilesToFinalDestination', 'moveFilesToFinalDestinationJob') {
        outputParameter("${JobParameterKeys.PBS_ID_LIST}")
        outputParameter("${JobParameterKeys.REALM}")
    }

    job('moveFilesToFinalDestinationWatchdog', 'watchdogJob') {
        inputParameter("${JobParameterKeys.PBS_ID_LIST}", 'moveFilesToFinalDestination', "${JobParameterKeys.PBS_ID_LIST}")
        inputParameter("${JobParameterKeys.REALM}", 'moveFilesToFinalDestination', "${JobParameterKeys.REALM}")
    }

    job('storeChecksumOfMergedBamFile', 'storeChecksumOfMergedBamFileJob')
}
