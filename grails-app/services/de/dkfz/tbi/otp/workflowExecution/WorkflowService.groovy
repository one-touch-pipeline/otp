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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.exceptions.FileAccessForProjectNotAllowedException
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterJobHandlingService
import de.dkfz.tbi.otp.workflowExecution.wes.WesRunService

@Transactional
class WorkflowService {

    JobService jobService

    OtpWorkflowService otpWorkflowService

    WorkflowVersionService workflowVersionService

    ClusterJobManagerFactoryService clusterJobManagerFactoryService

    FileSystemService fileSystemService

    ClusterJobHandlingService clusterJobHandlingService

    WesRunService wesRunService

    static final Set<String> FASTQC_WORKFLOWS = [
            BashFastQcWorkflow.WORKFLOW,
            WesFastQcWorkflow.WORKFLOW,
    ].toSet().asImmutable()

    @CompileDynamic
    Workflow getExactlyOneWorkflow(String name) {
        return CollectionUtils.exactlyOneElement(Workflow.findAllByNameAndDeprecatedDateIsNull(name), "Could not find workflow with name '${name}'")
    }

    @CompileDynamic
    WorkflowRun getUniqueWorkflowFromWorkflowSteps(List<Long> stepIds) {
        List<WorkflowStep> steps = WorkflowStep.findAllByIdInList(stepIds)
        assert steps*.workflowRun.unique().size() == 1
        return CollectionUtils.atMostOneElement(steps*.workflowRun.unique())
    }

    @CompileDynamic
    Set<SeqType> getSupportedSeqTypes(String name) {
        return getSupportedSeqTypesOfVersions(Workflow.findAllByName(name))
    }

    List<WorkflowRun> createRestartedWorkflows(List<WorkflowStep> steps) {
        return steps.collect {
            createRestartedWorkflow(it)
        }
    }

    @CompileDynamic
    WorkflowRun createRestartedWorkflow(WorkflowStep step) {
        assert step
        return createRestartedWorkflow(step.workflowRun)
    }

    @CompileDynamic
    WorkflowRun createRestartedWorkflow(WorkflowRun oldRun) {
        assert oldRun
        assert oldRun.state in [WorkflowRun.State.FAILED, WorkflowRun.State.FAILED_WAITING, WorkflowRun.State.KILLED]

        if (oldRun.project.state == Project.State.ARCHIVED || oldRun.project.state == Project.State.DELETED) {
            String stateName = oldRun.project.state.name().toLowerCase()
            throw new FileAccessForProjectNotAllowedException(
                    "${oldRun.project} is ${stateName} and ${oldRun} cannot be restarted"
            )
        }

        WorkflowRun run = createNewRunBasedOnOldRun(oldRun)
        run.workDirectory = oldRun.workDirectory
        createInputArtefactsForNewRun(oldRun, run)
        createAndConnectOutputArtefactsForNewRun(oldRun, run)

        oldRun.state = WorkflowRun.State.RESTARTED
        oldRun.save(flush: true)

        return run
    }

    /**
     * Kill a workflow run if it is in a state where killing the run is allowed:
     * [ PENDING, RUNNING_OTP, RUNNING_WES ]
     *
     * If the current workflow step is an OTP job/step -> let it run to the end
     * If the current workflow step is a Cluster job/step -> kill the cluster job
     * If the current workflow step is a WESkit job/step -> cancel the WES run
     *
     * The state of the workflow run is set to KILLED
     *
     * No next workflow step will be triggered (@link JobService#createNextJob(WorkflowRun workflowRun))
     *
     * @param run the workflow run to be killed
     * @return the workflow run that has been killed
     */
    WorkflowRun killWorkflowRun(WorkflowRun run) {
        assert run.state in WorkflowRun.UNFINISHED_STATES : "WorkflowRun ${run} is not allowed to be killed"

        if (run.project.state in [Project.State.ARCHIVED, Project.State.DELETED]) {
            String stateName = run.project.state.name().toLowerCase()
            throw new FileAccessForProjectNotAllowedException("${run.project} is ${stateName} and ${run} cannot be killed")
        }

        if (run.state == WorkflowRun.State.RUNNING_WES) {
            WorkflowStep step = run.workflowSteps.last()
            assert step: "Missing workflow step for the current workflow run"
            assert step.state == WorkflowStep.State.SUCCESS: "Workflow step is not in SUCCESS state"
            if (step.clusterJobs) {
                // Kill all cluster jobs in this workflow step
                clusterJobHandlingService.killClusterJobsInWorkflowStep(step)
            } else if (step.wesRuns) {
                // Kill all WESkit runs in this workflow step
                wesRunService.killWesRunsInWorkflowStep(step)
            }
        }

        run.state = WorkflowRun.State.KILLED
        run.save(flush: true)

        return run
    }

