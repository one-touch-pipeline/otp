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
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog

class LogServiceSpec extends Specification implements ServiceUnitTest<LogService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        [
                WorkflowMessageLog,
        ]
    }

    void "addSimpleLogEntry, when adding two messages, then the messages are connected to the processing step and return in the creation order"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String message1 = "message ${nextId}"
        String message2 = "message ${nextId}"

        when:
        service.addSimpleLogEntry(workflowStep, message1)
        service.addSimpleLogEntry(workflowStep, message2)

        then:
        workflowStep.logs.size() == 2
        (workflowStep.logs[0] as WorkflowMessageLog).message == message1
        (workflowStep.logs[1] as WorkflowMessageLog).message == message2
    }
}
