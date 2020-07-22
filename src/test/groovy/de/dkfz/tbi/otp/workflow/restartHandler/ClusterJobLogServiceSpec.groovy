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
package de.dkfz.tbi.otp.workflow.restartHandler

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowMessageLog

import java.nio.file.FileSystems

class ClusterJobLogServiceSpec extends Specification implements ServiceUnitTest<ClusterJobLogService>, DataTest, WorkflowSystemDomainFactory {

    @Rule
    TemporaryFolder temporaryFolder

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowMessageLog,
        ]
    }

    void setupData() {
        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem(_ as Realm) >> FileSystems.default
        }
        service.logService = Mock(LogService)
        service.fileService = Mock(FileService)
    }

    void "test createLogsWithIdentifier for ClusterJobLogService"() {
        given:
        setupData()

        File file = CreateFileHelper.createFile(temporaryFolder.newFile())
        File file2 = CreateFileHelper.createFile(temporaryFolder.newFile())

        Set<ClusterJob> clusterJobs = [
                DomainFactory.createClusterJob([
                        jobLog: file.absolutePath
                ]),
                DomainFactory.createClusterJob([
                        jobLog: file2.absolutePath
                ]),
        ]

        WorkflowStep workflowStep = createWorkflowStep([
                workflowError: createWorkflowError(),
                state        : WorkflowStep.State.FAILED,
                clusterJobs  : clusterJobs,
        ])

        when:
        Collection<LogWithIdentifier> logWithIdentifiers = service.createLogsWithIdentifier(workflowStep)

        then:
        logWithIdentifiers.size() == 2
        logWithIdentifiers[0].identifier == file.absolutePath
        logWithIdentifiers[0].log == file.text
        logWithIdentifiers[1].identifier == file2.absolutePath
        logWithIdentifiers[1].log == file2.text
    }

    void "test createLogsWithIdentifier when file not exists"() {
        given:
        setupData()

        String nonExistingPath = TestCase.uniqueNonExistentPath

        Set<ClusterJob> clusterJobs = [
                DomainFactory.createClusterJob([
                        jobLog: nonExistingPath
                ])
        ]

        WorkflowStep workflowStep = createWorkflowStep([
                workflowError: createWorkflowError(),
                state        : WorkflowStep.State.FAILED,
                clusterJobs  : clusterJobs,
        ])

        when:
        Collection<LogWithIdentifier> logWithIdentifiers = service.createLogsWithIdentifier(workflowStep)

        then:
        logWithIdentifiers == []
        1 * service.logService.addSimpleLogEntry(workflowStep, "The log file '${nonExistingPath}' does not exist.")
    }

    void "test createLogsWithIdentifier when threshold exceeded"() {
        given:
        File file = CreateFileHelper.createFile(temporaryFolder.newFile())

        setupData()
        service.fileService = Spy(FileService) {
            1 * fileSizeExceeded(file, _) >> true
        }

        Set<ClusterJob> clusterJobs = [
                DomainFactory.createClusterJob([
                        jobLog: file.absolutePath
                ]),
        ]

        WorkflowStep workflowStep = createWorkflowStep([
                workflowError: createWorkflowError(),
                state        : WorkflowStep.State.FAILED,
                clusterJobs  : clusterJobs,
        ])

        when:
        Collection<LogWithIdentifier> logWithIdentifiers = service.createLogsWithIdentifier(workflowStep)

        then:
        logWithIdentifiers == []
        1 * service.logService.addSimpleLogEntry(workflowStep, "The log file '${file}' is bigger than the 10 MB threshold.")
    }
}
