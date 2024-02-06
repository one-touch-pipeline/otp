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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.workflow.shared.RunningClusterJobException
import de.dkfz.tbi.otp.workflowExecution.*

class ClusterAccessServiceSpec extends Specification implements ServiceUnitTest<ClusterAccessService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ClusterJob,
                WorkflowStep,
        ]
    }

    void "executeJobs, when multiple scripts given, then all methods of preparation, sending and starting are called and for each a cluster id is returned"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        BatchEuphoriaJobManager batchEuphoriaJobManager = Mock(BatchEuphoriaJobManager)

        List<String> scripts = (1..5).collect { "script ${nextId}" }
        List<BEJob> beJobs = scripts.collect { new BEJob(new BEJobID("${nextId}"), batchEuphoriaJobManager) }
        List<String> ids = beJobs*.jobID*.shortID
        List<ClusterJob> clusterJobs = ids.collect {
            createClusterJob([
                    clusterJobId: it,
                    workflowStep: workflowStep,
                    checkStatus : ClusterJob.CheckStatus.FINISHED,
            ])
        }

        service.clusterJobManagerFactoryService = Mock(ClusterJobManagerFactoryService) {
            1 * getJobManager() >> batchEuphoriaJobManager
            0 * _
        }
        service.clusterJobHandlingService = Mock(ClusterJobHandlingService) {
            1 * createBeJobsToSend(batchEuphoriaJobManager, workflowStep, scripts, [:]) >> beJobs
            1 * sendJobs(batchEuphoriaJobManager, workflowStep, beJobs)
            1 * startJob(batchEuphoriaJobManager, workflowStep, beJobs)
            1 * createAndSaveClusterJobs(workflowStep, beJobs) >> clusterJobs
            1 * collectJobStatistics(workflowStep, clusterJobs)
            1 * startMonitorClusterJob(workflowStep, clusterJobs)
            0 * _
        }
        service.workflowRunService = Mock(WorkflowRunService) {
            1 * markJobAsNotRestartableInSeparateTransaction(workflowStep.workflowRun)
            1 * markJobAsRestartable(workflowStep.workflowRun)
            0 * _
        }
        service.logService = Mock(LogService)

        when:
        List<String> result = service.executeJobs(workflowStep, scripts)

        then:
        result == ids
    }

    void "executeJobs, when no scripts are given, then throw exception"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        service.executeJobs(workflowStep, [])

        then:
        thrown(NoScriptsGivenWorkflowException)
    }

    void "executeJobs, when workflowRun has running cluster jobs, then throw a RunningClusterJobException"() {
        given:
        WorkflowStep previous = createWorkflowStep()
        WorkflowStep workflowStep = createWorkflowStep([
                previous: previous,
        ])
        createClusterJob([
                workflowStep: previous,
                checkStatus : ClusterJob.CheckStatus.CHECKING,
        ])

        List<String> scripts = ["script ${nextId}"]

        service.clusterJobManagerFactoryService = Mock(ClusterJobManagerFactoryService) {
            0 * _
        }
        service.clusterJobHandlingService = Mock(ClusterJobHandlingService) {
            0 * _
        }
        service.workflowRunService = Mock(WorkflowRunService) {
            0 * _
        }
        service.logService = Mock(LogService) {
            0 * _
        }

        when:
        service.executeJobs(workflowStep, scripts)

        then:
        thrown(RunningClusterJobException)
    }
}
