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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CollectionUtils

class ClusterJobMonitorSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                ClusterJob,
                JobExecutionPlan,
                ProcessingStep,
                Realm,
        ]
    }

    ClusterJobMonitor monitor

    void setup() {
        monitor = new ClusterJobMonitor()
    }

    @Unroll
    void "test findAllClusterJobsToCheck"() {
        given:
        ClusterJob clusterJob = DomainFactory.createClusterJob(
                checkStatus: checkStatus,
                oldSystem: oldSystem,
                workflowStep: oldSystem ? null : createWorkflowStep(),
                processingStep: oldSystem ? DomainFactory.createProcessingStep() : null,
        )

        expect:
        CollectionUtils.atMostOneElement(monitor.findAllClusterJobsToCheck()) == clusterJob == found

        where:
        checkStatus                     | oldSystem || found
        ClusterJob.CheckStatus.CREATED  | true      || false
        ClusterJob.CheckStatus.CHECKING | true      || false
        ClusterJob.CheckStatus.FINISHED | true      || false
        ClusterJob.CheckStatus.CREATED  | false     || false
        ClusterJob.CheckStatus.CHECKING | false     || true
        ClusterJob.CheckStatus.FINISHED | false     || false
    }

    @Unroll
    void "test handleFinishedClusterJobs"() {
        given:
        monitor.jobService = Mock(JobService)
        WorkflowStep workflowStep = createWorkflowStep()
        ClusterJob finishedClusterJob = DomainFactory.createClusterJob(checkStatus: ClusterJob.CheckStatus.FINISHED, workflowStep: workflowStep, oldSystem: false, processingStep: null)
        DomainFactory.createClusterJob(checkStatus: checkStatus, workflowStep: workflowStep, oldSystem: false, processingStep: null)

        when:
        monitor.handleFinishedClusterJobs(finishedClusterJob)

        then:
        createNextJob * monitor.jobService.createNextJob(finishedClusterJob.workflowStep.workflowRun) >> { }

        where:
        checkStatus                     || createNextJob
        ClusterJob.CheckStatus.CREATED  || 0
        ClusterJob.CheckStatus.CHECKING || 0
        ClusterJob.CheckStatus.FINISHED || 1
    }
}
