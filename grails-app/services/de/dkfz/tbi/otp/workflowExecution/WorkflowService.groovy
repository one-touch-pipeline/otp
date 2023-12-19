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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.exceptions.FileAccessForProjectNotAllowedException
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow

@CompileDynamic
@Transactional
class WorkflowService {

    JobService jobService

    OtpWorkflowService otpWorkflowService

    private static final Set<String> FASTQC_WORKFLOWS = [
            BashFastQcWorkflow.WORKFLOW,
            WesFastQcWorkflow.WORKFLOW,
    ].toSet().asImmutable()

    Workflow getExactlyOneWorkflow(String name) {
        return CollectionUtils.exactlyOneElement(Workflow.findAllByNameAndDeprecatedDateIsNull(name))
    }

    Set<SeqType> getSupportedSeqTypes(String name) {
        return getSupportedSeqTypesOfVersions(Workflow.findAllByName(name))
    }

    void createRestartedWorkflows(List<WorkflowStep> steps, boolean startDirectly = true) {
        steps.each {
            createRestartedWorkflow(it, startDirectly)
        }
    }

    WorkflowRun createRestartedWorkflow(WorkflowStep step, boolean startDirectly = true) {
        assert step
        assert step.workflowRun.state == WorkflowRun.State.FAILED

        if (step.workflowRun.project.state == Project.State.ARCHIVED || step.workflowRun.project.state == Project.State.DELETED) {
            String stateName = step.workflowRun.project.state.name().toLowerCase()
            throw new FileAccessForProjectNotAllowedException(
                    "${step.workflowRun.project} is ${stateName} and ${step.workflowRun} cannot be restarted"
            )
        }

        WorkflowRun oldRun = step.workflowRun
        WorkflowRun run = createNewRunBasedOnOldRun(oldRun)
        run.workDirectory = oldRun.workDirectory
        createInputArtefactsForNewRun(oldRun, run)
        createAndConnectOutputArtefactsForNewRun(oldRun, run)

        oldRun.state = WorkflowRun.State.RESTARTED
        oldRun.save(flush: true)

        if (startDirectly) {
            jobService.createNextJob(run)
        }

        return run
    }

    private WorkflowRun createNewRunBasedOnOldRun(WorkflowRun oldRun) {
        return new WorkflowRun([
                workflow        : oldRun.workflow,
                workflowVersion : oldRun.workflowVersion,
                priority        : oldRun.project.processingPriority,
                project         : oldRun.project,
                displayName     : oldRun.displayName,
                shortDisplayName: oldRun.shortDisplayName,
                combinedConfig  : oldRun.combinedConfig,
                restartedFrom   : oldRun,
                state           : WorkflowRun.State.PENDING,
        ]).save(flush: true)
    }

    private void createInputArtefactsForNewRun(WorkflowRun oldRun, WorkflowRun newRun) {
        oldRun.inputArtefacts.each { String role, WorkflowArtefact inputArtefact ->
            new WorkflowRunInputArtefact([
                    role            : role,
                    workflowArtefact: inputArtefact,
                    workflowRun     : newRun,
            ]).save(flush: true)
        }
    }

    private void createAndConnectOutputArtefactsForNewRun(WorkflowRun oldRun, WorkflowRun newRun) {
        OtpWorkflow otpWorkflow = otpWorkflowService.lookupOtpWorkflowBean(oldRun)

        oldRun.outputArtefacts.each { String role, WorkflowArtefact oldWorkflowArtefact ->
            WorkflowArtefact newWorkflowArtefact = new WorkflowArtefact(
                    state: WorkflowArtefact.State.PLANNED_OR_RUNNING,
                    producedBy: newRun,
                    outputRole: oldWorkflowArtefact.outputRole,
                    displayName: oldWorkflowArtefact.displayName,
                    artefactType: oldWorkflowArtefact.artefactType,
            ).save(flush: true)

            Artefact oldArtefact = oldWorkflowArtefact.artefact.orElseThrow {
                new AssertionError("The old WorkflowArtefact ${oldWorkflowArtefact} of WorkflowRun ${oldRun} must have an concrete artefact" as Object)
            }

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
    }

    List<SeqType> getSupportedSeqTypesOfVersions(List<Workflow> workflows) {
        if (!workflows) {
            return []
        }
        return (WorkflowVersion.createCriteria().list {
            apiVersion {
                "in"("workflow", workflows)
            }
        } as List<WorkflowVersion>).collectMany { workflowVersion ->
            workflowVersion.supportedSeqTypes
        }.unique()
    }

    List<SeqType> getSupportedSeqTypesOfVersions(Workflow workflow) {
        if (!workflow) {
            return []
        }

        return (WorkflowVersion.createCriteria().list {
            apiVersion {
                "eq"("workflow", workflow)
            }
        } as List<WorkflowVersion>).collectMany { workflowVersion ->
            workflowVersion.supportedSeqTypes
        }.unique()
    }

    List<Workflow> findAllFastqcWorkflows() {
        return Workflow.createCriteria().list {
            isNull("deprecatedDate")
            "in"("name", FASTQC_WORKFLOWS)
            order("beanName", "asc")
        } as List<Workflow>
    }

    List<Workflow> findAllAlignmentWorkflows() {
        List<String> alignmentWorkflowNameList = alignmentWorkflowNames
        return alignmentWorkflowNameList ? Workflow.findAllByBeanNameInListAndDeprecatedDateIsNull(alignmentWorkflowNameList).sort { it.name } : []
    }

    private List<String> getAlignmentWorkflowNames() {
        Map<String, OtpWorkflow> workflowBeans = applicationContext.getBeansOfType(OtpWorkflow)
        return workflowBeans.findAll { it.value.isAlignment() }*.key
    }

    List<Workflow> findAllAnalysisWorkflows() {
        Map<String, OtpWorkflow> workflowBeans = applicationContext.getBeansOfType(OtpWorkflow)
        List<String> analysisWorkflowNames = workflowBeans.findAll { it.value.isAnalysis() }*.key
        return analysisWorkflowNames ? Workflow.findAllByBeanNameInListAndDeprecatedDateIsNull(analysisWorkflowNames).sort { it.name } : []
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
        workflow.defaultVersion = updateWorkflowDto.defaultVersion

        if (updateWorkflowDto.supportedSeqTypes) {
            workflow.defaultSeqTypesForWorkflowVersions = SeqType.getAll(updateWorkflowDto.supportedSeqTypes)
        } else {
            workflow.defaultSeqTypesForWorkflowVersions = null
        }

        if (updateWorkflowDto.allowedRefGenomes) {
            workflow.defaultReferenceGenomesForWorkflowVersions = ReferenceGenome.getAll(updateWorkflowDto.allowedRefGenomes)
        } else {
            workflow.defaultReferenceGenomesForWorkflowVersions = null
        }

        return workflow.save(flush: true)
    }

    /**
     * returns if a given workflow is an alignment workflow
     */
    boolean isAlignment(Workflow workflow) {
        return otpWorkflowService.lookupOtpWorkflowBean(workflow).isAlignment()
    }
}
