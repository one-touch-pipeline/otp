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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

@Rollback
@Integration
class WorkflowStepServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    void "runningWorkflowSteps, return only runningWorkflows"() {
        given:
        List<WorkflowStep> runningState = (1..3).collect {
            create(WorkflowStep.State.RUNNING, WorkflowRun.State.RUNNING)
        }
        WorkflowStep.State.values().each { WorkflowStep.State stepState ->
            WorkflowRun.State.values().each { WorkflowRun.State runState ->
                if (stepState != WorkflowStep.State.RUNNING && runState != WorkflowRun.State.RUNNING) {
                    create(stepState, runState)
                }
            }
        }

        when:
        List<WorkflowStep> ret = new WorkflowStepService().runningWorkflowSteps()

        then:
        TestCase.assertContainSame(ret, runningState)
    }

    private WorkflowStep create(WorkflowStep.State stepState, WorkflowRun.State runState) {
        return createWorkflowStep([
                workflowRun: createWorkflowRun([
                        state: runState,
                ]),
                state      : stepState,
        ])
    }
}
