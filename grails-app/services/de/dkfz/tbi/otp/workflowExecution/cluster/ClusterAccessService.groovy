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
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Service to send multiple scripts together to the cluster. For each job a cluster id will be returned.
 */
@GrailsCompileStatic
@Transactional
class ClusterAccessService {

    ClusterJobHandlingService clusterJobHandlingService

    ClusterJobManagerFactoryService clusterJobManagerFactoryService

    LogService logService

    List<String> executeJobs(Realm realm, WorkflowStep workflowStep, List<String> scripts, Map<JobSubmissionOption, String> jobSubmissionOptions = [:])
            throws Throwable {
        if (!scripts) {
            throw new NoScriptsGivenWorkflowException("No job scripts specified.")
        }
        assert realm: 'No realm specified.'

        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(realm)

        List<BEJob> beJobs = clusterJobHandlingService.createBeJobsToSend(jobManager, realm, workflowStep, scripts, jobSubmissionOptions)

        clusterJobHandlingService.sendJobs(jobManager, workflowStep, beJobs)

        clusterJobHandlingService.startJob(jobManager, workflowStep, beJobs)

        List<ClusterJob> clusterJobs = clusterJobHandlingService.collectJobStatistics(realm, workflowStep, beJobs)

        clusterJobHandlingService.startMonitorClusterJob(workflowStep, clusterJobs)

        List<String> ids = beJobs*.jobID*.shortID
        logService.addSimpleLogEntry(workflowStep, "Executed jobs and got job IDs: ${ids.join(', ')}")

        return ids
    }
}
