import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("DataTransferWorkflow") {
    start("start", "dataTransferStartJob")
    job("checkInputFiles", "checkInputFilesJob")
    job("createOutputDirectory", "createOutputDirectoryJob") {
        constantParameter("project", "PROJECT_NAME")
    }
    job("copyFiles", "copyFilesJob") {
        outputParameter("pbsIds")
    }
    job("copyFilesWatchdog", "myPBSWatchdogJob") {
        inputParameter("pbsIds", "copyFiles", "pbsIds")
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
}
