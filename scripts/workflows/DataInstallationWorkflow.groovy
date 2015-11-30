import static de.dkfz.tbi.otp.job.processing.PbsOptionMergingService.PBS_PREFIX

import de.dkfz.tbi.otp.job.jobs.dataTransfer.*
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys

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
        outputParameter(JobParameterKeys.PBS_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }
    job("copyFilesToFinalLocationWatchdog", "myPBSWatchdogJob") {
        inputParameter(JobParameterKeys.PBS_ID_LIST, "copyFilesToFinalLocation", JobParameterKeys.PBS_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "copyFilesToFinalLocation", JobParameterKeys.REALM)
    }
    // TODO: milestone
    job("checkFinalLocation", "checkFinalLocationJob")
    job("calculateChecksum", "calculateChecksumJob") {
        outputParameter(JobParameterKeys.PBS_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }
    job("calculateChecksumWatchdog", "myPBSWatchdogJob") {
        inputParameter(JobParameterKeys.PBS_ID_LIST, "calculateChecksum", JobParameterKeys.PBS_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "calculateChecksum", JobParameterKeys.REALM)
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
  "${PBS_PREFIX}${CalculateChecksumJob.simpleName}",
  null,
  null,
  '{"-l": { walltime: "12:00:00"}}',
  "set the walltime for the CalculateChecksumJob to 2h to get in the faster queue"
)

//picard option for mark duplicates
println ctx.processingOptionService.createOrUpdate(
  "${PBS_PREFIX}${CopyFilesJob.simpleName}",
  null,
  null,
  '{"-l": { walltime: "12:00:00"}}',
  "set the walltime for the CopyFilesJob to 2h to get in the faster queue"
)

