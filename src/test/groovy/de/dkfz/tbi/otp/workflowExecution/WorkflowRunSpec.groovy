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

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

class WorkflowRunSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
        ]
    }

    void "test getOriginalRestartedFrom and getRestartCount when not restarted"() {
        given:
        WorkflowRun run = createWorkflowRun()

        expect:
        run.originalRestartedFrom == null
        run.restartCount == 0
    }

    void "test getOriginalRestartedFrom and getRestartCount when restarted once"() {
        given:
        WorkflowRun run = createWorkflowRun()
        run.restartedFrom = createWorkflowRun()

        expect:
        run.originalRestartedFrom == run.restartedFrom
        run.restartCount == 1
    }

    void "test getOriginalRestartedFrom and getRestartCount when restarted three times"() {
        given:
        WorkflowRun run = createWorkflowRun()
        run.restartedFrom = createWorkflowRun()
        run.restartedFrom.restartedFrom = createWorkflowRun()
        run.restartedFrom.restartedFrom.restartedFrom = createWorkflowRun()

        expect:
        run.originalRestartedFrom == run.restartedFrom.restartedFrom.restartedFrom
        run.restartCount == 3
    }
}
