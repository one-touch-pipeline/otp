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
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

class WorkflowServiceSpec extends Specification implements ServiceUnitTest<WorkflowService>, WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
        ]
    }

    void "check getUniqueWorkflowFromWorkflowSteps functionality"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun()
        List<WorkflowStep> workflowSteps = (1..3).collect { createWorkflowStep(workflowRun: workflowRun) }

        when:
        WorkflowRun result = service.getUniqueWorkflowFromWorkflowSteps(workflowSteps*.id)

        then:
        result == workflowRun
    }

    void "getUniqueWorkflowFromWorkflowSteps, when workflowSteps have different workflowRuns, then throw assertion"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowStep workflowStep2 = createWorkflowStep()
        List<WorkflowStep> workflowSteps = [workflowStep, workflowStep2]

        when:
        service.getUniqueWorkflowFromWorkflowSteps(workflowSteps*.id)

        then:
        thrown(AssertionError)
    }
}
