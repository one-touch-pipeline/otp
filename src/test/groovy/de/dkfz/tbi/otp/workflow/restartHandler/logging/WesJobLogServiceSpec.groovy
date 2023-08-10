/*
 * Copyright 2011-2023 The OTP authors
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
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.workflow.restartHandler.LogWithIdentifier
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.nio.file.FileSystems
import java.nio.file.Path

class WesJobLogServiceSpec extends Specification implements ServiceUnitTest<WesJobLogService>, DataTest, WorkflowSystemDomainFactory {

    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    void setup() {
        service.workflowStepService = new WorkflowStepService()
        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_ as Realm) >> FileSystems.default
        }
        service.logService = Mock(LogService)
        service.configService = Mock(ConfigService)
        service.fileService = Spy(FileService)
        service.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
    }

    void "test createLogsWithIdentifier should return logs with identifier when wesIdentifier is defined"() {
        given:
        Path stdout1 = CreateFileHelper.createFile(tempDir.resolve("stdout1.txt"))
        Path stdout2 = CreateFileHelper.createFile(tempDir.resolve("stdout2.txt"))
        Path stderr1 = CreateFileHelper.createFile(tempDir.resolve("stderr1.txt"))
        Path stderr2 = CreateFileHelper.createFile(tempDir.resolve("stderr2.txt"))

        WesLog wesLog1 = createWesLog(stdout: stdout1, stderr: stderr1)
        WesLog wesLog2 = createWesLog(stdout: stdout2, stderr: stderr2)

        WesRunLog wesRunLog1 = createWesRunLog(runLog: wesLog1)
        WesRunLog wesRunLog2 = createWesRunLog(runLog: wesLog2)

        WesRun wesRun1 = createWesRun(wesRunLog: wesRunLog1)
        WesRun wesRun2 = createWesRun(wesRunLog: wesRunLog2)

        WorkflowStep prevWorkflowStep = createWorkflowStep(wesRuns: [ wesRun1, wesRun2 ])
        WorkflowStep workflowStep = createWorkflowStep(previous: prevWorkflowStep)

        when:
        Collection<LogWithIdentifier> result = service.createLogsWithIdentifier(workflowStep)

        then:
        result.size() == 4
        result[0].identifier == "${wesRun1.wesIdentifier}-${wesLog1.name}-stdout"
        result[0].log == stdout1.text

        result[1].identifier == "${wesRun1.wesIdentifier}-${wesLog1.name}-stderr"
        result[1].log == stderr1.text

        result[2].identifier == "${wesRun2.wesIdentifier}-${wesLog2.name}-stdout"
        result[2].log == stdout2.text

        result[3].identifier == "${wesRun2.wesIdentifier}-${wesLog2.name}-stderr"
        result[3].log == stderr2.text
    }

    void "test createLogsWithIdentifier should add simple log entry, when stdout or stderr file not exist"() {
        given:
        String stdout = TestCase.uniqueNonExistentPath
        String stderr = TestCase.uniqueNonExistentPath

        WesLog wesLog = createWesLog(stdout: stdout, stderr: stderr)

        WesRunLog wesRunLog = createWesRunLog(runLog: wesLog)

        WesRun wesRun = createWesRun(wesRunLog: wesRunLog)

        WorkflowStep prevWorkflowStep = createWorkflowStep(wesRuns: [ wesRun ])
        WorkflowStep workflowStep = createWorkflowStep(previous: prevWorkflowStep)

        when:
        Collection<LogWithIdentifier> result = service.createLogsWithIdentifier(workflowStep)

        then:
        result == []
        1 * service.logService.addSimpleLogEntry(workflowStep, "The log file '${stdout}' does not exist.")
        1 * service.logService.addSimpleLogEntry(workflowStep, "The log file '${stderr}' does not exist.")
    }

    void "test createLogsWithIdentifier should add simple log entry, when file size threshold exceeded"() {
        given:
        Path stdout = CreateFileHelper.createFile(tempDir.resolve("stdout.txt"))
        Path stderr = CreateFileHelper.createFile(tempDir.resolve("stderr.txt"))

        1 * service.fileService.fileSizeExceeded(stdout, _) >> true
        1 * service.fileService.fileSizeExceeded(stderr, _) >> true

        WesLog wesLog = createWesLog(stdout: stdout, stderr: stderr)

        WesRunLog wesRunLog = createWesRunLog(runLog: wesLog)

        WesRun wesRun = createWesRun(wesRunLog: wesRunLog)

        WorkflowStep prevWorkflowStep = createWorkflowStep(wesRuns: [ wesRun ])
        WorkflowStep workflowStep = createWorkflowStep(previous: prevWorkflowStep)

        when:
        Collection<LogWithIdentifier> result = service.createLogsWithIdentifier(workflowStep)

        then:
        result == []
        1 * service.logService.addSimpleLogEntry(workflowStep, "The log file '${stdout}' is bigger than the 10 MB threshold.")
        1 * service.logService.addSimpleLogEntry(workflowStep, "The log file '${stderr}' is bigger than the 10 MB threshold.")
    }

    void "test createLogsWithIdentifier should add simple log entry, when wesRunLog or runLog not exist"() {
        given:
        WesRunLog wesRunLog = createWesRunLog(runLog: null)

        WesRun wesRun1 = createWesRun(wesRunLog: wesRunLog)
        WesRun wesRun2 = createWesRun(wesRunLog: null)

        WorkflowStep prevWorkflowStep = createWorkflowStep(wesRuns: [ wesRun1, wesRun2 ])
        WorkflowStep workflowStep = createWorkflowStep(previous: prevWorkflowStep)

        when:
        Collection<LogWithIdentifier> result = service.createLogsWithIdentifier(workflowStep)

        then:
        result == []
        1 * service.logService.addSimpleLogEntry(workflowStep, "No log available for ${wesRun1.wesIdentifier}.")
        1 * service.logService.addSimpleLogEntry(workflowStep, "No log available for ${wesRun2.wesIdentifier}.")
    }
}
