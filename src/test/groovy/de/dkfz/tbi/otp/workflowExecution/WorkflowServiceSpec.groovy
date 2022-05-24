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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

import java.time.LocalDate

class WorkflowServiceSpec extends Specification implements ServiceUnitTest<WorkflowService>, WorkflowSystemDomainFactory, DataTest {

    static final String WORKFLOW_NAME = "WORKFLOW"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
        ]
    }

    void "getExactlyOneWorkflow, when a non deprecated workflow of the name exist, then return that workflow"() {
        given:
        createWorkflow()
        Workflow workflow = createWorkflow([
                name: WORKFLOW_NAME,
        ])

        when:
        Workflow returnedWorkflow = service.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        returnedWorkflow == workflow
    }

    void "getExactlyOneWorkflow, when no workflow of the name exists, then throw an exception"() {
        createWorkflow()

        when:
        service.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        thrown(AssertionError)
    }

    void "getExactlyOneWorkflow, when the only workflow with this name is deprecated, then throw an exception"() {
        createWorkflow([
                name          : WORKFLOW_NAME,
                deprecatedDate: LocalDate.now(),
        ])

        when:
        service.getExactlyOneWorkflow(WORKFLOW_NAME)

        then:
        thrown(AssertionError)
    }
}
