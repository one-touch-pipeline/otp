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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

class WorkflowSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    static final String WORKFLOW_NAME = "WORKFLOW"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
        ]
    }

    void "validate, when workflow name not exist yet, then object is valid"() {
        Workflow workflow = createWorkflow([
                name: WORKFLOW_NAME,
        ], false)

        when:
        workflow.validate()

        then:
        workflow.errors.errorCount == 0
    }

    void "validate, when already a workflow with the name exist, then object is invalid"() {
        createWorkflow([
                name: WORKFLOW_NAME,
        ])
        Workflow workflow = createWorkflow([
                name: WORKFLOW_NAME,
        ], false)

        expect:
        TestCase.assertValidateError(workflow, 'name', 'unique', WORKFLOW_NAME)
    }

    void "validate, when saved object is validate again, then the object is still valid"() {
        Workflow workflow = createWorkflow([
                name: WORKFLOW_NAME,
        ])

        when:
        workflow.validate()

        then:
        workflow.errors.errorCount == 0
    }
}
