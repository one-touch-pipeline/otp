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

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project

import java.time.LocalDate

@Transactional
class WorkflowVersionSelectorService {

    OtpWorkflowService otpWorkflowService
    WorkflowService workflowService

    @CompileDynamic
    List<WorkflowVersionSelector> findAllByProjectAndWorkflow(Project project, Workflow workflow) {
        return WorkflowVersionSelector.createCriteria().list {
            isNull('deprecationDate')
            eq('project', project)
            workflowVersion {
                apiVersion {
                    eq("workflow", workflow)
                }
            }
        } as List<WorkflowVersionSelector>
    }

    @CompileDynamic
    WorkflowVersionSelector findByProjectSeqTypeWorkflow(Project project, SeqType seqType, Workflow workflow) {
        return WorkflowVersionSelector.createCriteria().get {
            isNull('deprecationDate')
            eq('project', project)
            if (seqType) {
                eq('seqType', seqType)
            } else {
                isNull('seqType')
            }
            workflowVersion {
                apiVersion {
                    eq("workflow", workflow)
                }
            }
        } as WorkflowVersionSelector
    }

    @CompileDynamic
    WorkflowVersionSelector createOrUpdate(Project project, SeqType seqType, WorkflowVersion version) {
        assert project: "Parameter project must not be null."
        assert version: "Parameter version must not be null."

        WorkflowVersionSelector previous = findByProjectSeqTypeWorkflow(project, seqType, version.apiVersion.workflow)
        if (previous) {
            if (previous.workflowVersion == version) {
                return previous
            }
            previous.deprecationDate = LocalDate.now()
            previous.save(flush: true)
        }
        return new WorkflowVersionSelector(
                project: project,
                seqType: seqType,
                workflowVersion: version,
                previous: previous,
        ).save(flush: true)
    }

    /**
     * Update the fastqc workflow version for the given project.
     * Since only one fastqc workflow is allowed per project, all other fastqc workflow selectors will be deprecated.
     * If version is null, the current fastqc workflow selector for the project will be deprecated.
     * @param project is the current project
     * @param workflow is the current workflow, must be a fastqc workflow
     * @param version is the new workflow version, `null` means deprecate the current fastqc workflow selector
     * @return the created or updated WorkflowVersionSelector, or null if version is null
     */
    WorkflowVersionSelector updateFastqcVersion(Project project, Workflow workflow, WorkflowVersion version) {
        if (!workflowService.isFastqc(workflow)) {
            return null
        }

        if (!version) { // deprecate this fastqc workflow selector
            findAllByProjectAndWorkflow(project, workflow).each { deprecateSelectorIfUnused(it) }
            return null
        }

        assert version.apiVersion.workflow == workflow : "The provided version ${version} does not belong to the provided workflow ${workflow}."
        deprecateOtherFastqcWorkflowSelectors(project, workflow)

        return createOrUpdate(project, null, version)
    }

    /**
     * Deprecate all other fastqc workflow selectors for the given project.
     * If the current workflow is not a fastqc workflow, nothing will be done.
     * @param project is the current project
     * @param currentWorkflow is the current workflow, must be a fastqc workflow
     */
    @CompileDynamic
    void deprecateOtherFastqcWorkflowSelectors(Project project, Workflow currentWorkflow) {
        // Do nothing if current workflow is not a FastQC workflow
        if (!workflowService.isFastqc(currentWorkflow)) {
            return
        }

        // Find other FastQC currentWorkflow selectors for the same project and seqType
        List<WorkflowVersionSelector> otherFastqcSelectors = WorkflowVersionSelector.createCriteria().list {
            isNull('deprecationDate')
            eq('project', project)
            workflowVersion {
                apiVersion {
                    workflow {
                        'in'('name', workflowService.FASTQC_WORKFLOWS)
                        ne('id', currentWorkflow.id)
                    }
                }
            }
        } as List<WorkflowVersionSelector>

        // Deprecate other FastQC selectors
        otherFastqcSelectors.each { selector ->
            selector.deprecationDate = LocalDate.now()
            selector.save(flush: true)
        }
    }

    @CompileDynamic
    void deprecateSelectorIfUnused(WorkflowVersionSelector wvSelector) {
        assert wvSelector
        List<ReferenceGenomeSelector> existingLinkedRgSelectors = ReferenceGenomeSelector
                .findAllBySeqTypeAndProjectAndWorkflow(wvSelector.seqType, wvSelector.project, wvSelector.workflowVersion.workflow)
        if (!existingLinkedRgSelectors) {
            wvSelector.deprecationDate = LocalDate.now()
            wvSelector.save(flush: true)
        }
    }

    @CompileDynamic
    boolean hasAlignmentConfigForProjectAndSeqType(Project project, SeqType seqType) {
        Set<String> otpWorkflows = otpWorkflowService.lookupAlignableOtpWorkflowBeans().keySet()
        return WorkflowVersionSelector.findAllByProjectAndSeqTypeAndDeprecationDateIsNull(project, seqType).any { selector ->
            otpWorkflows.contains(selector.workflowVersion.workflow.beanName)
        }
    }
}
