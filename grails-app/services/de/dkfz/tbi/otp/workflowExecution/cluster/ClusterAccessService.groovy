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

import de.dkfz.roddy.execution.jobs.BEJob
import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExecutedCommandLogCallbackThreadLocalHolder
import de.dkfz.tbi.otp.workflow.shared.RunningClusterJobException
import de.dkfz.tbi.otp.workflowExecution.*

/**
 * Service to send multiple scripts together to the cluster. For each job a cluster id will be returned.
 */
@GrailsCompileStatic
@Transactional
class ClusterAccessService {

    ClusterJobHandlingService clusterJobHandlingService

    ClusterJobManagerFactoryService clusterJobManagerFactoryService

    LogService logService

    WorkflowRunService workflowRunService

    List<String> executeJobs(Realm realm, WorkflowStep workflowStep, List<String> scripts, Map<JobSubmissionOption, String> jobSubmissionOptions = [:])
            throws Throwable {
        if (!scripts) {
            throw new NoScriptsGivenWorkflowException("No job scripts specified.")
        }
        assert realm: 'No realm specified.'

        ensureNoClusterJobIsInChecking(workflowStep)

        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(realm)

        List<BEJob> beJobs = clusterJobHandlingService.createBeJobsToSend(jobManager, realm, workflowStep, scripts, jobSubmissionOptions)

        //begin of not restartable area
        workflowRunService.markJobAsNotRestartableInSeparateTransaction(workflowStep.workflowRun)

        ExecutedCommandLogCallbackThreadLocalHolder.withCommandLogCallback(new WorkflowStepCommandCallback(logService, workflowStep)) {
            clusterJobHandlingService.sendJobs(jobManager, workflowStep, beJobs)

            clusterJobHandlingService.startJob(jobManager, workflowStep, beJobs)
        }

        List<ClusterJob> clusterJobs = clusterJobHandlingService.createAndSaveClusterJobs(realm, workflowStep, beJobs)

        clusterJobHandlingService.collectJobStatistics(workflowStep, clusterJobs)

        clusterJobHandlingService.startMonitorClusterJob(workflowStep, clusterJobs)

        //end of not restartable area
        workflowStep.workflowRun.with {
            //its done in same transaction in which cluster jobs are saved
            jobCanBeRestarted = true
            save(flush: true)
        }

        List<String> ids = beJobs*.jobID*.shortID
        logService.addSimpleLogEntry(workflowStep, "Executed jobs and got job IDs: ${ids.join(', ')}")

        return ids
    }

    private void ensureNoClusterJobIsInChecking(WorkflowStep workflowStep) {
        List<ClusterJob> clusterJobs = workflowStep.workflowRun.workflowSteps*.clusterJobs.flatten() as List<ClusterJob>
        List<ClusterJob> runningClusterJobs = clusterJobs.findAll { ClusterJob clusterJob ->
            clusterJob.checkStatus != ClusterJob.CheckStatus.FINISHED
        }

        if (runningClusterJobs) {
            String header = "id, clusterId, checkStatus"
            String jobInfos = runningClusterJobs.collect {
                [
                        it.id,
                        it.clusterJobId,
                        it.checkStatus,
                ].join(', ')
            }.join('\n')
            throw new RunningClusterJobException("The workflow has still ${runningClusterJobs.size()} cluster jobs in checkStatus " +
            "${ClusterJob.CheckStatus.CREATED.name()} or  {ClusterJob.CheckStatus.CHECKING.name()}." +
                    "As long as cluster jobs are in these states, the job cannot be restarted." +
                    "\nCurrently the following jobs are affected:\n${header}\n${jobInfos}")
        }
    }
}
