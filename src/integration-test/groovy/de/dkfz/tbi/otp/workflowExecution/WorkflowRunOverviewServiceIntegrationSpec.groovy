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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import grails.util.Pair
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.utils.TimeFormats

@Rollback
@Integration
class WorkflowRunOverviewServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    WorkflowRunOverviewService service

    void setup() {
        service = new WorkflowRunOverviewService()
    }

    void "test getNumberOfRunsPerWorkflowAndState"() {
        given:
        Workflow workflow1 = createWorkflow()
        createWorkflowRun(workflow: workflow1, state: WorkflowRun.State.SUCCESS)
        createWorkflowRun(workflow: workflow1, state: WorkflowRun.State.FAILED)
        createWorkflowRun(workflow: workflow1, state: WorkflowRun.State.RUNNING_OTP)
        Workflow workflow2 = createWorkflow()
        createWorkflowRun(workflow: workflow2, state: WorkflowRun.State.FAILED)
        createWorkflowRun(workflow: workflow2, state: WorkflowRun.State.FAILED)

        when:
        Map<Pair<WorkflowRun.State, Workflow>, Long> result = service.numberOfRunsPerWorkflowAndState

        then:
        result[new Pair(WorkflowRun.State.SUCCESS, workflow1)] == 1
        result[new Pair(WorkflowRun.State.FAILED, workflow1)] == 1
        result[new Pair(WorkflowRun.State.RUNNING_OTP, workflow1)] == 1
        result[new Pair(WorkflowRun.State.FAILED, workflow2)] == 2
    }

    void "test getLatestRunsByState"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun run1 = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.FAILED)
        createWorkflowStep(workflowRun: run1)
        createWorkflowStep(workflowRun: run1)

        WorkflowRun run2 = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.FAILED)
        createWorkflowStep(workflowRun: run2)
        WorkflowStep stepFailed = createWorkflowStep(workflowRun: run2)

        WorkflowRun run3 = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)
        createWorkflowStep(workflowRun: run3)
        createWorkflowStep(workflowRun: run3)

        WorkflowRun run4 = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.SUCCESS)
        createWorkflowStep(workflowRun: run4)
        createWorkflowStep(workflowRun: run4)

        WorkflowRun run5 = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.SUCCESS)
        createWorkflowStep(workflowRun: run5)
        WorkflowStep stepSuccess = createWorkflowStep(workflowRun: run5)

        Map<Workflow, String> result

        when:
        result = service.latestFailedRuns

        then:
        result.size() == 1
        result.containsKey(run2.workflow)
        result[run2.workflow] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(stepFailed.lastUpdated)

        when:
        result = service.latestSuccessfulRuns

        then:
        result.size() == 1
        result.containsKey(run5.workflow)
        result[run5.workflow] == TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(stepSuccess.lastUpdated)
    }
}
