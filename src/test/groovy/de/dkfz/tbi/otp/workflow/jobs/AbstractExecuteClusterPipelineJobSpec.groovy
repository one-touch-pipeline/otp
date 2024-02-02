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

import grails.testing.gorm.DataTest
import groovy.transform.TupleConstructor
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.JobSubmissionOption
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterAccessService

class AbstractExecuteClusterPipelineJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    private WorkflowStep workflowStep
    private AbstractExecuteClusterPipelineJob job

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowArtefact,
                WorkflowStep,
        ]
    }

    @TupleConstructor
    private class TestAbstractExecuteClusterPipelineJob extends AbstractExecuteClusterPipelineJob {

        List<String> scripts

        @Override
        List<String> createScripts(WorkflowStep workflowStep) {
            return scripts
        }
    }

    private void setupData(List<String> scripts) {
        workflowStep = createWorkflowStep()
        job = new TestAbstractExecuteClusterPipelineJob(scripts)
    }

    void "execute, when no scripts return, then do not submit cluster jobs and change state to success"() {
        given:
        setupData([])
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToSuccess(workflowStep)
            0 * _
        }
        job.logService = Mock(LogService)

        when:
        job.execute(workflowStep)

        then:
        true
    }

    void "execute, when scripts return, then submit cluster jobs and change state to wait for system"() {
        given:
        List<String> scripts = [
                "script ${nextId}",
                "script ${nextId}",
        ]
        setupData(scripts)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToWaitingOnSystem(workflowStep)
            0 * _
        }
        job.clusterAccessService = Mock(ClusterAccessService) {
            1 * executeJobs(workflowStep, scripts, _)
        }
        job.logService = Mock(LogService)

        when:
        job.execute(workflowStep)

        then:
        true
    }

    void "execute, pass correct jobSubmissionOptions"() {
        given:
        List<String> scripts = [
                "script ${nextId}",
                "script ${nextId}",
        ]
        setupData(scripts)
        job.clusterAccessService = Mock(ClusterAccessService)
        job.logService = Mock(LogService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)

        workflowStep.workflowRun.combinedConfig = '{"OTP_CLUSTER": {"CORES": "cores_1", "NODE_FEATURE": "node_2"}}'
        workflowStep.workflowRun.save(flush: true)

        when:
        job.execute(workflowStep)

        then:
        1 * job.clusterAccessService.executeJobs(workflowStep, scripts,
                [(JobSubmissionOption.NODE_FEATURE): 'node_2', (JobSubmissionOption.CORES): 'cores_1'])
    }
}
