import de.dkfz.tbi.otp.job.jobs.utils.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

// This JobExecutionPlan-DSL shall only be used to test if it is possible to update the JobExecutionPlan

plan("DataInstallationWorkflow") {
    start("start", "dataInstallationStartJob")
    //job("checkInitialDataNotCompressed", "checkInitialDataNotCompressedJob")
    job("checkInputFiles", "checkInputFilesJob")
    //job("compressSequenceFiles", "compressSequenceFilesJob") {
    //    outputParameter(JobParameterKeys.JOB_ID_LIST)
    //}
    //job("compressSequenceFilesWatchdog", "myPbsWatchdog") {
    //    inputParameter(JobParameterKeys.JOB_ID_LIST, "compressSequenceFiles", JobParameterKeys.JOB_ID_LIST)
    //}
    job("createOutputDirectory", "createOutputDirectoryJob")
    job("copyFilesToFinalLocation", "copyFilesJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
    }
    job("copyFilesToFinalLocationWatchdog", "myPBSWatchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "copyFilesToFinalLocation", JobParameterKeys.JOB_ID_LIST)
    }
    // TODO: milestone
    job("checkFinalLocation", "checkFinalLocationJob")
    job("calculateChecksum", "calculateChecksumJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
    }
    job("calculateChecksumWatchdog", "myPBSWatchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "calculateChecksum", JobParameterKeys.JOB_ID_LIST)
    }
    job("compareChecksum", "compareChecksumJob")
    job("createViewByPid", "createViewByPidJob")
    job("checkViewByPid", "checkViewByPidJob")
    //job("checkArchivingPossible", "checkArchivingPossible")
    //job("archiveInitialData", "archiveInitialDataJob")
    //job("checkFinalArchive", "checkFinalArchive")
}
