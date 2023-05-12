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
package de.dkfz.tbi.otp.domainFactory.workflowSystem

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.*
import de.dkfz.tbi.otp.workflowExecution.wes.WesLog
import de.dkfz.tbi.otp.workflowExecution.wes.WesRun
import de.dkfz.tbi.otp.workflowExecution.wes.WesRunLog

import io.swagger.client.wes.model.State

import java.time.LocalDateTime
import java.time.ZonedDateTime

trait WorkflowSystemDomainFactory implements DomainFactoryCore, TaxonomyFactory {

    final static int MAX_PARALLEL_WORKFLOWS = 5

    Workflow createWorkflow(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Workflow, [
                name                : "name_${nextId}",
                beanName            : "beanName_${nextId}",
                enabled             : true,
                maxParallelWorkflows: MAX_PARALLEL_WORKFLOWS,
        ], properties, saveAndValidate)
    }

    Workflow findOrCreateWorkflow(String workflow, Map properties = [:]) {
        return findOrCreateDomainObject(Workflow, [
                name: workflow,
        ], [
                beanName            : "beanName_${nextId}",
                enabled             : true,
                maxParallelWorkflows: MAX_PARALLEL_WORKFLOWS,
        ], properties)
    }

    WorkflowRun createWorkflowRun(Map properties = [:]) {
        Workflow workflow = properties.workflow ?: properties.restartedFrom?.workflow ?: properties.workflowVersion?.workflow ?: createWorkflow()
        return createDomainObject(WorkflowRun, [
                workflow        : { workflow },
                workflowVersion : { properties.restartedFrom?.workflowVersion ?: createWorkflowVersion(workflow: workflow) },
                priority        : { createProcessingPriority() },
                project         : { createProject() },
                workDirectory   : "dir_${nextId}",
                displayName     : "displayName_${nextId}",
                shortDisplayName: "shortName_${nextId}",
                combinedConfig  : "{},"
        ], properties)
    }

    WorkflowStep createWorkflowStep(Map properties = [:]) {
        WorkflowStep step = createDomainObject(WorkflowStep, [
                workflowRun: { properties.restartedFrom?.workflowRun ?: properties.previous?.workflowRun ?: createWorkflowRun() },
                beanName   : { properties.restartedFrom?.beanName ?: "beanName_${nextId}" },
                state      : WorkflowStep.State.CREATED,
        ], properties, false)
        //it is necessary to add the step in the list of workflowRuns before saving
        //otherwise hibernate try to save the step with null for workflow run, which will fail with sql exception
        step.workflowRun.addToWorkflowSteps(step)
        return step.save(flush: true)
    }

    WesRun createWesRun(Map properties = [:]) {
        return createDomainObject(WesRun, [
                workflowStep: { createWorkflowStep() },
                wesIdentifier: "wes_identifier_${nextId}",
                subPath: "",
                state: WesRun.MonitorState.CHECKING,
                wesRunLog: { createWesRunLog() },
        ], properties)
    }

    WesRunLog createWesRunLog(Map properties = [:]) {
        return createDomainObject(WesRunLog, [
                state: State.QUEUED,
                runLog: { createWesLog() },
                taskLogs: { [ createWesLog() ] },
                runRequest: null,
        ], properties)
    }

    WesLog createWesLog(Map properties = [:]) {
        return createDomainObject(WesLog, [
                name: "name_${nextId}",
                cmd: "cmd_${nextId}",
                startTime: { LocalDateTime.now() },
                endTime: { LocalDateTime.now() },
                stdout: "stdout_${nextId}",
                stderr: "stderr_${nextId}",
                exitCode: 0,
        ], properties)
    }

    WorkflowArtefact createWorkflowArtefact(Map properties = [:]) {
        return createDomainObject(WorkflowArtefact, [
                artefactType: ArtefactType.FASTQ,
                displayName : "displayName_${nextId}",
                outputRole  : { properties.producedBy ? "role ${nextId}" : null },
        ], properties)
    }

    WorkflowRunInputArtefact createWorkflowRunInputArtefact(Map properties = [:]) {
        return createDomainObject(WorkflowRunInputArtefact, [
                workflowRun     : { createWorkflowRun() },
                role            : "role_${nextId}",
                workflowArtefact: { createWorkflowArtefact() },
        ], properties)
    }

    WorkflowJobErrorDefinition createWorkflowJobErrorDefinition(Map properties = [:]) {
        return createDomainObject(WorkflowJobErrorDefinition, [
                sourceType          : WorkflowJobErrorDefinition.SourceType.MESSAGE,
                action              : WorkflowJobErrorDefinition.Action.STOP,
                errorExpression     : "error_${nextId}",
                allowRestartingCount: nextId,
                jobBeanName         : "jobBeanName_${nextId}",
                name                : "name_${nextId}",
                beanToRestart       : { properties.action == WorkflowJobErrorDefinition.Action.RESTART_JOB ? "beanToRestart_${nextId}" : null },
        ], properties)
    }

    WorkflowError createWorkflowError(Map properties = [:]) {
        return createDomainObject(WorkflowError, [
                message   : "message_${nextId}",
                stacktrace: "stacktrace_${nextId}",
        ], properties)
    }

    ClusterJob createClusterJob(Map properties = [:]) {
        boolean oldSystem = properties.containsKey('oldSystem') ? properties.oldSystem : properties.containsKey('processingStep')
        return createDomainObject(ClusterJob, [
                validated     : false,
                oldSystem     : oldSystem,
                checkStatus   : ClusterJob.CheckStatus.CREATED,
                realm         : { createRealm() },
                clusterJobId  : "clusterJobId_${nextId}",
                userName      : "userName_${nextId}",
                clusterJobName: "clusterJobName_${nextId}_jobClass",
                jobClass      : "jobClass",
                processingStep: { oldSystem ? DomainFactory.createProcessingStep() : null },
                workflowStep  : { oldSystem ? null : createWorkflowStep() },
                queued        : ZonedDateTime.now(),
        ], properties)
    }

    WorkflowVersion createWorkflowVersion(Map properties = [:]) {
        return createDomainObject(WorkflowVersion, [
                workflow       : { createWorkflow() },
                workflowVersion: "${nextId}.0",
        ], properties)
    }

    ExternalWorkflowConfigSelector createExternalWorkflowConfigSelector(Map properties = [:], boolean saveAndValidate = true) {
        Set<Workflow> workflows = properties.workflows ?: properties.workflowVersions ? properties.workflowVersions*.workflow : [createWorkflow()] as Set
        return createDomainObject(ExternalWorkflowConfigSelector, [
                name                          : "externalWorkflowConfigSelectorName_${nextId}",
                workflowVersions              : { [createWorkflowVersion(workflow: workflows.first())] },
                workflows                     : workflows,
                referenceGenomes              : { [createReferenceGenome()] },
                libraryPreparationKits        : { [createLibraryPreparationKit()] },
                seqTypes                      : { [createSeqType()] },
                projects                      : { [createProject()] },
                externalWorkflowConfigFragment: { createExternalWorkflowConfigFragment() },
                selectorType                  : { SelectorType.GENERIC },
        ], properties, saveAndValidate)
    }

    ExternalWorkflowConfigFragment createExternalWorkflowConfigFragment(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ExternalWorkflowConfigFragment, [
                name        : "externalWorkflowConfigFragmentName_${nextId}",
                configValues: "{ \"WORKFLOWS\": { \"MEMORY_${nextId}\": \"1\" } }",
                previous    : null,
        ], properties, saveAndValidate)
    }

    WorkflowMessageLog createWorkflowMessageLog(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(WorkflowMessageLog, [
                workflowStep: { createWorkflowStep() },
                createdBy   : "domainFactory",
                message     : "message_${nextId}",
        ], properties, saveAndValidate)
    }

    WorkflowCommandLog createWorkflowCommandLog(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(WorkflowCommandLog, [
                workflowStep: { createWorkflowStep() },
                createdBy   : "domainFactory",
                command     : "command_${nextId}",
                exitCode    : 1L,
                stdout      : "stdout_${nextId}",
                stderr      : "stderr_${nextId}",
        ], properties, saveAndValidate)
    }

    OmittedMessage createOmittedMessage(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(OmittedMessage, [
                category: OmittedMessage.Category.PREREQUISITE_WORKFLOW_RUN_NOT_SUCCESSFUL,
                message : "message_${nextId}",
        ], properties, saveAndValidate)
    }

    WorkflowVersionSelector createWorkflowVersionSelector(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(WorkflowVersionSelector, [
                project        : { createProject() },
                seqType        : { createSeqType() },
                workflowVersion: { createWorkflowVersion() },
        ], properties, saveAndValidate)
    }

    ReferenceGenomeSelector createReferenceGenomeSelector(Map properties = [:], boolean saveAndValidate = true) {
        ReferenceGenome referenceGenome = properties.referenceGenome ?: createReferenceGenome(properties.species ? [
                speciesWithStrain: new HashSet<SpeciesWithStrain>(properties.species),
                species          : null,
        ] : [species: null])

        List<SpeciesWithStrain> speciesWithStrain = referenceGenome.species ? SpeciesWithStrain.findAllBySpeciesInList(referenceGenome.species as List) : []
        return createDomainObject(ReferenceGenomeSelector, [
                project        : { createProject() },
                referenceGenome: referenceGenome,
                seqType        : { createSeqType() },
                species        : { new HashSet<SpeciesWithStrain>(referenceGenome.speciesWithStrain + speciesWithStrain) },
                workflow       : { createWorkflow() },
        ], properties, saveAndValidate)
    }
}

class WorkflowSystemDomainFactoryInstance implements WorkflowSystemDomainFactory {

    final static WorkflowSystemDomainFactoryInstance INSTANCE = new WorkflowSystemDomainFactoryInstance()
}
