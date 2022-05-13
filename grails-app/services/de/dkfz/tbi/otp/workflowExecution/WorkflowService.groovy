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

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType

import java.time.LocalDate

@Transactional
class WorkflowService {

    JobService jobService

    OtpWorkflowService otpWorkflowService

    void createRestartedWorkflows(List<WorkflowStep> steps, boolean startDirectly = true) {
        steps.each {
            createRestartedWorkflow(it, startDirectly)
        }
    }

    WorkflowRun createRestartedWorkflow(WorkflowStep step, boolean startDirectly = true) {
        assert step
        assert step.workflowRun.state == WorkflowRun.State.FAILED

        WorkflowRun oldRun = step.workflowRun
        OtpWorkflow otpWorkflow = otpWorkflowService.lookupOtpWorkflowBean(oldRun)

        WorkflowRun run = new WorkflowRun([
                workflow        : oldRun.workflow,
                workflowVersion : oldRun.workflowVersion,
                priority        : oldRun.priority,
                project         : oldRun.project,
                displayName     : oldRun.displayName,
                shortDisplayName: step.workflowRun.shortDisplayName,
                combinedConfig  : oldRun.combinedConfig,
                restartedFrom   : oldRun,
                state           : WorkflowRun.State.PENDING,
        ]).save(flush: true)

        oldRun.inputArtefacts.each { String role, WorkflowArtefact inputArtefact ->
            new WorkflowRunInputArtefact([
                    role            : role,
                    workflowArtefact: inputArtefact,
                    workflowRun     : run,
            ]).save(flush: true)
        }

        oldRun.outputArtefacts.each { String role, WorkflowArtefact oldWorkflowArtefact ->
            WorkflowArtefact newWorkflowArtefact = new WorkflowArtefact(
                    state: WorkflowArtefact.State.PLANNED_OR_RUNNING,
                    producedBy: run,
                    outputRole: oldWorkflowArtefact.outputRole,
                    displayName: oldWorkflowArtefact.displayName,
                    artefactType: oldWorkflowArtefact.artefactType,
            ).save(flush: true)

            Artefact oldArtefact = oldWorkflowArtefact.artefact.orElseThrow({
                new AssertionError("The old WorkflowArtefact ${oldWorkflowArtefact} of WorkflowRun ${oldRun} must have an concrete artefact" as Object)
            })

            Artefact newArtefact = otpWorkflow.createCopyOfArtefact(oldArtefact)
            newArtefact.workflowArtefact = newWorkflowArtefact
            newArtefact.save(flush: true)

            WorkflowRunInputArtefact.findAllByWorkflowArtefact(oldWorkflowArtefact).each { WorkflowRunInputArtefact workflowRunInputArtefact ->
                workflowRunInputArtefact.workflowArtefact = newWorkflowArtefact
                workflowRunInputArtefact.save(flush: true)

                otpWorkflowService.lookupOtpWorkflowBean(workflowRunInputArtefact.workflowRun).reconnectDependencies(newArtefact, newWorkflowArtefact)
            }

            oldWorkflowArtefact.state = WorkflowArtefact.State.FAILED
            oldWorkflowArtefact.save(flush: true)
        }

        oldRun.state = WorkflowRun.State.RESTARTED
        oldRun.save(flush: true)

        if (startDirectly) {
            jobService.createNextJob(run)
        }

        return run
    }

    void enableWorkflow(Workflow workflow) {
        assert workflow
        workflow.enabled = true
        workflow.save(flush: true)
    }

    void disableWorkflow(Workflow workflow) {
        assert workflow
        workflow.enabled = false
        workflow.save(flush: true)
    }

    List<Workflow> list() {
        return Workflow.list()
    }

    List<Workflow> findAllByDeprecatedDateIsNull() {
        return Workflow.findAllByDeprecatedDateIsNull()
    }

    Workflow updateWorkflow(UpdateWorkflowDto updateWorkflowDto) {
        Workflow workflow = Workflow.get(updateWorkflowDto.id)
        workflow.priority = updateWorkflowDto.priority
        workflow.enabled = updateWorkflowDto.enabled
        workflow.maxParallelWorkflows = updateWorkflowDto.maxParallelWorkflows

        if (updateWorkflowDto.supportedSeqTypes) {
            workflow.supportedSeqTypes = SeqType.getAll(updateWorkflowDto.supportedSeqTypes)
        } else {
            workflow.supportedSeqTypes = null
        }

        if (updateWorkflowDto.allowedRefGenomes) {
            workflow.allowedReferenceGenomes = ReferenceGenome.getAll(updateWorkflowDto.allowedRefGenomes)
        } else {
            workflow.allowedReferenceGenomes = null
        }

        if (updateWorkflowDto.deprecated && !workflow.deprecatedDate) {
            workflow.deprecatedDate = LocalDate.now()
        } else if (!updateWorkflowDto.deprecated) {
            workflow.deprecatedDate = null
        }

        workflow.save(flush: true)
        return workflow
    }
}
