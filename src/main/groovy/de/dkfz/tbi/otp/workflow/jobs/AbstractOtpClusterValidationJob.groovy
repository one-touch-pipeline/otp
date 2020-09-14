/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflow.jobs

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*
import java.util.regex.Pattern

/**
 * Base job to do validation after an OTP pipeline {@link AbstractOtpClusterValidationJob} has run.
 */
abstract class AbstractOtpClusterValidationJob extends AbstractValidationJob {

    /**
     * checks, that all cluster jobs has created the job status log file with the correct content.
     *
     * Also it checks, that the {@link ClusterJob#checkStatus} is {@link ClusterJob.CheckStatus.FINISHED}
     */
    @Override
    protected void ensureExternalJobsRunThrough(WorkflowStep workflowStep) {
        Realm realm = workflowStep.realm
        Collection<ClusterJob> clusterJobs = workflowStep.clusterJobs
        FileSystem fileSystem = getFileSystem(workflowStep)
        logService.addSimpleLogEntry(workflowStep, "Start checking of ${clusterJobs.size()} cluster jobs")

        List<String> problems = clusterJobs.sort {
            it.clusterJobId
        }.collect { ClusterJob clusterJob ->
            checkSingleClusterJobAndReturnErrorMessage(realm, fileSystem, workflowStep, clusterJob)
        }.findAll()

        if (problems.isEmpty()) {
            logService.addSimpleLogEntry(workflowStep, "All ${clusterJobs.size()} cluster jobs have finished successfully (run till the end).")
        } else {
            String message = "${problems.size()} Cluster jobs have problems:\n- ${problems.join('\n- ')}\n\n" +
                    "${clusterJobs.size() - problems.size()} jobs finished successfully"
            logService.addSimpleLogEntry(workflowStep, message)
            throw new ValidationJobFailedException(message)
        }
    }

    private String checkSingleClusterJobAndReturnErrorMessage(Realm realm, FileSystem fileSystem, WorkflowStep workflowStep, ClusterJob clusterJob) {
        if (clusterJob.checkStatus != ClusterJob.CheckStatus.FINISHED) {
            return "Cluster job ${clusterJob.clusterJobId} is in state ${clusterJob.checkStatus} instead of FINISHED"
        }

        Path logFile = fileSystem.getPath(jobStatusLoggingFileService.constructLogFileLocation(realm, workflowStep, clusterJob.clusterJobId))
        if (!Files.exists(logFile)) {
            return "Cluster job ${clusterJob.clusterJobId} status log file ${logFile} does not exist."
        }
        if (!Files.isReadable(logFile)) {
            return "Cluster job ${clusterJob.clusterJobId} status log file ${logFile} can not be read."
        }

        String expectedLogMessage = jobStatusLoggingFileService.constructMessage(realm, workflowStep, clusterJob.clusterJobId)
        String logFileText = logFile.text
        return (logFileText =~ /(?:^|\s)${Pattern.quote(expectedLogMessage)}(?:$|\s)/) ? null :
            "Did not find \"${expectedLogMessage}\" in ${logFile}."
    }
}
