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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

@Rollback
@Integration
class WorkflowRunServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, DomainFactoryProcessingPriority {

    void "nextWaitingWorkflow, if more workflows allowed and state is PENDING, then return workflowRun"() {
        given:
        WorkflowRun workflowRun = createWorkflowRunHelper()
        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == workflowRun
    }

    @Unroll
    void "nextWaitingWorkflow,  if more workflows allow and state is #state, then return null"() {
        given:
        createWorkflowRunHelper(state)
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
    void "nextWaitingWorkflow, if more workflows allow and state is PENDING and state of artefact is #state, then return null"() {
        given:
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, state)
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

    void "nextWaitingWorkflow, if multiple workflowRuns ready sharing the highest processingPriority, then select after the highest work priority"() {
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

    void "nextWaitingWorkflow, if multiple workflowRuns ready sharing the highest processingPriority and work priority, return the oldest"() {
        given:
        createWorkflowRunWithPriority(5, 8)
        WorkflowRun workflowRun = createWorkflowRunWithPriority(5, 8)
        createWorkflowRunWithPriority(5, 8)
        workflowRun.dateCreated = workflowRun.dateCreated - 5
        workflowRun.save(flush: true)

        WorkflowRunService service = new WorkflowRunService()

        when:
        WorkflowRun ret = service.nextWaitingWorkflow(0)

        then:
        ret == workflowRun
    }

    private WorkflowRun createWorkflowRunHelper(WorkflowRun.State runState = WorkflowRun.State.PENDING,
                                                WorkflowArtefact.State artefactState = WorkflowArtefact.State.SUCCESS) {
        WorkflowRun workflowRun = createWorkflowRun([
                state: runState,
        ])
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                workflowArtefact: createWorkflowArtefact([
                        state: artefactState,
                ])
        ])
        return workflowRun
    }

    private WorkflowRun createWorkflowRunWithPriority(int runPriority, int workflowPriority) {
        return createWorkflowRun([
                state   : WorkflowRun.State.PENDING,
                priority: findOrCreateProcessingPriority([
                        priority: runPriority,
                ]),
                workflow: createWorkflow([
                        priority: workflowPriority as short,
                ]),
        ])
    }
}
