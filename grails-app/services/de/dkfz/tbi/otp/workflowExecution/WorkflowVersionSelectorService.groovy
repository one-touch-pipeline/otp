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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project

import java.time.LocalDate

@Transactional
class WorkflowVersionSelectorService {

    OtpWorkflowService otpWorkflowService

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
    WorkflowVersionSelector findByProjectAndWorkflowAndSeqType(Project project, Workflow workflow, SeqType seqType) {
        return WorkflowVersionSelector.createCriteria().get {
            isNull('deprecationDate')
            eq('seqType', seqType)
            eq('project', project)
            workflowVersion {
                apiVersion {
                    eq("workflow", workflow)
                }
            }
        } as WorkflowVersionSelector
    }

    @CompileDynamic
    WorkflowVersionSelector createOrUpdate(Project project, SeqType seqType, WorkflowVersion version) {
        WorkflowVersionSelector previous = findByProjectAndWorkflowAndSeqType(project, version.apiVersion.workflow, seqType)
        if (previous) {
            previous.deprecationDate = LocalDate.now()
            previous.save(flush: true)
        }
        if (version) {
            return new WorkflowVersionSelector(
                    project: project,
                    seqType: seqType,
                    workflowVersion: version,
                    previous: previous,
            ).save(flush: true)
        }
        return previous
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
        return WorkflowVersionSelector.findAllByProjectAndSeqTypeAndDeprecationDateIsNull(project, seqType).find {
            otpWorkflows.contains(it.workflowVersion.workflow.beanName)
        }
    }
}
