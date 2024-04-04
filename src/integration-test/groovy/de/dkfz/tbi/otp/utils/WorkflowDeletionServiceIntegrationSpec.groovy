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
package de.dkfz.tbi.otp.utils

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog
import de.dkfz.tbi.otp.workflowExecution.wes.WesRun

@Rollback
@Integration
class WorkflowDeletionServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    WorkflowDeletionService workflowDeletionService

    void "deleteWorkflowRun, should delete all a WorkflowRun and all its dependencies"() {
        given:
        WorkflowRunInputArtefact wria = createWorkflowArtefactAndRun()
        WorkflowRun workflowRun = createWorkflowRun(
                skipMessage: createSkipMessage(),
                workDirectory: null,
                workFolder: createWorkFolder(),
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
        WorkflowStepSkipMessage.count == 0
        WorkFolder.count == 0
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
        WorkflowStep lastWorkflowStep = createWorkflowSteps(wria.workflowRun).last()
        createWesRun([workflowStep: lastWorkflowStep])

        when:
        workflowDeletionService.deleteWorkflowStep(lastWorkflowStep)

        then:
        !WorkflowStep.get(lastWorkflowStep.id)
        WorkflowStep.count == 2
        ClusterJob.count == 4
        WorkflowLog.count == 4
        WorkflowError.count == 1
        WesRun.count == 0
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

    void "deleteWorkflowVersionSelector, should delete all WorkflowVersionSelectors to corresponding project"() {
        given:
        Project project = createProject()

        createWorkflowVersionSelector(
                project: project,
        )

        when:
        workflowDeletionService.deleteWorkflowVersionSelector(project)

        then:
        WorkflowVersionSelector.count == 0
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

    void "deleteWorkflowRun, should delete all WorkflowRun to corresponding project"() {
        given:
        Project project = createProject()

        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                producedBy: createWorkflowRun([
                        project      : project,
                        restartedFrom: createWorkflowRun([
                                project: project,
                        ]),
                ])
        ])

        createWorkflowRunInputArtefact([
                workflowArtefact: workflowArtefact,
                workflowRun     : createWorkflowRun([
                        project: project,
                ]),
        ])

        createWorkflowSteps(createWorkflowRun([
                project: project,
        ]))

        when:
        workflowDeletionService.deleteWorkflowRun(project)

        then:
        WorkflowRun.count == 0
        WorkflowStep.count == 0
        WorkflowArtefact.count == 0
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
