/*
 * Copyright 2011-2026 The OTP authors
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
import de.dkfz.tbi.otp.utils.TimeUtils

import java.time.ZonedDateTime

class WorkflowRunServiceSpec extends Specification implements ServiceUnitTest<WorkflowRunService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqTrack,
                WorkflowRun,
                WorkflowStep,
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
        run.skipMessage == null
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
        when:
        String status1 = service.getCumulatedClusterJobsStatus([])
        String status2 = service.getCumulatedClusterJobsStatus(null)

        then:
        status1 == ''
        status2 == ''
    }

    void 'calculateDuration, when firstJobStarted is null, return dash'() {
        given:
        WorkflowRun workflowRun = createWorkflowRun(state: WorkflowRun.State.SUCCESS)

        when:
        String duration = service.calculateDuration(workflowRun)

        then:
        duration == "-"
    }

    @Unroll
    void 'calculateDuration, when run is in running state #state and firstJobStarted is set, return formatted duration'() {
        given:
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(5)
        WorkflowRun workflowRun = createWorkflowRun(state: state, firstJobStarted: start)

        when:
        String duration = service.calculateDuration(workflowRun)

        then:
        duration != "-"

        where:
        state << [WorkflowRun.State.PENDING, WorkflowRun.State.RUNNING_OTP, WorkflowRun.State.RUNNING_WES]
    }

    @Unroll
    void 'calculateDuration, when run is finished with state #state and lastJobFinished is set, return duration between both timestamps'() {
        given:
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(10)
        ZonedDateTime finish = ZonedDateTime.now().minusMinutes(2)
        WorkflowRun workflowRun = createWorkflowRun(state: state, firstJobStarted: start, lastJobFinished: finish)

        when:
        String duration = service.calculateDuration(workflowRun)

        then:
        duration == TimeUtils.getFormattedDurationForZonedDateTime(start, finish)

        where:
        state << [
                WorkflowRun.State.SUCCESS,
                WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
                WorkflowRun.State.FAILED_FINAL,
                WorkflowRun.State.RESTARTED,
                WorkflowRun.State.KILLED,
        ]
    }

    void 'calculateDuration, when run is finished but lastJobFinished is null, return dash'() {
        given:
        WorkflowRun workflowRun = createWorkflowRun(state: WorkflowRun.State.SUCCESS, firstJobStarted: ZonedDateTime.now().minusMinutes(5))

        when:
        String duration = service.calculateDuration(workflowRun)

        then:
        duration == "-"
    }

    void 'calculateStepDuration, when jobStarted is null, return dash'() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.SUCCESS)

        when:
        String duration = service.calculateStepDuration(workflowStep)

        then:
        duration == "-"
    }

    void 'calculateStepDuration, when step is running and jobStarted is set, return formatted duration'() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.RUNNING, jobStarted: ZonedDateTime.now().minusMinutes(5))

        when:
        String duration = service.calculateStepDuration(workflowStep)

        then:
        duration != "-"
    }

    @Unroll
    void 'calculateStepDuration, when step is finished with state #state and both timestamps are set, return duration between both timestamps'() {
        given:
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(10)
        ZonedDateTime finish = ZonedDateTime.now().minusMinutes(2)
        WorkflowStep workflowStep = createWorkflowStep(state: state, jobStarted: start, jobFinished: finish, workflowError: workflowError)

        when:
        String duration = service.calculateStepDuration(workflowStep)

        then:
        duration == TimeUtils.getFormattedDurationForZonedDateTime(start, finish)

        where:
        state                      | workflowError
        WorkflowStep.State.FAILED  | createWorkflowError()
        WorkflowStep.State.SUCCESS | null
        WorkflowStep.State.SKIPPED | null
    }

    @Unroll
    void 'calculateDuration, when run is in running state #state but lastJobFinished is set, return duration between firstJobStarted and lastJobFinished'() {
        given:
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(10)
        ZonedDateTime finish = ZonedDateTime.now().minusMinutes(2)
        WorkflowRun workflowRun = createWorkflowRun(state: state, firstJobStarted: start, lastJobFinished: finish)

        when:
        String duration = service.calculateDuration(workflowRun)

        then:
        duration == TimeUtils.getFormattedDurationForZonedDateTime(start, finish)

        where:
        state << [WorkflowRun.State.PENDING, WorkflowRun.State.RUNNING_OTP, WorkflowRun.State.RUNNING_WES]
    }

    void 'calculateStepDuration, when step is in RUNNING state but jobFinished is set, return duration between jobStarted and jobFinished'() {
        given:
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(10)
        ZonedDateTime finish = ZonedDateTime.now().minusMinutes(2)
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.RUNNING, jobStarted: start, jobFinished: finish)

        when:
        String duration = service.calculateStepDuration(workflowStep)

        then:
        duration == TimeUtils.getFormattedDurationForZonedDateTime(start, finish)
    }

    void 'calculateDuration, when run is FAILED and lastJobFinished is null, return dash'() {
        given:
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(5)
        WorkflowRun workflowRun = createWorkflowRun(
                state: WorkflowRun.State.FAILED,
                firstJobStarted: start,
                lastJobFinished: null
        )

        when:
        String duration = service.calculateDuration(workflowRun)

        then:
        duration == "-"
    }
}
