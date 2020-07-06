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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

import java.time.LocalDate

class WorkflowSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    static final String WORKFLOW_NAME = "WORKFLOW"

    @Override
    Class[] getDomainClassesToMock() {
        [
                Workflow,
        ]
    }

    void "getExactlyOneWorkflow, when a non deprecated workflow of the name exist, then return that workflow"() {
        given:
        createWorkflow()
        createWorkflow([
                name          : WORKFLOW_NAME,
                deprecatedDate: LocalDate.now(),
        ])
        Workflow workflow = createWorkflow([
                name: WORKFLOW_NAME,
        ])

        when:
        Workflow returnedWorkflow = Workflow.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        returnedWorkflow == workflow
    }

    void "getExactlyOneWorkflow, when no workflow of the name exists, then throw an exception"() {
        createWorkflow()

        when:
        Workflow.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        thrown(AssertionError)
    }

    void "getExactlyOneWorkflow, when all workflows of the name are deprecated, then throw an exception"() {
        createWorkflow([
                name          : WORKFLOW_NAME,
                deprecatedDate: LocalDate.now(),
        ])
        createWorkflow([
                name          : WORKFLOW_NAME,
                deprecatedDate: LocalDate.now(),
        ])

        when:
        Workflow.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        thrown(AssertionError)
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

    void "validate, when only deprecated workflows of the name exist, then object is valid"() {
        createWorkflow([
                name          : WORKFLOW_NAME,
                deprecatedDate: LocalDate.now(),
        ])
        Workflow workflow = createWorkflow([
                name: WORKFLOW_NAME,
        ], false)

        when:
        workflow.validate()

        then:
        workflow.errors.errorCount == 0
    }

    void "validate, when already a non deprecate workflow exist, then object is invalid"() {
        createWorkflow([
                name: WORKFLOW_NAME,
        ])
        Workflow workflow = createWorkflow([
                name: WORKFLOW_NAME,
        ], false)

        expect:
        TestCase.assertValidateError(workflow, 'name', 'validator.not.unique.deprecate.message', WORKFLOW_NAME)
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
