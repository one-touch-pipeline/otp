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
package de.dkfz.tbi.otp.workflowExecution.cluster

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional

import de.dkfz.roddy.BEException
import de.dkfz.roddy.config.JobLog
import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.cluster.logs.ClusterLogDirectoryService
import de.dkfz.tbi.otp.workflowExecution.cluster.logs.JobStatusLoggingFileService

/**
 * A helper service for {@link ClusterAccessService}. Its only separate to simplify testing.
 *
 * It executes job submission/monitoring commands on the cluster head node using
 * the <a href="https://github.com/eilslabs/BatchEuphoria">BatchEuphoria</a> library.
 */
@GrailsCompileStatic
@Transactional
class ClusterJobHandlingService {

    private static final String NESTED_FAIL_MESSAGE_FOR_KILL = "Failed to kill associated jobs following an error submitting the job"

    ConfigService configService

    ClusterJobService clusterJobService

    ClusterJobHelperService clusterJobHelperService

    ClusterLogDirectoryService clusterLogDirectoryService

    ClusterStatisticService clusterStatisticService

    FileService fileService

    FileSystemService fileSystemService

    JobStatusLoggingFileService jobStatusLoggingFileService

    LogService logService

    List<BEJob> createBeJobsToSend(BatchEuphoriaJobManager jobManager, WorkflowStep workflowStep, List<String> scripts,
                                   Map<JobSubmissionOption, String> jobSubmissionOptions = [:]) {
        logService.addSimpleLogEntry(workflowStep, "Start preparing ${scripts.size()} scripts for sending to cluster")
        Map<JobSubmissionOption, String> combined = clusterJobHelperService.mergeResources(workflowStep.workflowRun.priority, jobSubmissionOptions)
        ResourceSet resourceSet = clusterJobHelperService.createResourceSet(combined)
        String jobName = clusterJobHelperService.constructJobName(workflowStep)

        String logFileName = jobStatusLoggingFileService.constructLogFileLocation(workflowStep)
        String logMessage = jobStatusLoggingFileService.constructMessage(workflowStep)
        File clusterLogDirectory = fileService.toFile(clusterLogDirectoryService.createAndGetLogDirectory(workflowStep))

        List<BEJob> beJobs = scripts.collect {
            new BEJob(
                    null,
                    jobName,
                    null,
                    clusterJobHelperService.wrapScript(it, logFileName, logMessage),
                    null,
                    resourceSet,
                    [],
                    [:],
                    jobManager,
                    JobLog.toOneFile(new File(clusterLogDirectory, "${jobName}-{JOB_ID}.log")),
                    null,
            )
        }
        logService.addSimpleLogEntry(workflowStep, "Finish preparing ${scripts.size()} scripts for sending to cluster")
        return beJobs
    }

    void sendJobs(BatchEuphoriaJobManager jobManager, WorkflowStep workflowStep, List<BEJob> beJobs) {
        logService.addSimpleLogEntry(workflowStep, "Begin submiting ${beJobs.size()} jobs to cluster: ${jobToString(beJobs)}")
        beJobs.each { BEJob job ->
            BEJobResult jobResult
            try {
                jobResult = jobManager.submitJob(job)
            } catch (BEException bEException) {
                logService.addSimpleLogEntryWithException(workflowStep, "Exception occured during submiting job:\n${job}", bEException)
                killFailedClusterJobs(workflowStep, job, beJobs, jobManager)
            }
            if (!jobResult.successful) {
                logService.addSimpleLogEntry(workflowStep, "Failed to submit job:\n${job}")
                killFailedClusterJobs(workflowStep, job, beJobs, jobManager)
            }
        }
        logService.addSimpleLogEntry(workflowStep, "Finish submiting ${beJobs.size()} jobs to cluster: ${jobToString(beJobs)}")
    }

