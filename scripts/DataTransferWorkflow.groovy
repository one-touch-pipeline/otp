import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("DataTransferWorkflow") {
    start("start", "dataTransferStartJob")
    job("checkInputFiles", "checkInputFiles")
    job("createOutputDirectory", "createOutputDirectory") {
        constantParameter("project", "PROJECT_NAME")
    }
    job("copyFiles", "copyFiles")
    job("copyFilesWatchdog", "myPbsWatchdog")
    // TODO: milestone
    job("checkFinalLocation", "checkFinalLocation")
    job("createViewByPid", "createViewByPid")
    job("checkViewByPid", "checkViewByPid")
}
