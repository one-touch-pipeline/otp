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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

class WorkflowStepServiceSpec extends Specification implements ServiceUnitTest<WorkflowStepService>, WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    void "test getPreviousRunningWorkflowStep, when not restart, return the previous"() {
        given:
        WorkflowStep expected = createWorkflowStep()
        WorkflowStep stepToCheck = createWorkflowStep(previous: expected, restartedFrom: null)

        when:
        WorkflowStep result = service.getPreviousRunningWorkflowStep(stepToCheck)

        then:
        expected == result
    }

    void "test getPreviousRunningWorkflowStep, when restart, return the previous of the restarted one"() {
        given:
        WorkflowStep expected = createWorkflowStep()
        WorkflowStep restarted = createWorkflowStep([
                previous: expected
        ])
        WorkflowStep failed = createWorkflowStep([
                previous: restarted
        ])
        WorkflowStep stepToCheck = createWorkflowStep([
                previous     : failed,
                restartedFrom: restarted,
        ])

        when:
        WorkflowStep result = service.getPreviousRunningWorkflowStep(stepToCheck)

        then:
        expected == result
        expected != result.previous
    }
}
