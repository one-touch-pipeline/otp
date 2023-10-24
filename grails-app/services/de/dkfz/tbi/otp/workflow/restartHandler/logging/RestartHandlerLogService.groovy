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

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.workflow.restartHandler.LogWithIdentifier
import de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.FileSystem
import java.nio.file.Path

trait RestartHandlerLogService {

    LogService logService
    FileSystemService fileSystemService
    FileService fileService

    final long factor1024 = 1024
    final long maxFileSizeInMiB = 10
    final long maxFileSize = maxFileSizeInMiB * factor1024 * factor1024

    abstract WorkflowJobErrorDefinition.SourceType getSourceType()

    abstract Collection<LogWithIdentifier> createLogsWithIdentifier(WorkflowStep workflowStep)

    LogWithIdentifier createLogWithIdentifier(String filePath, String identifier, WorkflowStep workflowStep) {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        Path file = fileSystem.getPath(filePath)

        if (!fileService.fileIsReadable(file)) {
            logService.addSimpleLogEntry(workflowStep, "The log file '${file}' does not exist.")
        } else if (fileService.fileSizeExceeded(file, maxFileSize)) {
            logService.addSimpleLogEntry(workflowStep, "The log file '${file}' is bigger than the ${maxFileSizeInMiB} MB threshold.")
        } else {
            return new LogWithIdentifier(
                    identifier: identifier,
                    log: file.text
            )
        }

        return null
    }
}
