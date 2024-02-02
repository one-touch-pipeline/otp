/*
 * Copyright 2011-2024 The OTP authors
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

import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.workflow.RoddyService
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepService

/**
 * Base job to do validation after an Roddy pipeline {@link AbstractExecuteRoddyPipelineJob} has run.
 *
 * It provides the implementation of {@link #ensureExternalJobsRunThrough} for the {@link AbstractExecuteRoddyPipelineJob}
 */
abstract class AbstractRoddyClusterValidationJob extends AbstractValidationJob {

    @Autowired
    RoddyService roddyService

    @Autowired
    WorkflowStepService workflowStepService

    /**
     * After Roddy job is submitted and executed, its log file (#JobStateLogFile)
     * is checked to see if
     * <p><ul>
     * <li>cluster job id can be found in the file, or
     * <li>cluster job is marked as successfully finished
     * </ul><p>
     * Any error found (validation failed) leads to an entry in OTP log file
     * and ValidationJobFailedException thrown, which will be handled by centrally
     *
     * @param workflowStep to be validated
     * @throws ValidationJobFailedException if Roddy job validation fails
     */
    @CompileDynamic
    @Override
    protected void ensureExternalJobsRunThrough(WorkflowStep workflowStep) {
        // cluster jobs are connected to the job sending them, not to this validation job
        Set<ClusterJob> clusterJobs = workflowStepService.getPreviousRunningWorkflowStep(workflowStep).clusterJobs
        if (!clusterJobs) {
            logService.addSimpleLogEntry(workflowStep, "No cluster job found to be validated.")
            return
        }

        JobStateLogFile jobStateLogFile = roddyService.getJobStateLogFile(workflowStep)
        List<String> errorMessages = []
        clusterJobs.each { ClusterJob clusterJob ->
            if (!jobStateLogFile.containsClusterJobId(clusterJob.clusterJobId)) {
                errorMessages.add("JobStateLogFile contains no information for this cluster job ${clusterJob.clusterJobId}.")
            } else if (!jobStateLogFile.isClusterJobFinishedSuccessfully(clusterJob.clusterJobId)) {
                errorMessages.add("Status code of cluster job ${clusterJob.clusterJobId}: " +
                        "${jobStateLogFile.getPropertyFromLatestLogFileEntry(clusterJob.clusterJobId, "statusCode")}.")
            }
        }

        if (errorMessages.empty) {
            logService.addSimpleLogEntry(workflowStep, "All cluster jobs have finished successfully.")
        } else {
            String message = "${errorMessages.size()} errors occured in the Workflowstep {workflowStep}:\n${errorMessages.join('\n')}."
            logService.addSimpleLogEntry(workflowStep, message)
            throw new ValidationJobFailedException(message)
        }
    }
}
