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

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepService

import java.nio.file.FileSystem
import java.nio.file.Path

@Transactional
class ClusterJobLogService extends AbstractRestartHandlerLogService {

    LogService logService

    FileSystemService fileSystemService

    FileService fileService

    WorkflowStepService workflowStepService

    static private final long FACTOR_1024 = 1024

    static final long MAX_FILE_SIZE_IN_MIB = 10

    static final long MAX_FILE_SIZE = MAX_FILE_SIZE_IN_MIB * FACTOR_1024 * FACTOR_1024

    @Override
    WorkflowJobErrorDefinition.SourceType getSourceType() {
        return WorkflowJobErrorDefinition.SourceType.CLUSTER_JOB
    }

    @Override
    Collection<LogWithIdentifier> createLogsWithIdentifier(WorkflowStep workflowStep) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(workflowStep.workflowRun.project.realm)
        WorkflowStep prevRunningWorkflowStep = workflowStepService.getPreviousRunningWorkflowStep(workflowStep)

        return prevRunningWorkflowStep?.clusterJobs?.findResults { ClusterJob clusterJob ->
            Path file = fileSystem.getPath(clusterJob.jobLog)
            if (!FileService.isFileReadable(file)) {
                logService.addSimpleLogEntry(workflowStep, "The log file '${file}' does not exist.")
                return
            } else if (fileService.fileSizeExceeded(file.toFile(), MAX_FILE_SIZE)) {
                logService.addSimpleLogEntry(workflowStep, "The log file '${file}' is bigger than the ${MAX_FILE_SIZE_IN_MIB} MB threshold.")
                return
            }
            return new LogWithIdentifier([
                    identifier: clusterJob.jobLog,
                    log       : file.text,
            ])
        }
    }
}
