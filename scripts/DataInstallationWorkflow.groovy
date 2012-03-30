import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("DataInstallationWorkflow") {
    start("start", "dataInstallationStartJob")
    job("checkInitialDataNotCompressed", "checkInitialDataNotCompressedJob")
    job("checkInputFiles", "checkInputFilesJob")
    job("compressSequenceFiles", "compressSequenceFilesJob") {
        outputParameter("pbsIds")
    }
    job("compressSequenceFilesWatchdog", "myPbsWatchdog") {
        inputParameter("pbsIds", "compressSequenceFiles", "pbsIDs")
    }
    job("createOutputDirectory", "createOutputDirectoryJob")
    job("copyFilesToFinalLocation", "copyFilesToFinalLocationJob") {
        outputParameter("pbsIds")
    }
    job("copyFilesToFinalLocationWatchdog", "myPBSWatchdogJob") {
        inputParameter("pbsIds", "copyFilesToFinalLocation", "pbsIds")
    }
    // TODO: milestone
    job("checkFinalLocation", "checkFinalLocationJob")
    job("calculateChecksum", "calculateChecksumJob") {
        outputParameter("pbsIds")
    }
    job("calculateChecksumWatchdog", "myPBSWatchdogJob") {
        inputParameter("pbsIds", "calculateChecksum", "pbsIds")
    }
    job("compareChecksum", "compareChecksumJob")
    job("createViewByPid", "createViewByPidJob")
    job("checkViewByPid", "checkViewByPidJob")
    job("archiveInitialData", "archiveInitialDataJob")
    job("setInstallationCompleted", "setInstallationCompletedJob")
}
