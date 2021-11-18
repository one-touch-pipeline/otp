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
package de.dkfz.tbi.otp.utils

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog

@Rollback
@Integration
class WorkflowDeletionServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    WorkflowDeletionService workflowDeletionService

    void "deleteWorkflowRun, should delete all a WorkflowRun and all its dependencies"() {
        given:
        WorkflowRunInputArtefact wria = createWorkflowArtefactAndRun()
        WorkflowRun workflowRun = createWorkflowRun(
                omittedMessage: createOmittedMessage()
        )
        createWorkflowSteps(wria.workflowRun)
        wria.workflowArtefact.producedBy = workflowRun
        wria.workflowArtefact.outputRole = "someOutPutRole"
        wria.save(flush: true)

        when:
        workflowDeletionService.deleteWorkflowRun(workflowRun)

        then:
        WorkflowRun.count == 0
        WorkflowArtefact.count == 0
        WorkflowRunInputArtefact.count == 0
        WorkflowStep.count == 0
        ClusterJob.count == 0
        WorkflowLog.count == 0
        WorkflowError.count == 0
        OmittedMessage.count == 0
    }

    void "test deleteWorkflowArtefact"() {
        given:
        WorkflowRunInputArtefact wria = createWorkflowArtefactAndRun()
        createWorkflowArtefact(producedBy: wria.workflowRun)

        when:
        workflowDeletionService.deleteWorkflowArtefact(wria.workflowArtefact)

        then:
        WorkflowRun.count == 0
        WorkflowArtefact.count == 0
        WorkflowRunInputArtefact.count == 0
    }

    void "deleteWorkflowStep, should delete a WorkflowStep and all its dependencies if it is last in list"() {
        given:
        WorkflowRunInputArtefact wria = createWorkflowArtefactAndRun()
        WorkflowStep LastWorkflowStep = createWorkflowSteps(wria.workflowRun).last()

        when:
        workflowDeletionService.deleteWorkflowStep(LastWorkflowStep)

        then:
        !WorkflowStep.get(LastWorkflowStep.id)
        WorkflowStep.count == 2
        ClusterJob.count == 4
        WorkflowLog.count == 4
        WorkflowError.count == 1
    }

    void "deleteWorkflowStep, should throw IllegalArgumentException if a WorkflowStep has already been restarted"() {
        given:
        WorkflowRunInputArtefact wria = createWorkflowArtefactAndRun()
        List<WorkflowStep> workflowSteps = createWorkflowSteps(wria.workflowRun)

        when:
        workflowDeletionService.deleteWorkflowStep(workflowSteps[1])

        then:
        thrown(IllegalArgumentException)
    }

    void "deleteWorkflowStep, should throw IllegalArgumentException if a WorkflowStep has already a following WorkflowStep"() {
        given:
        WorkflowRunInputArtefact wria = createWorkflowArtefactAndRun()
        List<WorkflowStep> workflowSteps = createWorkflowSteps(wria.workflowRun)

        when:
        workflowDeletionService.deleteWorkflowStep(workflowSteps.first())

        then:
        thrown(IllegalArgumentException)
    }

    void "deleteSelectedProjectSeqTypeWorkflowVersions, should delete all SelectedProjectSeqTypeWorkflowVersions to corresponding project"() {
        given:
        Project project = createProject()

        createSelectedProjectSeqTypeWorkflowVersion(
                project: project,
        )

        when:
        workflowDeletionService.deleteSelectedProjectSeqTypeWorkflowVersions(project)

        then:
        SelectedProjectSeqTypeWorkflowVersion.count == 0
    }

    void "deleteReferenceGenomeSelector, should delete all ReferenceGenomeSelector to corresponding project"() {
        given:
        Project project = createProject()

        createReferenceGenomeSelector(
                project: project,
        )

        when:
        workflowDeletionService.deleteReferenceGenomeSelector(project)

        then:
        ReferenceGenomeSelector.count == 0
    }

    private WorkflowRunInputArtefact createWorkflowArtefactAndRun() {
        WorkflowArtefact wa = createWorkflowArtefact()
        WorkflowRun workflowRun = createWorkflowRun()
        return createWorkflowRunInputArtefact(workflowRun: workflowRun, role: "whatever", workflowArtefact: wa)
    }

    private List<WorkflowStep> createWorkflowSteps(WorkflowRun workflowRun) {
        WorkflowStep workflowStep = createWorkflowStep(
                workflowRun: workflowRun,
        )
        for (int i : 0..3) {
            createClusterJob(workflowStep: workflowStep)
            createWorkflowMessageLog(workflowStep: workflowStep)
        }
        WorkflowStep followingWorkflowStep = createWorkflowStep(
                workflowRun: workflowRun,
                workflowError: createWorkflowError(),
                state: WorkflowStep.State.FAILED,
                previous: workflowStep
        )
        WorkflowStep restartedWorkflowStep = createWorkflowStep(
                workflowRun: workflowRun,
                restartedFrom: followingWorkflowStep,
                previous: workflowStep
        )
        for (int i : 0..3) {
            createClusterJob(workflowStep: restartedWorkflowStep)
            createWorkflowMessageLog(workflowStep: restartedWorkflowStep)
        }
        return [workflowStep, followingWorkflowStep, restartedWorkflowStep]
    }
}
