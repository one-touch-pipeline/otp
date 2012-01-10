import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("DataTransferWorkflow") {
    start("start", "dataTransferStartJob")
    job("checkInputFiles", "checkInputFilesJob")
    job("createOutputDirectory", "createOutputDirectoryJob") {
        constantParameter("project", "PROJECT_NAME")
    }
    job("copyFiles", "copyFilesJob")
    job("copyFilesWatchdog", "myPbsWatchdog")
    // TODO: milestone
    job("checkFinalLocation", "checkFinalLocationJob")
    job("createViewByPid", "createViewByPidJob")
    job("checkViewByPid", "checkViewByPidJob")
}
