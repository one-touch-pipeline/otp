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
package de.dkfz.tbi.otp.workflowExecution.cluster

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional

import de.dkfz.roddy.config.JobLog
import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.ngsdata.Realm
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

    ConfigService configService

    ClusterJobService clusterJobService

    ClusterJobHelperService clusterJobHelperService

    ClusterLogDirectoryService clusterLogDirectoryService

    ClusterStatisticService clusterStatisticService

    FileService fileService

    FileSystemService fileSystemService

    JobStatusLoggingFileService jobStatusLoggingFileService

    LogService logService

    List<BEJob> createBeJobsToSend(BatchEuphoriaJobManager jobManager, Realm realm, WorkflowStep workflowStep, List<String> scripts,
                                   Map<JobSubmissionOption, String> jobSubmissionOptions = [:]) {
        Map<JobSubmissionOption, String> combined = clusterJobHelperService.mergeResources(workflowStep.workflowRun.priority, realm, jobSubmissionOptions)
        ResourceSet resourceSet = clusterJobHelperService.createResourceSet(combined)
        String jobName = clusterJobHelperService.constructJobName(workflowStep)

        String logFileName = jobStatusLoggingFileService.constructLogFileLocation(realm, workflowStep)
        String logMessage = jobStatusLoggingFileService.constructMessage(realm, workflowStep)
        File clusterLogDirectory = clusterLogDirectoryService.createAndGetLogDirectory(workflowStep).toFile()

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
                    JobLog.toOneFile(clusterLogDirectory),
                    null,
            )
        }
        logService.addSimpleLogEntry(workflowStep, "Created cluster jobs")
        return beJobs
    }

    @SuppressWarnings("CatchException")
    void sendJobs(BatchEuphoriaJobManager jobManager, WorkflowStep workflowStep, List<BEJob> beJobs) {
        beJobs.each { BEJob job ->
            BEJobResult jobResult = jobManager.submitJob(job)
            if (!jobResult.successful) {
                logService.addSimpleLogEntry(workflowStep, "Failed to submit job: ${job}, try killing associated cluster jobs: ${jobToString(beJobs)}")
                try {
                    jobManager.killJobs(beJobs)
                } catch (Exception e) {
                    logService.addSimpleLogEntry(workflowStep, "Failed to kill jobs after submitting ${job}")
                    throw new SubmitClusterJobException("Failed to kill all jobs after an error occurred submitting the job: ${job}.", e)
                }
                throw new SubmitClusterJobException("An error occurred submitting the job: ${job}. Associated cluster jobs were killed.")
            }
        }
        logService.addSimpleLogEntry(workflowStep, "Submit cluster jobs: ${jobToString(beJobs)}")
    }

    @SuppressWarnings("CatchException")
    void startJob(BatchEuphoriaJobManager jobManager, WorkflowStep workflowStep, List<BEJob> beJobs) {
        try {
            jobManager.startHeldJobs(beJobs)
        } catch (Exception e) {
            logService.addSimpleLogEntry(workflowStep, "Failed to kill jobs after failed to start jobs: ${jobToString(beJobs)}")
            try {
                jobManager.killJobs(beJobs)
            } catch (Exception ignored) {
                logService.addSimpleLogEntry(workflowStep, "Failed to start jobs: ${jobToString(beJobs)}")
                throw new StartClusterJobException("Failed to kill jobs after failing starting job: ${jobToString(beJobs)}", e)
            }
            throw new StartClusterJobException("An error occurred when starting jobs: ${jobToString(beJobs)}", e)
        }
        logService.addSimpleLogEntry(workflowStep, "Started cluster jobs: ${jobToString(beJobs)}")
    }

    void collectJobStatistics(Realm realm, WorkflowStep workflowStep, List<BEJob> beJobs) {
        String sshUser = configService.sshUser
        beJobs.each { BEJob job ->
            ClusterJob clusterJob = clusterJobService.createClusterJob(
                    realm, job.jobID.shortID, sshUser, workflowStep, job.jobName
            )
            clusterStatisticService.retrieveAndSaveJobInformationAfterJobStarted(clusterJob)

            logService.addSimpleLogEntry(workflowStep, "LogFile: ${clusterJob.jobLog}")
        }
        logService.addSimpleLogEntry(workflowStep, "Collected cluster job statistic: ${jobToString(beJobs)}")
    }

    private String jobToString(List<BEJob> jobs) {
        return jobs*.jobID*.shortID.join(', ')
    }
}
