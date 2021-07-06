/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.shared.NoArtefactOfRoleException
import de.dkfz.tbi.otp.workflow.shared.NoConcreteArtefactException
import de.dkfz.tbi.otp.workflow.shared.WrongWorkflowException
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class PanCancerService {

    static final String WORKFLOW = "PanCancer alignment"

    def <T> T getOutputArtefact(WorkflowStep workflowStep, String outputRoleName) {
        WorkflowRun workflowRun = workflowStep.workflowRun
        if (workflowRun.workflow.name != WORKFLOW) {
            throw new WrongWorkflowException("The step is from workflow ${workflowRun.workflow.name}, but expected is ${WORKFLOW}")
        }
        WorkflowArtefact workflowArtefact = workflowRun.outputArtefacts[outputRoleName]
        if (!workflowArtefact) {
            throw new NoArtefactOfRoleException("The WorkflowRun ${workflowRun} has no input artefacts of role " +
                    "${outputRoleName}, only ${workflowRun.outputArtefacts.keySet().sort()}")
        }
        Optional<Artefact> optionalArtefact = workflowArtefact.artefact
        if (!optionalArtefact.isPresent()) {
            throw new NoConcreteArtefactException("The WorkflowArtefact ${workflowArtefact} of WorkflowRun ${workflowRun} has no concrete artefact yet")
        }
        return optionalArtefact.get() as T
    }

    def <T> T getInputArtefact(WorkflowStep workflowStep, String inputRoleName) {
        return CollectionUtils.atMostOneElement(getInputArtefacts(workflowStep, inputRoleName))
    }

    def <T> List<T> getInputArtefacts(WorkflowStep workflowStep, String inputRoleName) {
        WorkflowRun workflowRun = workflowStep.workflowRun
        if (workflowRun.workflow.name != WORKFLOW) {
            throw new WrongWorkflowException("The step is from workflow ${workflowRun.workflow.name}, but expected is ${WORKFLOW}")
        }
        List<WorkflowArtefact> workflowArtefacts = workflowRun.inputArtefacts.findAll {
            it.key.startsWith(inputRoleName)
        }.collect {
            it.value
        }
        if (!workflowArtefacts) {
            throw new NoArtefactOfRoleException("The WorkflowRun ${workflowRun} has no input artefacts of role " +
                    "${inputRoleName}, only ${workflowRun.inputArtefacts.keySet().sort()}")
        }
        List<Optional<Artefact>> optionalArtefacts = workflowArtefacts.collect {
            it.artefact
        }
        optionalArtefacts.forEach {
            if (!it.isPresent()) {
                throw new NoConcreteArtefactException("The WorkflowArtefact ${it} of WorkflowRun ${workflowRun} has no concrete artefact yet")
            }
        }
        return optionalArtefacts.collect {
            it.get() as T
        }
    }
}
