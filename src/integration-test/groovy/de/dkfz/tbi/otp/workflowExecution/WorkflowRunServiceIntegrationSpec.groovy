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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.project.Project

import java.time.Instant
import java.time.temporal.ChronoUnit

@Rollback
@Integration
class WorkflowRunServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, DomainFactoryProcessingPriority {

    WorkflowRunService workflowRunService
    AutoTimestampEventListener autoTimestampEventListener

    void "nextWaitingWorkflow, if more workflows allowed and state is PENDING, then return workflowRun"() {
        given:
        WorkflowRun workflowRun = createWorkflowRunHelper()
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, WorkflowArtefact.State.SUCCESS, workflowRun.workflow)

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

        then:
        ret == workflowRun
    }

    void "nextWaitingWorkflow, when workflow is disabled, then return null"() {
        given:
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, WorkflowArtefact.State.SUCCESS, createWorkflow(enabled: false))

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

        then:
        ret == null
    }

    void "nextWaitingWorkflow, if overall workflow count is ok, but the workflow specific count is too high then return null"() {
        given:
        WorkflowRun workflowRun = createWorkflowRunHelper()

        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, WorkflowArtefact.State.SUCCESS, workflowRun.workflow)
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_WES, WorkflowArtefact.State.SUCCESS, workflowRun.workflow)

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

        then:
        ret == null
    }

    @Unroll
    void "nextWaitingWorkflow, if more workflows are allowed and state is #state, then return null"() {
        given:
        createWorkflowRunHelper(state as WorkflowRun.State)

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

        then:
        ret == null

        where:
        state << (WorkflowRun.State.values() - WorkflowRun.State.PENDING)
    }

    void "nextWaitingWorkflow, if not more workflows are allowed, then return null"() {
        given:
        createWorkflowRunHelper()

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(Integer.MAX_VALUE)

        then:
        ret == null
    }

    @Unroll
    void "nextWaitingWorkflow, if more workflows are allowed and state is PENDING and state of artefact is #state, then return null"() {
        given:
        createWorkflowRunHelper(WorkflowRun.State.RUNNING_OTP, state as WorkflowArtefact.State)

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

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

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

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

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

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

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

        then:
        ret == workflowRun3
    }

    void "nextWaitingWorkflow, if multiple workflowRuns ready sharing the highest processingPriority and work priority, return the oldest"() {
        given:
        createWorkflowRunWithPriority(5, 8)
        createWorkflowRunWithPriority(5, 8)

        WorkflowRun workflowRun =
                createWorkflowRunWithPriority(5, 8, createWorkflow(), Date.from(Instant.now().minus(5, ChronoUnit.DAYS)))

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

        then:
        ret == workflowRun
    }

    void "nextWaitingWorkflow, if multiple workflowRuns ready sharing the highest processingPriority and work priority, return the restarted workflow first"() {
        given:
        createWorkflowRunWithPriority(5, 8)
        Workflow workflow = createWorkflow()
        WorkflowRun originalWorkflowRun = createWorkflowRunWithPriority(5, 8, workflow)
        originalWorkflowRun.state = WorkflowRun.State.RESTARTED
        originalWorkflowRun.save(flush: true)
        WorkflowRun restartedWorkflowRun = createWorkflowRunWithPriority(5, 8, workflow)
        restartedWorkflowRun.restartedFrom = originalWorkflowRun
        restartedWorkflowRun.save(flush: true)

        createWorkflowRunInputArtefact([
                workflowRun     : restartedWorkflowRun,
                workflowArtefact: createWorkflowArtefact([
                        state: WorkflowArtefact.State.SUCCESS,
                ]),
        ])

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

        then:
        ret == restartedWorkflowRun
    }

    void "nextWaitingWorkflow, if project of workflow run is archived, then return null"() {
        given:
        WorkflowRun workflowRun = createWorkflowRunHelper()
        workflowRun.project.state = Project.State.ARCHIVED
        workflowRun.project.save(flush: true)

        when:
        WorkflowRun ret = workflowRunService.nextWaitingWorkflow(0)

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

    void "workflowOverview, with stepFilter equal to current step beanName, returns matching runs"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun run1 = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)
        WorkflowRun run2 = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)

        createWorkflowStep(workflowRun: run1, beanName: 'dataInstallationConditionalFailJob', obsolete: false)
        createWorkflowStep(workflowRun: run2, beanName: 'otherStepName', obsolete: false)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(
            workflow, [WorkflowRun.State.RUNNING_OTP], null, 'dataInstallationConditionalFailJob', [], 0, 10
        )

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data*.id.contains(run1.id)
        !result.data*.id.contains(run2.id)
    }

    void "workflowOverview, with stepFilter not equal to current step beanName, excludes run"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun run = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)
        createWorkflowStep(workflowRun: run, beanName: 'someJobName', obsolete: false)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(
            workflow, [WorkflowRun.State.RUNNING_OTP], null, 'some', [], 0, 10
        )

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        !result.data*.id.contains(run.id)
    }

    void "workflowOverview, excludes run when stepFilter matches an obsolete latest step"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun run = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)

        createWorkflowStep(workflowRun: run, beanName: 'firstStepName', obsolete: false)
        createWorkflowStep(workflowRun: run, beanName: 'lastStepName', obsolete: true)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(
            workflow, [WorkflowRun.State.RUNNING_OTP], null, 'lastStepName', [], 0, 10
        )

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        !result.data*.id.contains(run.id)
    }

    void "workflowOverview, excludes run when stepFilter matches an older non-obsolete step"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun run = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)

        createWorkflowStep(workflowRun: run, beanName: 'firstStepName', obsolete: false)
        createWorkflowStep(workflowRun: run, beanName: 'lastStepName', obsolete: false)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(
            workflow, [WorkflowRun.State.RUNNING_OTP], null, 'firstStepName', [], 0, 10
        )

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        !result.data*.id.contains(run.id)
    }

    void "workflowOverview, includes run when stepFilter matches the latest non-obsolete step"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun run = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)

        createWorkflowStep(workflowRun: run, beanName: 'firstStepName', obsolete: false)
        createWorkflowStep(workflowRun: run, beanName: 'lastStepName', obsolete: true)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(
            workflow, [WorkflowRun.State.RUNNING_OTP], null, 'firstStepName', [], 0, 10
        )

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data*.id.contains(run.id)
    }

    void "workflowOverview, with run having only obsoleted steps, excludes run when stepFilter is active"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun run = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)
        createWorkflowStep(workflowRun: run, beanName: 'someJob', obsolete: true)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(
            workflow, [WorkflowRun.State.RUNNING_OTP], null, 'someJob', [], 0, 10
        )

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        !result.data*.id.contains(run.id)
    }

    void "workflowOverview, returns workflow display name in data"() {
        given:
        Workflow workflow = createWorkflow()
        createWorkflowRun(workflow: workflow)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(workflow, [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data.size() == 1
        result.data[0].workflow == workflow.name
    }

    void "workflowOverview, returns formatted comment when run has a comment"() {
        given:
        WorkflowRun run = createWorkflowRun()
        run.comment = new Comment(comment: "my note", author: "tester", modificationDate: new Date()).save(flush: true)
        run.save(flush: true)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(run.workflow, [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data.size() == 1
        result.data[0].comment.contains("tester")
        result.data[0].comment.contains("my note")
    }

    void "workflowOverview, returns empty string when run has no comment"() {
        given:
        WorkflowRun run = createWorkflowRun()

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(run.workflow, [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data.size() == 1
        result.data[0].comment == ""
    }

    void "workflowOverview, filters by state"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun pendingRun = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.PENDING)
        createWorkflowRun(workflow: workflow, state: WorkflowRun.State.FAILED)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(workflow, [WorkflowRun.State.PENDING], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data*.id == [pendingRun.id]
    }

    void "workflowOverview, filters by name substring"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun matchingRun = createWorkflowRun(workflow: workflow, displayName: "target run xyz")
        createWorkflowRun(workflow: workflow, displayName: "other run")

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(workflow, [], "target", null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data*.id == [matchingRun.id]
    }

    void "workflowOverview, counts reflect running and failed states correctly"() {
        given:
        Workflow workflow = createWorkflow()
        createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_OTP)
        createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RUNNING_WES)
        createWorkflowRun(workflow: workflow, state: WorkflowRun.State.FAILED)
        createWorkflowRun(workflow: workflow, state: WorkflowRun.State.PENDING)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(workflow, [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.workflowsFiltered == 4
        result.running == 2
        result.failed == 1
    }

    void "workflowOverview, with no matching runs, returns zero counts instead of throwing an error"() {
        given:
        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(createWorkflow(), [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data == []
        result.workflowsFiltered == 0
        result.running == 0
        result.failed == 0
    }

    void "workflowOverview, excludes LEGACY runs from data and filtered count"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun activeRun = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.SUCCESS)
        createWorkflowRun(workflow: workflow, state: WorkflowRun.State.LEGACY)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(workflow, [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data*.id == [activeRun.id]
        result.workflowsFiltered == 1
    }

    void "workflowOverview, workflowsTotal excludes LEGACY runs globally"() {
        given:
        createWorkflowRun(state: WorkflowRun.State.SUCCESS)
        createWorkflowRun(state: WorkflowRun.State.LEGACY)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(null, [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.workflowsTotal == 1
    }

    void "workflowOverview, reports last non-obsolete step as current step"() {
        given:
        WorkflowRun run = createWorkflowRun()
        createWorkflowStep(workflowRun: run, beanName: 'firstStep', obsolete: false)
        WorkflowStep lastStep = createWorkflowStep(workflowRun: run, beanName: 'lastStep', obsolete: false)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(run.workflow, [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data[0].step == lastStep.beanName
        result.data[0].stepId == lastStep.id
    }

    void "workflowOverview, ignores obsolete steps when determining last step"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowStep activeStep = createWorkflowStep(workflowRun: run, beanName: 'activeStep', obsolete: false)
        createWorkflowStep(workflowRun: run, beanName: 'obsoleteStep', obsolete: true)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(run.workflow, [], null, null, [], 0, 10)

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data[0].step == activeStep.beanName
    }

    void "workflowOverview, stepFilter composes with workflow, state, and name filters as AND"() {
        given:
        Workflow workflow1 = createWorkflow()
        Workflow workflow2 = createWorkflow()

        WorkflowRun run1 = createWorkflowRun(workflow: workflow1, state: WorkflowRun.State.RUNNING_OTP, displayName: 'foo')
        WorkflowRun run2 = createWorkflowRun(workflow: workflow1, state: WorkflowRun.State.PENDING, displayName: 'foo')
        WorkflowRun run3 = createWorkflowRun(workflow: workflow2, state: WorkflowRun.State.RUNNING_OTP, displayName: 'foo')
        WorkflowRun run4 = createWorkflowRun(workflow: workflow1, state: WorkflowRun.State.RUNNING_OTP, displayName: 'bar')

        createWorkflowStep(workflowRun: run1, beanName: 'installation', obsolete: false)
        createWorkflowStep(workflowRun: run2, beanName: 'installation', obsolete: false)
        createWorkflowStep(workflowRun: run3, beanName: 'installation', obsolete: false)
        createWorkflowStep(workflowRun: run4, beanName: 'installation', obsolete: false)

        WorkflowRunSearchCriteria criteria = new WorkflowRunSearchCriteria(
            workflow1, [WorkflowRun.State.RUNNING_OTP], 'foo', 'installation', [], 0, 10
        )

        when:
        WorkflowRunSearchResult result = workflowRunService.workflowOverview(criteria)

        then:
        result.data*.id == [run1.id]
    }
}
