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
package de.dkfz.tbi.otp.workflowExecution.wes

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class WesRunServiceSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                WesRun,
        ]
    }

    void "monitoredRuns, when called, return all WesRun in monitor state checking"() {
        given:
        createWesRun([state: WesRun.MonitorState.FINISHED])
        WesRun wesRun1 = createWesRun([state: WesRun.MonitorState.CHECKING])
        WesRun wesRun2 = createWesRun([state: WesRun.MonitorState.CHECKING])
        createWesRun([state: WesRun.MonitorState.FINISHED])

        and: 'service'
        WesRunService service = new WesRunService()

        when:
        List<WesRun> result = service.monitoredRuns()

        then:
        TestCase.assertContainSame(result, [wesRun1, wesRun2])
    }

    void "allByWorkflowStep, when called, return all WesRun connected to the WorkflowStep"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        WesRun wesRun1 = createWesRun([state: WesRun.MonitorState.CHECKING, workflowStep: workflowStep])
        WesRun wesRun2 = createWesRun([state: WesRun.MonitorState.FINISHED, workflowStep: workflowStep])
        createWesRun([state: WesRun.MonitorState.CHECKING])
        createWesRun([state: WesRun.MonitorState.FINISHED])

        and: 'service'
        WesRunService service = new WesRunService()

        when:
        List<WesRun> result = service.allByWorkflowStep(workflowStep)

        then:
        TestCase.assertContainSame(result, [wesRun1, wesRun2])
    }
}
