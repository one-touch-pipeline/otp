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
package de.dkfz.tbi.otp.workflow.jobs

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.cluster.logs.JobStatusLoggingFileService

import java.nio.file.FileSystems

class AbstractOtpClusterValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        [
                WorkflowStep,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    private AbstractOtpClusterValidationJob job

    private WorkflowStep workflowStep

    private void setupData() {
        job = Spy(AbstractOtpClusterValidationJob)
        job.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem(_) >> FileSystems.default
        }
        job.logService = Mock(LogService)
        job.jobStatusLoggingFileService = Mock(JobStatusLoggingFileService)

        workflowStep = createWorkflowStep()
    }

    private void setupDataWithClusterJob(ClusterJob.CheckStatus checkStatus = ClusterJob.CheckStatus.FINISHED) {
        setupData()
        createClusterJob([
                workflowStep: workflowStep,
                checkStatus : checkStatus,
        ])
    }

    void "ensureExternalJobsRunThrough, when workflowStep has no ClusterJob, then run successfully"() {
        given:
        setupData()

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        noExceptionThrown()
    }

    @Unroll
    void "ensureExternalJobsRunThrough, when ClusterJob is in checkStatus #checkStatus, then fail"() {
        given:
        setupDataWithClusterJob(checkStatus)

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        ValidationJobFailedException e = thrown()
        e.message.contains(checkStatus.toString())

        where:
        checkStatus << [
                ClusterJob.CheckStatus.CREATED,
                ClusterJob.CheckStatus.CHECKING,
        ]
    }

    void "ensureExternalJobsRunThrough, when the job status log file does not exist, then fail"() {
        given:
        File file = new File(temporaryFolder.newFile(), 'NonExistingFile.txt')
        setupDataWithClusterJob()
        1 * job.jobStatusLoggingFileService.constructLogFileLocation(_, _, _) >> file.path

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        ValidationJobFailedException e = thrown()
        e.message.contains('does not exist')
    }

    void "ensureExternalJobsRunThrough, when the job status log file is not readable, then fail"() {
        given:
        File file = temporaryFolder.newFile()
        file.readable = false

        setupDataWithClusterJob()
        1 * job.jobStatusLoggingFileService.constructLogFileLocation(_, _, _) >> file.path

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        ValidationJobFailedException e = thrown()
        e.message.contains('can not be read')
    }

    void "ensureExternalJobsRunThrough, when the job status do not contain the expected message, then fail"() {
        given:
        File file = temporaryFolder.newFile()

        setupDataWithClusterJob()
        1 * job.jobStatusLoggingFileService.constructLogFileLocation(_, _, _) >> file.path
        1 * job.jobStatusLoggingFileService.constructMessage(_, _, _) >> 'wrong message'

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        ValidationJobFailedException e = thrown()
        e.message.contains('Did not find')
    }

    void "ensureExternalJobsRunThrough, when the job status log file contain the expected message, then success"() {
        given:
        String message = "message ${nextId}"
        File file = temporaryFolder.newFile()
        file.text = message

        setupDataWithClusterJob()
        1 * job.jobStatusLoggingFileService.constructLogFileLocation(_, _, _) >> file.path
        1 * job.jobStatusLoggingFileService.constructMessage(_, _, _) >> message

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        noExceptionThrown()
    }
}
