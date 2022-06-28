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
package de.dkfz.tbi.otp.workflow

import grails.testing.gorm.DataTest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class WorkflowSharedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    final static String FALSELY_WORK_FLOW_NAME = "falselyWorkFlowName"
    final static String TEST_WORKFLOW_NAME = "testWorkflowName"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
        ]
    }

    @Shared
    WorkflowShared workflowSharedInstance = new Object() as WorkflowShared

    @Unroll
    void "checkWorkflowName, should do nothing if workflow name is allowed"() {
        given:
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: TEST_WORKFLOW_NAME
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])

        when:
        if (isList) {
            workflowSharedInstance.checkWorkflowName(workflowStep, [TEST_WORKFLOW_NAME])
        } else {
            workflowSharedInstance.checkWorkflowName(workflowStep, TEST_WORKFLOW_NAME)
        }

        then:
        _

        where:
        isList << [true, false]
    }

    @Unroll
    void "checkWorkflowName, should fail with assertion error if workflow name is not allowed"() {
        given:
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: FALSELY_WORK_FLOW_NAME
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])

        when:
        if (isList) {
            workflowSharedInstance.checkWorkflowName(workflowStep, [TEST_WORKFLOW_NAME])
        } else {
            workflowSharedInstance.checkWorkflowName(workflowStep, TEST_WORKFLOW_NAME)
        }

        then:
        thrown(AssertionError)

        where:
        isList << [true, false]
    }
}
