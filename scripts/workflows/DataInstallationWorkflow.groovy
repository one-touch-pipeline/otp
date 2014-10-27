import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("DataInstallationWorkflow") {
    start("start", "dataInstallationStartJob")
    //job("checkInitialDataNotCompressed", "checkInitialDataNotCompressedJob")
    job("checkInputFiles", "checkInputFilesJob")
    //job("compressSequenceFiles", "compressSequenceFilesJob") {
    //    outputParameter("pbsIds")
    //}
    //job("compressSequenceFilesWatchdog", "myPbsWatchdog") {
    //    inputParameter("pbsIds", "compressSequenceFiles", "pbsIDs")
    //}
    job("createOutputDirectory", "createOutputDirectoryJob")
    job("copyFilesToFinalLocation", "copyFilesJob") {
        outputParameter("__pbsIds")
    }
    job("copyFilesToFinalLocationWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "copyFilesToFinalLocation", "__pbsIds")
    }
    // TODO: milestone
    job("checkFinalLocation", "checkFinalLocationJob")
    job("calculateChecksum", "calculateChecksumJob") {
        outputParameter("__pbsIds")
    }
    job("calculateChecksumWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "calculateChecksum", "__pbsIds")
    }
    job("compareChecksum", "compareChecksumJob")
    job("createViewByPid", "createViewByPidJob")
    job("checkViewByPid", "checkViewByPidJob")
    //job("checkArchivingPossible", "checkArchivingPossible")
    //job("archiveInitialData", "archiveInitialDataJob")
    //job("checkFinalArchive", "checkFinalArchive")
}


//picard option for mark duplicates
println ctx.processingOptionService.createOrUpdate(
  "PBS_CalculateChecksumJob",
  null,
  null,
  '{"-l": { walltime: "2:00:00"}}',
  "set the walltime for the CalculateChecksumJob to 2h to get in the faster queue"
)

//picard option for mark duplicates
println ctx.processingOptionService.createOrUpdate(
  "PBS_CopyFilesJob",
  null,
  null,
  '{"-l": { walltime: "2:00:00"}}',
  "set the walltime for the CopyFilesJob to 2h to get in the faster queue"
)

