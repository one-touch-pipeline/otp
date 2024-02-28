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
package de.dkfz.tbi.otp.workflow.jobs

import grails.testing.gorm.DataTest
import groovy.transform.TupleConstructor
import io.swagger.client.wes.model.RunId
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.domainFactory.administration.DocumentFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.utils.MapUtilService
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.wes.*

import java.nio.file.Path

class AbstractExecuteWesPipelineJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, DocumentFactory {

    private WorkflowStep workflowStep
    private AbstractExecuteWesPipelineJob job

    @TempDir
    Path tempDir

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowArtefact,
                WorkflowStep,
        ]
    }

    @SuppressWarnings("GetterMethodCouldBeProperty")
    @TupleConstructor
    private class TestAbstractExecuteWesPipelineJob extends AbstractExecuteWesPipelineJob {

        boolean shouldSend

        @Override
        WesWorkflowType getWorkflowType() {
            return WesWorkflowType.NEXTFLOW
        }

        @Override
        String getWorkflowTypeVersion() {
            return "21.04.0"
        }

        @Override
        String getWorkflowUrl(WorkflowRun workflowRun) {
            return "nf-seq-qc-${workflowRun.workflowVersion.workflowVersion}/main.nf"
        }

        @Override
        Map<Path, Map<String, String>> getRunSpecificParameters(WorkflowStep workflowStep, Path path) {
            return [
                    (tempDir.resolve("1")): ["k1": "v1"],
                    (tempDir.resolve("2")): ["k2": "v2"],
            ]
        }

        @Override
        boolean shouldWeskitJobSend(WorkflowStep workflowStep) {
            return shouldSend
        }
    }

    private void setupData() {
        workflowStep = createWorkflowStep()
        workflowStep.workflowRun.combinedConfig = '{"config":"combined"}'
        workflowStep.workflowRun.workDirectory = tempDir.resolve("workDir").toString()
    }

    void "execute, when processed files cannot be copied, then do not submit Weskit jobs and change state to success"() {
        given:
        setupData()
        job = new TestAbstractExecuteWesPipelineJob(false)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToSuccess(workflowStep)
            0 * _
        }
        job.fileSystemService = new TestFileSystemService()
        job.fileService = Mock(FileService) {
            1 * deleteDirectoryContent(_)
        }
        job.logService = Mock(LogService)

        when:
        job.execute(workflowStep)

        then:
        true
    }

    void "execute, when processed files can be copied, then submit Weskit jobs and change state to wait for system"() {
        given:
        setupData()
        job = new TestAbstractExecuteWesPipelineJob(true)
        job.configFragmentService = new ConfigFragmentService()
        job.mapUtilService = new MapUtilService()

        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToWaitingOnSystem(workflowStep)
            0 * _
        }
        job.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem() >> tempDir.getFileSystem()
            0 * _
        }
        job.weskitAccessService = Mock(WeskitAccessService) {
            2 * runWorkflow(_) >> new RunId().runId("RUN_ID")
            0 * _
        }
        job.workflowRunService = Mock(WorkflowRunService) {
            1 * markJobAsNotRestartableInSeparateTransaction(_)
            1 * markJobAsRestartable(_)
            0 * _
        }
        job.fileService = Mock(FileService) {
            1 * deleteDirectoryContent(_)
            2 * createDirectoryRecursivelyAndSetPermissionsViaBash(_)
            0 * _
        }
        job.logService = Mock(LogService) {
            1 * addSimpleLogEntry(workflowStep, "Clean up the output directory ${workflowStep.workflowRun.workDirectory}")
            1 * addSimpleLogEntry(workflowStep, "Create 2 weskit calls")
            2 * addSimpleLogEntry(workflowStep, "Workflow job with run id RUN_ID has been sent to Weskit")
            2 * addSimpleLogEntry(workflowStep) { it.startsWith("Call Weskit with ") }
            2 * addSimpleLogEntry(workflowStep) { it.startsWith("Work directory ") }
            0 * _
        }
        job.wesRunService = Mock(WesRunService) {
            2 * saveWorkflowRun(workflowStep, _, _)
            0 * _
        }

        when:
        job.execute(workflowStep)

        then:
        true
    }
}