    @SuppressWarnings("CatchException")
    private void killFailedClusterJobs(WorkflowStep workflowStep, BEJob job, List<BEJob> beJobs, BatchEuphoriaJobManager jobManager) {
        List<BEJob> startedBeJobs = beJobs.findAll {
            it.runResult
        }
        logService.addSimpleLogEntry(workflowStep, "Try killing associated cluster jobs: ${jobToString(startedBeJobs)}")
        try {
            jobManager.killJobs(startedBeJobs)
        } catch (Exception e) {
            String message = "${NESTED_FAIL_MESSAGE_FOR_KILL}:\n${job}."
            logService.addSimpleLogEntryWithException(workflowStep, message, e)
            throw new KillClusterJobException(message, e)
        }
        throw new SubmitClusterJobException("An error occurred submitting the job: ${job}. Associated cluster jobs were killed.")
    }

    @SuppressWarnings("CatchException")
    void startJob(BatchEuphoriaJobManager jobManager, WorkflowStep workflowStep, List<BEJob> beJobs) {
        logService.addSimpleLogEntry(workflowStep, "Begin starting ${beJobs.size()} cluster jobs: ${jobToString(beJobs)}")
        try {
            jobManager.startHeldJobs(beJobs)
        } catch (Exception e) {
            logService.addSimpleLogEntry(workflowStep, "Failed to start jobs: ${jobToString(beJobs)}\nException: ${e.message}")
            logService.addSimpleLogEntry(workflowStep, "Try killing associated cluster jobs: ${jobToString(beJobs)}")
            try {
                jobManager.killJobs(beJobs)
            } catch (Exception e2) {
                logService.addSimpleLogEntry(workflowStep, "Failed to kill jobs after failed to start jobs: ${jobToString(beJobs)}\n" +
                        "Exception: ${e2.message}")
                throw new KillClusterJobException("Failed to kill jobs after failing starting jobs: ${jobToString(beJobs)}", e)
            }
            throw new StartClusterJobException("An error occurred when starting jobs: ${jobToString(beJobs)}", e)
        }
        logService.addSimpleLogEntry(workflowStep, "Finish starting ${beJobs.size()} cluster jobs: ${jobToString(beJobs)}")
    }

    List<ClusterJob> createAndSaveClusterJobs(WorkflowStep workflowStep, List<BEJob> beJobs) {
        logService.addSimpleLogEntry(workflowStep, "Begin creating  ${beJobs.size()} cluster job statistic: ${jobToString(beJobs)}")
        String sshUser = configService.sshUser
        List<ClusterJob> clusterJobs = beJobs.collect { BEJob job ->
            clusterJobService.createClusterJob(job.jobID.shortID, sshUser, workflowStep, job.jobName)
        }
        logService.addSimpleLogEntry(workflowStep, "Finish creating ${beJobs.size()} cluster job statistic: ${jobToString(beJobs)}")
        return clusterJobs
    }

    void collectJobStatistics(WorkflowStep workflowStep, List<ClusterJob> clusterJobs) {
        logService.addSimpleLogEntry(workflowStep, "Begin collecting  ${clusterJobs.size()} cluster job statistic: ${clusterJobToString(clusterJobs)}")
        clusterJobs.collect { ClusterJob clusterJob ->
            clusterStatisticService.retrieveAndSaveJobInformationAfterJobStarted(clusterJob)
            logService.addSimpleLogEntry(workflowStep, "LogFile: ${clusterJob.jobLog}")
        }
        logService.addSimpleLogEntry(workflowStep, "Finish collecting ${clusterJobs.size()} cluster job statistic: ${clusterJobToString(clusterJobs)}")
    }

    void startMonitorClusterJob(WorkflowStep workflowStep, List<ClusterJob> clusterJobs) {
        logService.addSimpleLogEntry(workflowStep, "Begin starting of checking of cluster jobs: ${clusterJobToString(clusterJobs)}")
        clusterJobs.each { ClusterJob clusterJob ->
            clusterJob.checkStatus = ClusterJob.CheckStatus.CHECKING
            clusterJob.save(flush: true)
        }
        logService.addSimpleLogEntry(workflowStep, "All cluster jobs are now checked by the cluster monitor: ${clusterJobToString(clusterJobs)}")
    }

    private String jobToString(List<BEJob> jobs) {
        return jobs*.jobID*.shortID.join(', ')
    }

    private String clusterJobToString(List<ClusterJob> jobs) {
        return jobs*.clusterJobId.join(', ')
    }
}
