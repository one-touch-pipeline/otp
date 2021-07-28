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
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
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
        run.configs == []
        run.combinedConfig == '{"config":"combined"}'
        run.workflow == workflow
        run.state == WorkflowRun.State.PENDING
        run.restartedFrom == null
        run.omittedMessage == null
        run.workflowSteps == []
        run.displayName == multiLineName[0] + "\n" + multiLineName[1]
        run.shortDisplayName == shortName
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
}
