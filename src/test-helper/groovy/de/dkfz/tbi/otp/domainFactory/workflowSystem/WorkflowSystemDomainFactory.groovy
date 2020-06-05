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
import de.dkfz.tbi.otp.workflowExecution.*

trait WorkflowSystemDomainFactory implements DomainFactoryCore {

    Workflow createWorkflow(Map properties = [:]) {
        return createDomainObject(Workflow, [
                name   : "name_${nextId}",
                enabled: true,
        ], properties)
    }

    WorkflowRun createWorkflowRun(Map properties = [:]) {
        return createDomainObject(WorkflowRun, [
                workflow: { createWorkflow() },
                priority: { createProcessingPriority() },
        ], properties)
    }

    WorkflowStep createWorkflowStep(Map properties = [:]) {
        WorkflowStep step = createDomainObject(WorkflowStep, [
                workflowRun: { createWorkflowRun() },
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
                individual: { createIndividual() },
                seqType: { createSeqType() },
        ], properties)
    }

    WorkflowRunInputArtefact createWorkflowRunInputArtefact(Map properties = [:]) {
        return createDomainObject(WorkflowRunInputArtefact, [
                workflowRun     : { createWorkflowRun() },
                role            : "role_${nextId}",
                workflowArtefact: { createWorkflowArtefact() },
        ], properties)
    }
}

class WorkflowSystemDomainFactoryInstance implements WorkflowSystemDomainFactory {

    final static WorkflowSystemDomainFactoryInstance INSTANCE = new WorkflowSystemDomainFactoryInstance()
}
