/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.restartHandler.logging

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.restartHandler.LogWithIdentifier
import de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowCommandLog

class ExecutedCommandLogServiceSpec extends Specification implements ServiceUnitTest<ExecutedCommandLogService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowCommandLog,
        ]
    }

    void "getSourceType returns COMMAND_LOG"() {
        expect:
        service.sourceType == WorkflowJobErrorDefinition.SourceType.COMMAND_LOG
    }

    void "createLogsWithIdentifier returns empty list when no WorkflowCommandLog exists for the step"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowError: createWorkflowError(),
                state        : WorkflowStep.State.FAILED,
        ])

        when:
        Collection<LogWithIdentifier> result = service.createLogsWithIdentifier(workflowStep)

        then:
        result == []
    }

    void "createLogsWithIdentifier returns stdout and stderr entries for the command log of the step"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowError: createWorkflowError(),
                state        : WorkflowStep.State.FAILED,
        ])
        WorkflowCommandLog commandLog = createWorkflowCommandLog([
                workflowStep: workflowStep,
                stdout      : "some stdout output",
                stderr      : "some stderr output",
        ])

        when:
        Collection<LogWithIdentifier> result = service.createLogsWithIdentifier(workflowStep)

        then:
        result.size() == 2
        result[0].identifier == "command-stdout"
        result[0].log == commandLog.stdout
        result[1].identifier == "command-stderr"
        result[1].log == commandLog.stderr
    }

    void "createLogsWithIdentifier returns only the last command log when multiple exist"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowError: createWorkflowError(),
                state        : WorkflowStep.State.FAILED,
        ])
        createWorkflowCommandLog([
                workflowStep: workflowStep,
                stdout      : "first stdout",
                stderr      : "first stderr",
        ])
        WorkflowCommandLog lastCommandLog = createWorkflowCommandLog([
                workflowStep: workflowStep,
                stdout      : "last stdout",
                stderr      : "last stderr",
        ])

        when:
        Collection<LogWithIdentifier> result = service.createLogsWithIdentifier(workflowStep)

        then:
        result.size() == 2
        result[0].log == lastCommandLog.stdout
        result[1].log == lastCommandLog.stderr
    }
}
