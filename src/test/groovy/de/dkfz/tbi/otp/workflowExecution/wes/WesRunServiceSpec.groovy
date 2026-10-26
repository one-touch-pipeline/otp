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
package de.dkfz.tbi.otp.workflowExecution.wes

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import io.swagger.client.wes.model.State
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.job.processing.FileSystemService

import java.nio.file.*

class WesRunServiceSpec extends Specification implements ServiceUnitTest<WesRunService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                WesRun,
                WesRunLog,
                WesLog,
                WorkflowStep,
                WorkflowRun,
                WorkFolder,
                BaseFolder,
        ]
    }

    @TempDir
    Path tempDir

    @Shared
    Path reportFilePath

    @Shared
    Path otherFilePath

    void setup() {
        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }
    }

    void "monitoredRuns, when called, return all WesRun in monitor state checking"() {
        given:
        createWesRun([state: WesRun.MonitorState.FINISHED])
        WesRun wesRun1 = createWesRun([state: WesRun.MonitorState.CHECKING])
        WesRun wesRun2 = createWesRun([state: WesRun.MonitorState.CHECKING])
        createWesRun([state: WesRun.MonitorState.FINISHED])

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

        when:
        List<WesRun> result = service.allByWorkflowStep(workflowStep)

        then:
        TestCase.assertContainSame(result, [wesRun1, wesRun2])
    }

    void "getReportPath, when report file exists, returns correct path"() {
        given:
        WesRun wesRun = createWesRun([workflowStep: createWorkflowStep([workflowRun: createWorkflowRun([workFolder: createWorkFolder()])]), subPath: "subdir"])
        Path mockWorkFolder = Paths.get("/work/folder")
        Path mockSubPath = mockWorkFolder.resolve("subdir")
        reportFilePath = mockSubPath.resolve("report-20252034.html")

        and: 'mocked services'
        service.filestoreService = Mock(FilestoreService) {
            getWorkFolderPath(_) >> mockWorkFolder
        }
        service.fileService = Mock(FileService) {
            findFileInPath(mockSubPath, WesRunService.MATCHER_REPORT_FILE) >> reportFilePath
        }

        when:
        Path result = service.getReportPath(wesRun)

        then:
        result == reportFilePath
    }

    void "getReportFileContent, when valid path and readable file, returns expected content"() {
        given:
        WesRun wesRun = createWesRun([workflowStep: createWorkflowStep([workflowRun: createWorkflowRun([workFolder: createWorkFolder()])]), subPath: "subdir"])
        String expectedContent = "<html>Report content</html>"

        and: 'create mock report file with expected content'
        Path mockWorkFolder = tempDir.resolve("workFolder")
        Path mockSubPath = mockWorkFolder.resolve("subdir")
        reportFilePath = mockSubPath.resolve("report-20252034.html")
        // Create the report file
        CreateFileHelper.createFile(reportFilePath, expectedContent)
        // Creates other file
        otherFilePath = mockSubPath.resolve("other.html")
        CreateFileHelper.createFile(otherFilePath)

        and: 'mock services for successful file operations'
        service.filestoreService = Mock(FilestoreService) {
            1 * getWorkFolderPath(_) >> mockWorkFolder
        }
        service.fileService = Mock(FileService) {
            findFileInPath(mockSubPath, WesRunService.MATCHER_REPORT_FILE) >> reportFilePath
            fileIsReadable(reportFilePath) >> true
        }

        when:
        byte[] result = service.getReportFileContent(wesRun)

        then:
        result == expectedContent.bytes
    }

    @Unroll
    void "getReportFileContent, when #scenario, throws #exceptionClass.simpleName with correct message"() {
        given:
        WesRun wesRun = createWesRun([
            id         : 123L,
            workflowStep: createWorkflowStep([
                workflowRun: createWorkflowRun([workFolder: createWorkFolder()])]), subPath: "test-subdir"])

        and: 'mock the file structure'
        Path mockWorkFolder = tempDir.resolve("workFolder")
        Path mockSubPath = mockWorkFolder.resolve("subdir")

        reportFilePath = mockSubPath.resolve("report-20252034.html")
        CreateFileHelper.createFile(reportFilePath)

        otherFilePath = mockSubPath.resolve("other.html")

        and: 'mock services for file operations'
        service.filestoreService = Mock(FilestoreService) {
            1 * getWorkFolderPath(_) >> mockWorkFolder
        }
        service.fileService = Mock(FileService) {
            1 * findFileInPath(_ as Path, WesRunService.MATCHER_REPORT_FILE) >> reportFileClosure()
            (0..1) * fileIsReadable(reportFilePath as Path) >> readable
        }

        when:
        service.getReportFileContent(wesRun)

        then:
        FileSystemException ex = thrown()
        ex.class == exceptionClass
        ex.message.contains("Report file for 123")
        ex.message.contains(exceptionTextClosure())

        where:
        scenario                       || exceptionClass        | readable | reportFileClosure  | exceptionTextClosure
        "report file not found"        || NoSuchFileException   | true     | { null }           | { "not found: test-subdir" }
        "wrong file found"             || NoSuchFileException   | true     | { otherFilePath }  | { "not found: ${otherFilePath}" }
        "file exists but not readable" || AccessDeniedException | false    | { reportFilePath } | { "found but not readable: ${reportFilePath}" }
    }

    void "getNextflowLogPath, when called, returns Nextflow log path"() {
        given:
        WesRun wesRun = createWesRun([
                workflowStep: createWorkflowStep([
                        workflowRun: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
                subPath: "subdir",
        ])
        Path workFolder = Paths.get("/work/folder")

        service.filestoreService = Mock(FilestoreService) {
            getWorkFolderPath(_) >> workFolder
        }

        expect:
        service.getNextflowLogPath(wesRun) == workFolder.resolve("subdir/.nextflow.log")
    }

    void "getNextflowLogFileContent, when file is readable, returns expected content"() {
        given:
        WesRun wesRun = createWesRun([
                workflowStep: createWorkflowStep([
                        workflowRun: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
                subPath: "subdir",
        ])

        Path workFolder = tempDir.resolve("workFolder")
        Path nextflowLog = workFolder.resolve("subdir/.nextflow.log")
        String expectedContent = "Nextflow log content"
        CreateFileHelper.createFile(nextflowLog, expectedContent)

        service.filestoreService = Mock(FilestoreService) {
            getWorkFolderPath(_) >> workFolder
        }
        service.fileService = Mock(FileService) {
            fileIsReadable(nextflowLog) >> true
        }

        when:
        byte[] result = service.getNextflowLogFileContent(wesRun)

        then:
        result == expectedContent.bytes
    }

    @Unroll
    void "getNextflowLogFileContent, when #scenario, throws #exceptionClass.simpleName"() {
        given:
        WesRun wesRun = createWesRun([
                workflowStep: createWorkflowStep([
                        workflowRun: createWorkflowRun([
                                workFolder: createWorkFolder(),
                        ]),
                ]),
                subPath: "subdir",
        ])

        Path workFolder = tempDir.resolve("workFolder")
        Path nextflowLog = workFolder.resolve("subdir/.nextflow.log")

        if (fileExists) {
            CreateFileHelper.createFile(nextflowLog)
        }

        service.filestoreService = Mock(FilestoreService) {
            getWorkFolderPath(_) >> workFolder
        }
        service.fileService = Mock(FileService) {
            (0..1) * fileIsReadable(nextflowLog) >> readable
        }

        when:
        service.getNextflowLogFileContent(wesRun)

        then:
        thrown(exceptionClass)

        where:
        scenario               | fileExists | readable || exceptionClass
        "file does not exist"  | false      | false    || NoSuchFileException
        "file is not readable" | true       | false    || AccessDeniedException
    }

    void "hasNextflowLog, when work folder is null, returns false"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([workFolder: null])
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: workflowRun])
        WesRun wesRun = createWesRun([workflowStep: workflowStep])

        expect:
        !service.hasNextflowLog(wesRun)
    }

    void "getWeskitLogPath, returns log.json next to stdout"() {
        given:
        Path directory = tempDir.resolve("weskit")
        Path stdout = directory.resolve("stdout")

        WesLog runLog = createWesLog([stdout: stdout.toString()])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        expect:
        service.getWeskitLogPath(wesRun) == directory.resolve("log.json")
    }

    void "getWeskitLogPath, when stdout path has no parent, returns null"() {
        given:
        WesLog runLog = createWesLog([stdout: "stdout.log"])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        expect:
        service.getWeskitLogPath(wesRun) == null
    }

    void "getWeskitLogFileContent, when log.json is readable, returns its content"() {
        given:
        Path directory = tempDir.resolve("weskit")
        Path stdout = directory.resolve("stdout")
        Path logJson = directory.resolve("log.json")
        String expectedContent = '{"foo":"bar"}'

        CreateFileHelper.createFile(logJson, expectedContent)

        WesLog runLog = createWesLog([stdout: stdout.toString()])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        service.fileService = Mock(FileService) {
            fileIsReadable(logJson) >> true
        }

        expect:
        service.getWeskitLogFileContent(wesRun) == expectedContent
    }

    void "getWeskitLogFileContent, when stdout path has no parent, returns path not set message"() {
        given:
        WesLog runLog = createWesLog([stdout: "stdout.log"])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        service.messageSourceService = Mock(MessageSourceService) {
            createMessage("wesRunService.path_to_log_not_set") >> "Path to log not set"
        }

        expect:
        service.getWeskitLogFileContent(wesRun) == "Path to log not set"
    }

    void "getStdoutFileContent, when file is readable, returns its content"() {
        given:
        Path stdout = tempDir.resolve("stdout")
        String expectedContent = "stdout content"
        CreateFileHelper.createFile(stdout, expectedContent)

        WesLog runLog = createWesLog([stdout: stdout.toString()])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        service.fileService = Mock(FileService) {
            fileIsReadable(stdout) >> true
        }

        expect:
        service.getStdoutFileContent(wesRun) == expectedContent
    }

    void "getStderrFileContent, when file is readable, returns its content"() {
        given:
        Path stderr = tempDir.resolve("stderr")
        String expectedContent = "stderr content"
        CreateFileHelper.createFile(stderr, expectedContent)

        WesLog runLog = createWesLog([stderr: stderr.toString()])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        service.fileService = Mock(FileService) {
            fileIsReadable(stderr) >> true
        }

        expect:
        service.getStderrFileContent(wesRun) == expectedContent
    }

    void "getStdoutFileContent, when path is not set, returns message"() {
        given:
        WesLog runLog = createWesLog([stdout: null])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        service.messageSourceService = Mock(MessageSourceService) {
            createMessage("wesRunService.path_to_log_not_set") >> "Path to log not set"
        }

        expect:
        service.getStdoutFileContent(wesRun) == "Path to log not set"
    }

    void "getStdoutFileContent, when file does not exist, returns message"() {
        given:
        Path stdout = tempDir.resolve("non-existing-stdout")

        WesLog runLog = createWesLog([stdout: stdout.toString()])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        service.fileService = Mock(FileService) {
            fileIsReadable(stdout) >> false
            fileExists(stdout) >> false
        }
        service.messageSourceService = Mock(MessageSourceService) {
            createMessage("wesRunService.non_existing_file") >> "File doesn't exist"
        }

        expect:
        service.getStdoutFileContent(wesRun) == "File doesn't exist"
    }

    void "getStdoutFileContent, when file is not readable, returns message"() {
        given:
        Path stdout = tempDir.resolve("stdout")

        WesLog runLog = createWesLog([stdout: stdout.toString()])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        service.fileService = Mock(FileService) {
            fileIsReadable(stdout) >> false
            fileExists(stdout) >> true
        }
        service.messageSourceService = Mock(MessageSourceService) {
            createMessage("wesRunService.unreadable_file") >> "File is not readable"
        }

        expect:
        service.getStdoutFileContent(wesRun) == "File is not readable"
    }

    @Unroll
    void "get log file content for #logType, when path is relative, returns invalid path message"() {
        given:
        WesLog runLog = createWesLog([
                stdout: "relative/stdout.log",
                stderr: "relative/stderr.log",
        ])
        WesRunLog wesRunLog = createWesRunLog([runLog: runLog])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        service.messageSourceService = Mock(MessageSourceService) {
            createMessage("baseFolder.path.not.absolute") >> "Path specified is not an absolute path"
        }
        service.fileService = Mock(FileService)

        when:
        String result = service."get${logType}FileContent"(wesRun)

        then:
        result == "Path specified is not an absolute path"
        0 * service.fileService._

        where:
        logType  | _
        "Stdout" | _
        "Stderr" | _
    }

    void "getById, when WesRun exists, returns correct WesRun"() {
        given:
        WesRun wesRun = createWesRun([id: 123L])

        when:
        WesRun result = service.getById(123L)

        then:
        result == wesRun
    }

    void "getById, when WesRun does not exist, returns null"() {
        when:
        WesRun result = service.getById(999L)

        then:
        result == null
    }

    @Unroll
    void "hasReports, when wesRunLog state is #state, returns #expected"() {
        given:
        WesRunLog wesRunLog = createWesRunLog([state: state])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog])

        when:
        boolean result = service.hasReports(wesRun)

        then:
        result == expected

        where:
        state                || expected
        State.COMPLETE       || true
        State.EXECUTOR_ERROR || true
        State.SYSTEM_ERROR   || true
        State.RUNNING        || false
        State.PAUSED         || false
        State.CANCELED       || false
        State.INITIALIZING   || false
        State.QUEUED         || false
        State.UNKNOWN        || false
    }

    void "hasReports, when wesRunLog is null, returns false"() {
        given:
        WesRun wesRun = createWesRun([wesRunLog: null])

        when:
        boolean result = service.hasReports(wesRun)

        then:
        result == false
    }

    void "hasReports, when workflowStep is obsolete, returns false"() {
        given:
        WesRunLog wesRunLog = createWesRunLog([state: State.COMPLETE])
        WorkflowStep workflowStep = createWorkflowStep([obsolete: true])
        WesRun wesRun = createWesRun([wesRunLog: wesRunLog, workflowStep: workflowStep])

        when:
        boolean result = service.hasReports(wesRun)

        then:
        result == false
    }

    void "getCumulatedWesRunsStatus, when empty list provided, returns empty string"() {
        when:
        String result = service.getCumulatedWesRunsStatus([])

        then:
        result == ""
    }

    @Unroll
    void "getCumulatedWesRunsStatus, with single WesRun in #monitorStatus/#wesRunState, returns #expected"() {
        given:
        List<WesRunStateDto> wesRunStates = [new WesRunStateDto(monitorStatus, wesRunState)]

        when:
        String result = service.getCumulatedWesRunsStatus(wesRunStates)

        then:
        result == expected

        where:
        monitorStatus                | wesRunState          || expected
        WesRun.MonitorState.CHECKING | State.RUNNING        || "CHECKING"
        WesRun.MonitorState.FINISHED | State.COMPLETE       || "FINISHED/COMPLETE"
        WesRun.MonitorState.FINISHED | State.EXECUTOR_ERROR || "FINISHED/EXECUTOR_ERROR"
        WesRun.MonitorState.FINISHED | null                 || "FINISHED/UNKNOWN"
    }

    void "getCumulatedWesRunsStatus, with multiple WesRuns, returns highest priority status"() {
        given:
        List<WesRunStateDto> wesRunStates = [
                new WesRunStateDto(WesRun.MonitorState.FINISHED, State.COMPLETE),       // Priority: 1 + 0 = 1
                new WesRunStateDto(WesRun.MonitorState.CHECKING, State.RUNNING),        // Priority: 3 + 0 = 3 (highest)
                new WesRunStateDto(WesRun.MonitorState.FINISHED, State.EXECUTOR_ERROR), // Priority: 1 + 1 = 2
        ]

        when:
        String result = service.getCumulatedWesRunsStatus(wesRunStates)

        then:
        result == "CHECKING"  // CHECKING has highest priority (3), and doesn't show state for checking
    }

    void "saveWorkflowRun, when called, creates WesRun with correct properties"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        String wesIdentifier = "wes-run-123"
        String subPath = "workflow/run/path"

        when:
        service.saveWorkflowRun(workflowStep, wesIdentifier, subPath)

        then:
        WesRun wesRun = WesRun.findByWesIdentifier(wesIdentifier)
        wesRun != null
        wesRun.workflowStep == workflowStep
        wesRun.wesIdentifier == wesIdentifier
        wesRun.subPath == subPath
        wesRun.state == WesRun.MonitorState.CHECKING
    }

    void "killWesRunsInWorkflowStep, when workflow step has multiple WES runs, cancel all runs and log the identifiers"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        WesRun wesRun1 = createWesRun([workflowStep: workflowStep, wesIdentifier: "wes-id-1"])
        WesRun wesRun2 = createWesRun([workflowStep: workflowStep, wesIdentifier: "wes-id-2"])
        String expectedLog = "Following WESkit runs have been cancelled: ${[wesRun1.wesIdentifier, wesRun2.wesIdentifier].join(',')}"

        service.weskitAccessService = Mock(WeskitAccessService)
        service.logService = Mock(LogService)

        when:
        service.killWesRunsInWorkflowStep(workflowStep)

        then:
        1 * service.weskitAccessService.cancelRun(wesRun1)
        1 * service.weskitAccessService.cancelRun(wesRun2)
        1 * service.logService.addSimpleLogEntry(workflowStep, expectedLog)
    }

    void "killWesRunsInWorkflowStep, when workflow step has a single WES run, cancel the run and log the identifier"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        WesRun wesRun = createWesRun([workflowStep: workflowStep, wesIdentifier: "wes-id-single"])

        service.weskitAccessService = Mock(WeskitAccessService)
        service.logService = Mock(LogService)

        when:
        service.killWesRunsInWorkflowStep(workflowStep)

        then:
        1 * service.weskitAccessService.cancelRun(wesRun)
        1 * service.logService.addSimpleLogEntry(workflowStep, "Following WESkit runs have been cancelled: wes-id-single")
    }

    void "killWesRunsInWorkflowStep, when workflow step has no WES runs, do not call cancelRun and throw AssertionError"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        service.weskitAccessService = Mock(WeskitAccessService)

        when:
        service.killWesRunsInWorkflowStep(workflowStep)

        then:
        0 * service.weskitAccessService.cancelRun(_)
        Throwable ex = thrown()
        ex.class == AssertionError
        ex.message.contains("doesn't contain any WESkit runs")
    }
}
