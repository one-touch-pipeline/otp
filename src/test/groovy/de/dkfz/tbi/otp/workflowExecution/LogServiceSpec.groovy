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

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowCommandLog
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog

class LogServiceSpec extends Specification implements ServiceUnitTest<LogService>, DataTest, WorkflowSystemDomainFactory, UserDomainFactory {

    private static final String SYSTEM_USER = "SYSTEM"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowMessageLog,
                WorkflowCommandLog,
        ]
    }

    void setup() {
        service.securityService = Mock(SecurityService)
        service.processingOptionService = Mock(ProcessingOptionService) {
            0 * _
        }
    }

    void "addSimpleLogEntry, when adding two messages, then the messages are connected to the processing step and return in the creation order"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String message1 = "message ${nextId}"
        GString message2 = """message ${nextId}"""

        when:
        service.addSimpleLogEntry(workflowStep, message1)
        service.addSimpleLogEntry(workflowStep, message2)

        then:
        2 * service.processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_SYSTEM_USER) >> SYSTEM_USER

        List<WorkflowLog> logs = WorkflowLog.all
        logs.size() == 2
        (logs[0] as WorkflowMessageLog).message == message1
        (logs[1] as WorkflowMessageLog).message == message2
        logs[0].workflowStep == workflowStep
        logs[1].workflowStep == workflowStep
    }

    void "addSimpleLogEntry, when the user is not set then log it with the username = SYSTEM"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String message = "message ${nextId}"

        when:
        service.addSimpleLogEntry(workflowStep, message)

        then:
        1 * service.processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_SYSTEM_USER) >> SYSTEM_USER

        List<WorkflowLog> logs = WorkflowLog.all
        logs.size() == 1
        logs[0].createdBy == SYSTEM_USER
        logs[0].workflowStep == workflowStep
    }

    void "addSimpleLogEntry, when the user is set then log it with the username"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String message = "message ${nextId}"
        User testUser = createUser()

        service.securityService = Mock(SecurityService) {
            getCurrentUser() >> testUser
        }

        when:
        service.addSimpleLogEntry(workflowStep, message)

        then:
        List<WorkflowLog> logs = WorkflowLog.all
        logs.size() == 1
        logs[0].createdBy == testUser.username
        logs[0].workflowStep == workflowStep
    }

    void "addSimpleLogEntryWithException, when adding a message with stacktrace, the stacktrace is converted and added together with the message"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String message = "message ${nextId}"
        String exceptionMessage = "exception ${nextId}"
        OtpRuntimeException otpRuntimeException = new OtpRuntimeException(exceptionMessage)

        when:
        service.addSimpleLogEntryWithException(workflowStep, message, otpRuntimeException)

        then:
        1 * service.processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_SYSTEM_USER) >> SYSTEM_USER

        List<WorkflowLog> logs = WorkflowLog.all
        logs.size() == 1
        WorkflowMessageLog log = logs[0] as WorkflowMessageLog
        log.workflowStep == workflowStep
        log.message.startsWith(message)
        log.message.contains(exceptionMessage)
    }

    void "addSimpleLogEntryWithException, when the user is not set then log it with the username = SYSTEM"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String message = "message ${nextId}"
        String exceptionMessage = "exception ${nextId}"
        OtpRuntimeException otpRuntimeException = new OtpRuntimeException(exceptionMessage)

        when:
        service.addSimpleLogEntryWithException(workflowStep, message, otpRuntimeException)

        then:
        1 * service.processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_SYSTEM_USER) >> SYSTEM_USER

        List<WorkflowLog> logs = WorkflowLog.all
        logs.size() == 1
        logs[0].createdBy == SYSTEM_USER
        logs[0].workflowStep == workflowStep
    }

    void "addSimpleLogEntryWithException, when the user is set then log it with the username"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String message = "message ${nextId}"
        String exceptionMessage = "exception ${nextId}"
        OtpRuntimeException otpRuntimeException = new OtpRuntimeException(exceptionMessage)
        User testUser = createUser()

        service.securityService = Mock(SecurityService) {
            getCurrentUser() >> testUser
        }

        when:
        service.addSimpleLogEntryWithException(workflowStep, message, otpRuntimeException)

        then:
        List<WorkflowLog> logs = WorkflowLog.all
        logs.size() == 1
        logs[0].createdBy == testUser.username
        logs[0].workflowStep == workflowStep
    }

    void "addCommandLogEntry, should create a new WorkflowCommandLog when ProcessOutput is set"() {
        given:
        User testUser = createUser()
        WorkflowStep workflowStep = createWorkflowStep()
        String commandParam = "command ${nextId}"
        ProcessOutput processOutput = new ProcessOutput([
                stdout  : "message  ${nextId}",
                stderr  : "message  ${nextId}",
                exitCode: 3,
        ])

        service.securityService = Mock(SecurityService) {
            getCurrentUser() >> testUser
        }

        when:
        service.addCommandLogEntry(workflowStep, commandParam, processOutput)

        then:
        List<WorkflowCommandLog> logs = WorkflowCommandLog.all
        logs.size() == 1
        logs[0].command == commandParam
        logs[0].exitCode == processOutput.exitCode as Long
        logs[0].stdout == processOutput.stdout
        logs[0].workflowStep == workflowStep
        logs[0].stderr == processOutput.stderr
        logs[0].createdBy == testUser.username
    }

    void "addCommandLogEntry, should create a new WorkflowCommandLog when ProcessOutput is empty"() {
        given:
        User testUser = createUser()
        WorkflowStep workflowStep = createWorkflowStep()
        String commandParam = "command ${nextId}"
        ProcessOutput processOutput = new ProcessOutput([
                stdout  : "",
                stderr  : null,
                exitCode: 0,
        ])

        service.securityService = Mock(SecurityService) {
            getCurrentUser() >> testUser
        }

        when:
        service.addCommandLogEntry(workflowStep, commandParam, processOutput)

        then:
        List<WorkflowCommandLog> logs = WorkflowCommandLog.all
        logs.size() == 1
        logs[0].command == commandParam
        logs[0].exitCode == processOutput.exitCode as Long
        logs[0].stdout == ""
        logs[0].workflowStep == workflowStep
        logs[0].stderr == ""
        logs[0].createdBy == testUser.username
    }
}
