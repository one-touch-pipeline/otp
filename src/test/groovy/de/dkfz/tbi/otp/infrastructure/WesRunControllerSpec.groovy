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
package de.dkfz.tbi.otp.infrastructure

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.entity.ContentType
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowRunService
import de.dkfz.tbi.otp.workflowExecution.wes.*

import javax.servlet.http.HttpServletResponse
import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException

import java.nio.file.Path
import java.nio.file.Paths

class WesRunControllerSpec extends Specification implements ControllerUnitTest<WesRunController>, DataTest, UserAndRoles, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Role,
                User,
                UserRole,
                WesRun,
                WesRunLog,
                WesLog,
        ]
    }

    void setupData() {
        createUserAndRoles()

        controller.workflowRunService = Mock(WorkflowRunService)
        controller.wesRunService = Mock(WesRunService)
    }

    void "show, when WesRun exists, renders show view with correct data"() {
        given:
        setupData()
        WesRunLog wesRunLog = createWesRunLog([runRequest: '{"test":"log","number":42}',])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])
        controller.params.id = wesRun.id.toString()

        when:
        def result = controller.show()

        then:
        result.wesRun == wesRun
        result.nav != null
        result.NA == WesRunController.NOT_AVAILABLE
        result.formattedRunRequest.contains('"test": "log"')
        result.formattedRunRequest.contains('"number": 42')
        result.formattedRunRequest.contains('\n')
    }

// Turn it on after the bug described in otp-2807 is resolved
//    void "showTask, when taskLogs not empty, renders showTask view with correct data"() {
//        given:
//        setupData()
//        WesLog taskLog = createWesLog()
//        WesRunLog wesRunLog = createWesRunLog([taskLogs: [taskLog]])
//        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])
//        controller.params.id = taskLog.id.toString()
//
//        when:
//        def result = controller.showTask()
//
//        then:
//        result.wesLog == taskLog
//        result.wesRun == wesRun
//        result.nav != null
//        result.NA == WesRunController.NOT_AVAILABLE
//    }

    void "showStdout, when called, returns stdout log data"() {
        given:
        setupData()

        WesLog runLog = createWesLog([stdout: "/path/to/stdout"])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        controller.wesRunService.getById(wesRun.id) >> wesRun
        controller.wesRunService.getStdoutFileContent(wesRun) >> "stdout content"

        WesRunReportCommand command = new WesRunReportCommand(id: wesRun.id)

        when:
        controller.showStdout(command, new NavigationCommand())

        then:
        view == '/wesRun/showLog'
        model.wesRun == wesRun
        model.filePath == "/path/to/stdout"
        model.content == "stdout content"
        model.title != null
        model.nav != null
    }

    void "showStderr, when called, returns stderr log data"() {
        given:
        setupData()

        WesLog runLog = createWesLog([stderr: "/path/to/stderr"])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        controller.wesRunService.getById(wesRun.id) >> wesRun
        controller.wesRunService.getStderrFileContent(wesRun) >> "stderr content"

        WesRunReportCommand command = new WesRunReportCommand(id: wesRun.id)

        when:
        controller.showStderr(command, new NavigationCommand())

        then:
        view == '/wesRun/showLog'
        model.wesRun == wesRun
        model.filePath == "/path/to/stderr"
        model.content == "stderr content"
        model.title != null
        model.nav != null
    }

    void "showWeskitLog, when called, returns WESkit log data"() {
        given:
        setupData()

        WesRun wesRun = createWesRun()
        Path logPath = Paths.get("/path/to/log.json")

        controller.wesRunService.getById(wesRun.id) >> wesRun
        controller.wesRunService.getWeskitLogPath(wesRun) >> logPath
        controller.wesRunService.getWeskitLogFileContent(wesRun) >> '{"test":"log"}'

        WesRunReportCommand command = new WesRunReportCommand(id: wesRun.id)

        when:
        controller.showWeskitLog(command, new NavigationCommand())

        then:
        view == '/wesRun/showLog'
        model.wesRun == wesRun
        model.filePath == logPath
        model.content == '{"test":"log"}'
        model.title != null
        model.nav != null
    }

    @Unroll
    void "showReport, #scenario"() {
        given:
        setupData()
        WesRun wesRun = wesRunExists ? createWesRun() : null
        Long wesRunId = wesRun?.id ?: nonExistentId
        WesRunReportCommand command = validCommand ? new WesRunReportCommand(id: wesRunId) : new WesRunReportCommand(id: null)

        and: 'mock service behavior'
        if (wesRunExists) {
            controller.wesRunService.getById(wesRunId) >> wesRun
            controller.wesRunService.hasReports(wesRun) >> hasReport
            if (reportFileException) {
                controller.wesRunService.getReportFileContent(wesRun) >> { throw reportFileException }
            } else if (reportContent) {
                controller.wesRunService.getReportFileContent(wesRun) >> reportContent
            }
        } else {
            controller.wesRunService.getById(wesRunId) >> null
        }

        when:
        controller.showReport(command)

        then:
        if (expectedContent != null) {
            assert controller.response.contentAsByteArray == expectedContent
            assert controller.response.contentType.startsWith(ContentType.TEXT_HTML.mimeType)
        }
        controller.response.status == expectedStatus

        and: 'validate command if needed'
        if (!validCommand) {
            assert !command.validate()
        }

        where:
        scenario                                       | wesRunExists | validCommand | hasReport | reportContent                                 | reportFileException                         | expectedStatus                   | expectedContent                               | nonExistentId
        "WesRun exists and report file is available"   | true         | true         | true      | "<html><body>Test Report</body></html>".bytes | null                                        | HttpServletResponse.SC_OK        | "<html><body>Test Report</body></html>".bytes | null
        "WesRun exists but step is obsolete"           | true         | true         | false     | null                                          | null                                        | HttpServletResponse.SC_NOT_FOUND | null                                          | null
        "WesRun does not exist"                        | false        | true         | false     | null                                          | null                                        | HttpServletResponse.SC_NOT_FOUND | null                                          | 999L
        "report file does not exist"                   | true         | true         | true      | null                                          | new NoSuchFileException("Report not found") | HttpServletResponse.SC_NOT_FOUND | null                                          | null
        "access to report file is denied"              | true         | true         | true      | null                                          | new AccessDeniedException("Access denied")  | HttpServletResponse.SC_FORBIDDEN | null                                          | null
        "command with null ID is treated as not found" | false        | false        | false     | null                                          | null                                        | HttpServletResponse.SC_NOT_FOUND | null                                          | null
    }
}
