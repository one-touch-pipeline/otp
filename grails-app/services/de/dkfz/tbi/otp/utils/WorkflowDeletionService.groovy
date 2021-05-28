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

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog

@Transactional
class WorkflowDeletionService {

    void deleteWorkflowRun(WorkflowRun workflowRun) {
        workflowRun.workflowSteps?.reverse()?.each {
            deleteWorkflowStep(it)
        }

        WorkflowArtefact.findAllByProducedBy(workflowRun).each {
            deleteWorkflowArtefact(it)
        }

        OmittedMessage omittedMessage = workflowRun.omittedMessage

        workflowRun.delete(flush: true)

        deleteOmittedMessage(omittedMessage)
    }

    void deleteWorkflowArtefact(WorkflowArtefact workflowArtefact) {
        WorkflowRunInputArtefact.findAllByWorkflowArtefact(workflowArtefact).each {
            WorkflowRun workflowRun = it.workflowRun
            it.delete(flush: true)
            deleteWorkflowRun(workflowRun)
        }
        workflowArtefact.artefact.ifPresent { it.delete(flush: true) }
        workflowArtefact.delete(flush: true)
    }

    void deleteWorkflowStep(WorkflowStep workflowStep) {
        if (WorkflowStep.findAllByPrevious(workflowStep) || WorkflowStep.findAllByRestartedFrom(workflowStep)) {
            throw new IllegalArgumentException()
        }
        workflowStep.workflowRun.workflowSteps.remove(workflowStep)
        workflowStep.logs.each {
            deleteWorkflowLog(it)
        }
        ClusterJob.findAllByWorkflowStep(workflowStep).each {
            it.delete(flush: true)
        }
        WorkflowError error = workflowStep.workflowError

        workflowStep.delete(flush: true)

        deleteWorkflowError(error)
    }

    void deleteWorkflowError(WorkflowError workflowError) {
        if (workflowError) {
            workflowError.delete(flush: true)
        }
    }

    void deleteOmittedMessage(OmittedMessage omittedMessage) {
        if (omittedMessage) {
            omittedMessage.delete(flush: true)
        }
    }

    void deleteWorkflowLog(WorkflowLog workflowLog) {
        if (workflowLog) {
            workflowLog.delete(flush: true)
        }
    }

    void deleteActiveProjectWorkflows(Project project) {
        List<ActiveProjectWorkflow> activeProjectWorkflowList = ActiveProjectWorkflow.findAllByProject(project).sort { -it.id }
        if (activeProjectWorkflowList) {
            ReferenceGenomeSelector referenceGenomeSelector = CollectionUtils.exactlyOneElement(
                    ReferenceGenomeSelector.findAll {
                        activeProjectWorkflowList.contains(activeProjectWorkflows)
                    }
            )

            activeProjectWorkflowList.each {
                referenceGenomeSelector.activeProjectWorkflows.remove(it)
                it.delete(flush: true)
            }
            deleteReferenceGenomeSelector(referenceGenomeSelector)
        }
    }

    void deleteReferenceGenomeSelector(ReferenceGenomeSelector referenceGenomeSelector) {
        if (referenceGenomeSelector) {
            referenceGenomeSelector.delete(flush: true)
        }
    }
}
