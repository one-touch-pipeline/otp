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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.exceptions.OtpAssertRuntimeException
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog
import de.dkfz.tbi.otp.workflowExecution.wes.WesRun

@Transactional
class WorkflowDeletionService {

    WorkflowLogService workflowLogService

    @CompileDynamic
    void deleteWorkflowRun(WorkflowRun workflowRun) {
        workflowRun.workflowSteps?.reverse()?.each {
            deleteWorkflowStep(it)
        }

        WorkflowArtefact.findAllByProducedBy(workflowRun).each {
            deleteWorkflowArtefact(it)
        }

        WorkflowRunInputArtefact.findAllByWorkflowRun(workflowRun).each {
            it.delete(flush: true)
        }

        WorkflowStepSkipMessage skipMessage = workflowRun.skipMessage
        WorkFolder workFolder = workflowRun.workFolder
        workflowRun.delete(flush: true)

        deleteSkipMessage(skipMessage)
        deleteWorkFolder(workFolder)
    }

    @CompileDynamic
    void deleteWorkFolder(WorkFolder workFolder) {
        if (workFolder) {
            workFolder.delete(flush: true)
        }
    }

    @CompileDynamic
    void deleteWorkflowArtefact(WorkflowArtefact workflowArtefact) {
        WorkflowRunInputArtefact.findAllByWorkflowArtefact(workflowArtefact).each {
            deleteWorkflowRun(it.workflowRun)
        }
        if (!workflowArtefact.artefact.isEmpty()) {
            throw new OtpAssertRuntimeException("artefact for workflowArtefact ${workflowArtefact} still exist: ${workflowArtefact.artefact.get()}")
        }
        workflowArtefact.delete(flush: true)
    }

    @CompileDynamic
    void deleteWorkflowStep(WorkflowStep workflowStep) {
        if (WorkflowStep.findAllByPrevious(workflowStep) || WorkflowStep.findAllByRestartedFrom(workflowStep)) {
            throw new IllegalArgumentException()
        }
        workflowStep.workflowRun.workflowSteps.remove(workflowStep)
        workflowLogService.findAllByWorkflowStepInCorrectOrder(workflowStep).each {
            deleteWorkflowLog(it)
        }
        ClusterJob.findAllByWorkflowStep(workflowStep, [sort: 'id', order: 'desc']).each {
            it.delete(flush: true)
        }
        WorkflowError error = workflowStep.workflowError
        WesRun.findAllByWorkflowStep(workflowStep).forEach { deleteWesRun(it) }
        workflowStep.delete(flush: true)

        deleteWorkflowError(error)
    }

    @CompileDynamic
    void deleteWesRun(WesRun wesRun) {
        if (wesRun) {
            wesRun.delete(flush: true)
        }
    }

    @CompileDynamic
    void deleteWorkflowError(WorkflowError workflowError) {
        if (workflowError) {
            workflowError.delete(flush: true)
        }
    }

    @CompileDynamic
    void deleteSkipMessage(WorkflowStepSkipMessage skipMessage) {
        if (skipMessage) {
            skipMessage.delete(flush: true)
        }
    }

    @CompileDynamic
    void deleteWorkflowLog(WorkflowLog workflowLog) {
        if (workflowLog) {
            workflowLog.delete(flush: true)
        }
    }

    @CompileDynamic
    void deleteWorkflowVersionSelector(Project project) {
        WorkflowVersionSelector.findAllByProject(project, [sort: 'id', order: 'desc'])
                .each { it.delete(flush: true) }
    }

    @CompileDynamic
    void deleteReferenceGenomeSelector(Project project) {
        ReferenceGenomeSelector.findAllByProject(project, [sort: 'id', order: 'desc'])
                .each { it.delete(flush: true) }
    }

    @CompileDynamic
    void deleteWorkflowRun(Project project) {
        WorkflowRun.findAllByProject(project, [sort: 'id', order: 'desc'])
                .each { deleteWorkflowRun(it) }
    }
}
