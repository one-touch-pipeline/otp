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

import org.joda.time.DateTime

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError

trait WorkflowSystemDomainFactory implements DomainFactoryCore {

    Workflow createWorkflow(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(Workflow, [
                name    : "name_${nextId}",
                beanName: "beanName_${nextId}",
                enabled : true,
        ], properties, saveAndValidate)
    }

    WorkflowRun createWorkflowRun(Map properties = [:]) {
        return createDomainObject(WorkflowRun, [
                workflow   : { properties.restartedFrom?.workflow ?: createWorkflow() },
                priority   : { createProcessingPriority() },
                project    : { createProject() },
                displayName: "displayName_${nextId}",
        ], properties)
    }

    WorkflowStep createWorkflowStep(Map properties = [:]) {
        WorkflowStep step = createDomainObject(WorkflowStep, [
                workflowRun: { properties.restartedFrom?.workflowRun ?: createWorkflowRun() },
                beanName   : "beanName_${nextId}",
                state      : WorkflowStep.State.CREATED,
        ], properties, false)
        //it is necessary to add the step in the list of workflowRuns before saving
        //otherwise hibernate try to save the step with null for workflow run, which will fail with sql exception
        step.workflowRun.addToWorkflowSteps(step)
        step.save(flush: true)
    }

    WorkflowArtefact createWorkflowArtefact(Map properties = [:]) {
        return createDomainObject(WorkflowArtefact, [
                individual : { createIndividual() },
                seqType    : { createSeqType() },
                displayName: "displayName_${nextId}",
                outputRole: { properties.producedBy ? "role ${nextId}" : null },
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
                realm         : { DomainFactory.createRealm() },
                clusterJobId  : "clusterJobId_${nextId}",
                userName      : "userName_${nextId}",
                clusterJobName: "clusterJobName_${nextId}_jobClass",
                jobClass      : "jobClass",
                processingStep: { oldSystem ? DomainFactory.createProcessingStep() : null },
                workflowStep  : { oldSystem ? null : createWorkflowStep() },
                queued        : new DateTime(),
        ], properties)
    }

    WorkflowVersion createWorkflowVersion(Map properties = [:]) {
        return createDomainObject(WorkflowVersion, [
                workflow: { createWorkflow() },
                workflowVersion: "${nextId}.0",
        ], properties)
    }

    ExternalWorkflowConfigSelector createExternalWorkflowConfigSelector(Map properties = [:]) {
        return createDomainObject(ExternalWorkflowConfigSelector, [
                name: "externalWorkflowConfigSelectorName_${nextId}",
                workflowVersions: { [createWorkflowVersion()] },
                workflows: { [createWorkflow()] },
                referenceGenomes: { [createReferenceGenome()] },
                libraryPreparationKits: { [createLibraryPreparationKit()] },
                seqTypes: { [createSeqType()] },
                projects: { [createProject()] },
                externalWorkflowConfigFragment: { createExternalWorkflowConfigFragment() },
                selectorType: { SelectorType.GENERIC },
                basePriority: nextId,
                fineTuningPriority: nextId,
        ], properties)
    }

    ExternalWorkflowConfigFragment createExternalWorkflowConfigFragment(Map properties = [:]) {
        return createDomainObject(ExternalWorkflowConfigFragment, [
                name: "externalWorkflowConfigFragmentName_${nextId}",
                configValues: '{ "TestConfigKey": "TestConfigValue" }',
                previous: null,
        ], properties)
    }

}

class WorkflowSystemDomainFactoryInstance implements WorkflowSystemDomainFactory {

    final static WorkflowSystemDomainFactoryInstance INSTANCE = new WorkflowSystemDomainFactoryInstance()
}
