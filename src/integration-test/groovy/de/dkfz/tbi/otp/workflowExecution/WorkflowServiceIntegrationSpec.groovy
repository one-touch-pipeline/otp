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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

@Rollback
@Integration
class WorkflowServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    WorkflowService service

    void setup() {
        service = new WorkflowService()
    }

    void "test, createRestartedWorkflow should create a copy of the old workflowArtefacts"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        workflowStep.workflowRun.state = WorkflowRun.State.FAILED
        WorkflowArtefact wa1 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: workflowStep.workflowRun, outputRole: "asdf")
        WorkflowArtefact wa2 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: workflowStep.workflowRun, outputRole: "qwertz")
        workflowStep.workflowRun.save(flush: true)

        when:
        service.createRestartedWorkflow(workflowStep, false)

        then:
        noExceptionThrown()
        WorkflowArtefact.count() == 4
        WorkflowArtefact.findAllByProducedBy(workflowStep.workflowRun) == [wa1, wa2]
    }
}
