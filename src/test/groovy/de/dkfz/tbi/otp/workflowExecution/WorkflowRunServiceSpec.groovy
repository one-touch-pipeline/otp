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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project

class WorkflowRunServiceSpec extends Specification implements ServiceUnitTest<WorkflowRunService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqTrack,
                WorkflowRun,
        ]
    }

    void 'countOfRunningWorkflows, return all workflows in state RUNNING and WAITING'() {
        given:
        int countOfRunningWorkflowStates = WorkflowRunService.STATES_COUNTING_AS_RUNNING.size()
        WorkflowRun.State.values().each {
            createWorkflowRun(state: it)
        }

        when:
        int count = service.countOfRunningWorkflows()

        then:
        count == countOfRunningWorkflowStates
    }

    void 'buildWorkflowRun, when created, then the values should be correct'() {
        given:
        service.configFragmentService = Mock(ConfigFragmentService) {
            mergeSortedFragments(_) >> '{"config":"combined"}'
        }
        Workflow workflow = createWorkflow()
        SeqTrack seqTrack = createSeqTrack()
        Project project = createProject()
        String dir = "/tmp/baseDir${nextId}"
        List<String> multiLineName = ["asdf", "xyz"]
        String shortName = "DI: asdf"

        when:
        WorkflowRun run = service.buildWorkflowRun(workflow, seqTrack.processingPriority, dir, project, multiLineName, shortName)

        then:
        run
        run.workDirectory == dir
        run.combinedConfig == null
        run.workflow == workflow
        run.state == WorkflowRun.State.PENDING
        run.restartedFrom == null
        run.omittedMessage == null
        run.workflowSteps == []
        run.displayName == multiLineName[0] + "\n" + multiLineName[1]
        run.shortDisplayName == shortName
    }

    @Unroll
    void 'saveCombinedConfig, when called with #combinedConfig, then update the combinedConfig'() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()

        when:
        service.saveCombinedConfig(workflowRun.id, combinedConfig)

        then:
        workflowRun.refresh()
        workflowRun.combinedConfig == combinedConfig

        where:
        combinedConfig | _
        '{key: value}' | _
        null           | _
    }

    void 'saveCombinedConfig, when called with invalid json, then throw a validationException'() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        String combinedConfig = "{'invalid':'invalid':'invalid'}"

        when:
        service.saveCombinedConfig(workflowRun.id, combinedConfig)

        then:
        thrown(ValidationException)
    }

    void 'saveCombinedConfig, when called with unknown id, then throw a Assertion'() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        String combinedConfig = '{key: value}'

        when:
        service.saveCombinedConfig(workflowRun.id - 1, combinedConfig)

        then:
        thrown(AssertionError)
    }

    void 'markJobAsNotRestartableInSeparateTransaction, when call, then change mayJobRestarted to true'() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        assert workflowRun.jobCanBeRestarted

        when:
        service.markJobAsNotRestartableInSeparateTransaction(workflowRun)

        then:
        !workflowRun.jobCanBeRestarted
    }

    @Unroll
    void 'getCumulatedClusterJobsStatus, should return state of a cluster job list with highest priority'() {
        when:
        List<ClusterJobStateDto> clusterJobStatusList = statusList.collect {
            it as ClusterJobStateDto
        }

        then:
        service.getCumulatedClusterJobsStatus(clusterJobStatusList) == resultStatus

        where:
        statusList                                                                                | resultStatus
        [[checkStatus: ClusterJob.CheckStatus.CHECKING, exitStatus: null],
         [checkStatus: ClusterJob.CheckStatus.CREATED, exitStatus: null],
         [checkStatus: ClusterJob.CheckStatus.FINISHED, exitStatus: ClusterJob.Status.COMPLETED]] | 'CHECKING'
        [[checkStatus: ClusterJob.CheckStatus.CREATED, exitStatus: null],
         [checkStatus: ClusterJob.CheckStatus.FINISHED, exitStatus: ClusterJob.Status.FAILED]]    | 'CREATED'
        [[checkStatus: ClusterJob.CheckStatus.FINISHED, exitStatus: ClusterJob.Status.COMPLETED],
         [checkStatus: ClusterJob.CheckStatus.FINISHED, exitStatus: ClusterJob.Status.FAILED]]    | 'FINISHED/FAILED'
        [[checkStatus: ClusterJob.CheckStatus.FINISHED, exitStatus: ClusterJob.Status.COMPLETED],
         [checkStatus: ClusterJob.CheckStatus.FINISHED, exitStatus: ClusterJob.Status.COMPLETED]] | 'FINISHED/COMPLETED'
    }

    void 'getCumulatedClusterJobsStatus, should return empty string for null object or empty list'() {
        expect:
        service.getCumulatedClusterJobsStatus([]) == ''
        service.getCumulatedClusterJobsStatus(null) == ''
    }
}
