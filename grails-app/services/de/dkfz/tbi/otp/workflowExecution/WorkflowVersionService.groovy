/*
 * Copyright 2011-2022 The OTP authors
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
import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.MergingCriteriaService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType

import java.time.LocalDate

@CompileDynamic
@Transactional
class WorkflowVersionService {

    CommentService commentService
    MergingCriteriaService mergingCriteriaService

    List<WorkflowVersion> list() {
        return WorkflowVersion.list()
    }

    List<WorkflowVersion> findAllByWorkflow(Workflow workflow) {
        return WorkflowVersion.createCriteria().list {
            apiVersion {
                eq('workflow', workflow)
            }
        }
    }

    List<WorkflowVersion> findAllByWorkflows(List<Workflow> workflows) {
        if (!workflows) {
            return []
        }
        return WorkflowVersion.createCriteria().list {
            apiVersion {
                'in'("workflow", workflows)
            }
        } as List<WorkflowVersion>
    }

    List<WorkflowVersion> findAllByWorkflowId(Long workflowId) {
        return WorkflowVersion.createCriteria().list {
            apiVersion {
                workflow {
                    eq("id", workflowId)
                }
            }
        } as List<WorkflowVersion>
    }

    List<WorkflowVersion> findAllByWorkflowSeqTypeAndReferenceGenome(Workflow workflow, SeqType seqType, ReferenceGenome referenceGenome) {
        return WorkflowVersion.createCriteria().list {
            if (workflow) {
                apiVersion {
                    eq("workflow", workflow)
                }
            }
            if (seqType) {
                supportedSeqTypes {
                    eq('id', seqType.id)
                }
            }
            if (referenceGenome) {
                allowedReferenceGenomes {
                    eq('id', referenceGenome.id)
                }
            }
        } as List<WorkflowVersion>
    }

    WorkflowVersion updateWorkflowVersion(UpdateWorkflowVersionDto updateDto) {
        WorkflowVersion workflowVersion = WorkflowVersion.get(updateDto.workflowVersionId)
        commentService.saveComment(workflowVersion, updateDto.comment)
        workflowVersion.allowedReferenceGenomes = updateDto.allowedRefGenomes.collect { id -> ReferenceGenome.get(id) }
        workflowVersion.supportedSeqTypes = updateDto.supportedSeqTypes.collect { id -> SeqType.get(id) }
        workflowVersion.deprecatedDate = updateDto.deprecate ? (workflowVersion.deprecatedDate ?: LocalDate.now()) : null
        workflowVersion.supportedSeqTypes.each {
            mergingCriteriaService.createDefaultMergingCriteria(it)
        }

        return workflowVersion.save(flush: true)
    }
}
