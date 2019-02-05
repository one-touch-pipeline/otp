/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
