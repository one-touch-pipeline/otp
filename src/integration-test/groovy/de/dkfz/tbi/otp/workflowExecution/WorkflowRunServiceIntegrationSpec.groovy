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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.project.Project

import java.time.Instant
import java.time.temporal.ChronoUnit

@Rollback
@Integration
class WorkflowRunServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, DomainFactoryProcessingPriority {

    AutoTimestampEventListener autoTimestampEventListener

    void "nextWaitingWorkflow, if more workflows allowed and state is PENDING, then return workflowRun"() {
        given:
        WorkflowRun workflowRun = createWorkflowRunHelper()
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, WorkflowArtefact.State.SUCCESS, workflowRun.workflow)
        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == workflowRun
    }

    void "nextWaitingWorkflow, when workflow is disabled, then return null"() {
        given:
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, WorkflowArtefact.State.SUCCESS, createWorkflow(enabled: false))
        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == null
    }

    void "nextWaitingWorkflow, if overall workflow count is ok, but the workflow specific count is too high then return null"() {
        given:
        WorkflowRun workflowRun = createWorkflowRunHelper()

        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, WorkflowArtefact.State.SUCCESS, workflowRun.workflow)
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_WES, WorkflowArtefact.State.SUCCESS, workflowRun.workflow)
        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == null
    }

    @Unroll
    void "nextWaitingWorkflow, if more workflows are allowed and state is #state, then return null"() {
        given:
        createWorkflowRunHelper(state as WorkflowRun.State)
        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == null

        where:
        state << (WorkflowRun.State.values() - WorkflowRun.State.PENDING)
    }

    void "nextWaitingWorkflow, if not more workflows are allowed, then return null"() {
        given:
        createWorkflowRunHelper()
        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(Integer.MAX_VALUE)

        then:
        ret == null
    }

    @Unroll
    void "nextWaitingWorkflow, if more workflows are allowed and state is PENDING and state of artefact is #state, then return null"() {
        given:
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, state as WorkflowArtefact.State)
        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == null

        where:
        state << (WorkflowArtefact.State.values() - WorkflowArtefact.State.SUCCESS)
    }

    void "nextWaitingWorkflow, if multiple workflowRuns ready, then return the workflowRun with highest processingPriority"() {
        given:
        createWorkflowRunWithPriority(1, 0)
        createWorkflowRunWithPriority(1, 8)
        WorkflowRun workflowRun = createWorkflowRunWithPriority(5, 3)
        createWorkflowRunWithPriority(3, 0)
        createWorkflowRunWithPriority(3, 8)

        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == workflowRun
    }

    void "nextWaitingWorkflow, if multiple workflowRuns ready sharing the highest processingPriority, then select after the highest workPriority"() {
        given:
        createWorkflowRunWithPriority(5, 0)
        createWorkflowRunWithPriority(5, 8)
        WorkflowRun workflowRun = createWorkflowRunWithPriority(5, 12)
        createWorkflowRunWithPriority(5, 0)
        createWorkflowRunWithPriority(5, 8)

        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == workflowRun
    }

    void "nextWaitingWorkflow, if multiple workflowRuns ready sharing the highest processingPriority reach limit, then select one with lower processingPriority"() {
        given:
        WorkflowRun workflowRun1 = createWorkflowRunWithPriority(5, 0)
        workflowRun1.state = WorkflowRun.State.RUNNING_OTP
        workflowRun1.save(flush: true)

        WorkflowRun workflowRun2 = createWorkflowRunWithPriority(5, 0, workflowRun1.workflow)
        workflowRun2.state = WorkflowRun.State.RUNNING_OTP
        workflowRun2.save(flush: true)

        WorkflowRun workflowRun3 = createWorkflowRunWithPriority(4, 0)

        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == workflowRun3
    }

    void "nextWaitingWorkflow, if multiple workflowRuns ready sharing the highest processingPriority and work priority, return the oldest"() {
        given:
        createWorkflowRunWithPriority(5, 8)
        createWorkflowRunWithPriority(5, 8)

        WorkflowRun workflowRun =
                createWorkflowRunWithPriority(5, 8, createWorkflow(), Date.from(Instant.now().minus(5, ChronoUnit.DAYS)))

        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == workflowRun
    }

    void "nextWaitingWorkflow, if project of workflow run is archived, then return null"() {
        given:
        WorkflowRun workflowRun = createWorkflowRunHelper()
        workflowRun.project.state = Project.State.ARCHIVED
        workflowRun.project.save(flush: true)
        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == null
    }

    private WorkflowRun createWorkflowRunHelper(WorkflowRun.State runState = WorkflowRun.State.PENDING,
                                                WorkflowArtefact.State artefactState = WorkflowArtefact.State.SUCCESS, Workflow workflow = createWorkflow()) {
        workflow.maxParallelWorkflows = 2
        workflow.save(flush: true)
        WorkflowRun workflowRun = createWorkflowRun([
                state   : runState,
                workflow: workflow,
        ])
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                workflowArtefact: createWorkflowArtefact([
                        state: artefactState,
                ]),
        ])
        return workflowRun
    }

    private WorkflowRun createWorkflowRunWithPriority(int runPriority, int workflowPriority, Workflow wf = createWorkflow(), Date dateCreated = new Date()) {
        wf.priority = workflowPriority as short
        wf.maxParallelWorkflows = 1
        wf.save(flush: true)

        WorkflowRun workflowRun = null

        autoTimestampEventListener.withoutDateCreated(WorkflowRun) {
            workflowRun = createWorkflowRun([
                    state      : WorkflowRun.State.PENDING,
                    priority   : findOrCreateProcessingPriority([
                            priority: runPriority,
                    ]),
                    workflow   : wf,
                    dateCreated: dateCreated,
            ])
        }

        return workflowRun
    }
}