    @CompileDynamic
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

    @CompileDynamic
    private void createInputArtefactsForNewRun(WorkflowRun oldRun, WorkflowRun newRun) {
        oldRun.inputArtefacts.each { String role, WorkflowArtefact inputArtefact ->
            new WorkflowRunInputArtefact([
                    role            : role,
                    workflowArtefact: inputArtefact,
                    workflowRun     : newRun,
            ]).save(flush: true)
        }
    }

    @CompileDynamic
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

                OtpWorkflow nextWorkflow = otpWorkflowService.lookupOtpWorkflowBean(workflowRunInputArtefact.workflowRun)
                workflowRunInputArtefact.workflowRun.outputArtefacts.each { Map.Entry<String, WorkflowArtefact> it ->
                    nextWorkflow.reconnectDependencies(it.value.artefact.get(), newArtefact, workflowRunInputArtefact.role)
                }
            }

            oldWorkflowArtefact.state = WorkflowArtefact.State.FAILED
            oldWorkflowArtefact.save(flush: true)
        }
    }

    @CompileDynamic
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

    @CompileDynamic
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

    @CompileDynamic
    List<Workflow> findAllFastqcWorkflows() {
        return Workflow.createCriteria().list {
            isNull("deprecatedDate")
            "in"("name", FASTQC_WORKFLOWS)
            order("beanName", "asc")
        } as List<Workflow>
    }

    @CompileDynamic
    List<Workflow> findAllAlignmentWorkflows() {
        List<String> alignmentWorkflowNameList = alignmentWorkflowNames
        return alignmentWorkflowNameList ? Workflow.findAllByBeanNameInListAndDeprecatedDateIsNull(alignmentWorkflowNameList).sort { it.name } : []
    }

    Workflow findAlignmentWorkflowsForSeqType(SeqType seqType) {
        List<String> alignmentWorkflowNameList = alignmentWorkflowNames
        return seqType ? CollectionUtils.atMostOneElement(
                workflowVersionService.findAllByWorkflowSeqTypeAndReferenceGenome(null, seqType, null)*.workflow.findAll {
                    it.deprecatedDate == null && it.beanName in alignmentWorkflowNameList
                }.unique()
        ) : null
    }

    private List<String> getAlignmentWorkflowNames() {
        Map<String, OtpWorkflow> workflowBeans = applicationContext.getBeansOfType(OtpWorkflow)
        return workflowBeans.findAll { it.value.isAlignment() }*.key
    }

    @CompileDynamic
    List<Workflow> findAllAnalysisWorkflows() {
        Map<String, OtpWorkflow> workflowBeans = applicationContext.getBeansOfType(OtpWorkflow)
        List<String> analysisWorkflowNames = workflowBeans.findAll { it.value.isAnalysis() }*.key
        return analysisWorkflowNames ? Workflow.findAllByBeanNameInListAndDeprecatedDateIsNull(analysisWorkflowNames).sort { it.name } : []
    }

    @CompileDynamic
    void enableWorkflow(Workflow workflow) {
        assert workflow
        workflow.enabled = true
        workflow.save(flush: true)
    }

    @CompileDynamic
    void disableWorkflow(Workflow workflow) {
        assert workflow
        workflow.enabled = false
        workflow.save(flush: true)
    }

    @CompileDynamic
    List<Workflow> list() {
        return Workflow.list()
    }

    @CompileDynamic
    List<Workflow> findAllByDeprecatedDateIsNull() {
        return Workflow.findAllByDeprecatedDateIsNull()
    }

    @CompileDynamic
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
        return otpWorkflowService.lookupOtpWorkflowBean(workflow)?.isAlignment()
    }

    /**
     * returns if a given workflow is a FastQc workflow
     */
    boolean isFastqc(Workflow workflow) {
        return workflow.name in FASTQC_WORKFLOWS
    }
}
