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
package de.dkfz.tbi.otp.workflow

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.shared.NoArtefactOfRoleException
import de.dkfz.tbi.otp.workflow.shared.NoConcreteArtefactException
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Transactional
class ConcreteArtefactService {

    def <T> T getOutputArtefact(WorkflowStep workflowStep, String outputRoleName, boolean required = true) {
        return CollectionUtils.atMostOneElement(getOutputArtefacts(workflowStep, outputRoleName, required))
    }

    def <T> List<T> getOutputArtefacts(WorkflowStep workflowStep, String outputRoleName, boolean required = true) {
        WorkflowRun workflowRun = workflowStep.workflowRun
        List<WorkflowArtefact> workflowArtefacts = workflowRun.outputArtefacts.findAll {
            it.key ==~ /^${outputRoleName}(_\d+)?$/
        }*.value
        if (!workflowArtefacts) {
            if (required) {
                throw new NoArtefactOfRoleException("The WorkflowRun ${workflowRun} has no output artefacts of role " +
                        "${outputRoleName}, only ${workflowRun.outputArtefacts.keySet().sort()}")
            } else {
                return []
            }
        }
        workflowArtefacts.each {
            if (!it.artefact.isPresent()) {
                throw new NoConcreteArtefactException("The WorkflowArtefact ${it} of WorkflowRun ${workflowRun} has no concrete artefact yet")
            }
        }
        return workflowArtefacts*.artefact.collect {
            it.get() as T
        }
    }

    def <T> T getInputArtefact(WorkflowStep workflowStep, String inputRoleName, boolean required = true) {
        return CollectionUtils.atMostOneElement(getInputArtefacts(workflowStep, inputRoleName, required))
    }

    def <T> List<T> getInputArtefacts(WorkflowStep workflowStep, String inputRoleName, boolean required = true) {
        WorkflowRun workflowRun = workflowStep.workflowRun
        List<WorkflowArtefact> workflowArtefacts = workflowRun.inputArtefacts.findAll {
            it.key ==~ /^${inputRoleName}(_\d+)?$/
        }*.value
        if (!workflowArtefacts) {
            if (required) {
                throw new NoArtefactOfRoleException("The WorkflowRun ${workflowRun} has no input artefacts of role " +
                        "${inputRoleName}, only ${workflowRun.inputArtefacts.keySet().sort()}")
            } else {
                return []
            }
        }
        workflowArtefacts.each {
            if (!it.artefact.isPresent()) {
                throw new NoConcreteArtefactException("The WorkflowArtefact ${it} of WorkflowRun ${workflowRun} has no concrete artefact yet")
            }
        }
        return workflowArtefacts*.artefact.collect {
            it.get() as T
        }
    }
}
