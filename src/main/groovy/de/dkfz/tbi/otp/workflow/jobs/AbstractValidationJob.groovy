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

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Checks whether the expected files and directories were created
 */
abstract class AbstractValidationJob implements Job {

    @Autowired
    FileSystemService fileSystemService
    @Autowired
    WorkflowStateChangeService workflowStateChangeService

    @Override
    final void execute(WorkflowStep workflowStep) {
        List<String> errors = []
        errors.addAll(getExpectedFiles(workflowStep).collect {
            try {
                FileService.ensureFileIsReadableAndNotEmpty(it)
            } catch (Throwable t) {
                return "Expected file not found, ${t.message}"
            }
        })
        errors.addAll(getExpectedDirectories(workflowStep).collect {
            try {
                FileService.ensureDirIsReadable(it)
            } catch (Throwable t) {
                return "Expected directory not found, ${t.message}"
            }
        })

        try {
            doFurtherValidation(workflowStep)
        } catch (Throwable t) {
            errors.add("Further validation failed, ${t.message}")
        }

        errors = errors.findAll()
        if (errors) {
            throw new WorkflowException(errors.join(","))
        }
        saveResult(workflowStep)
        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }


    @Override
    final JobStage getJobStage() {
        return JobStage.VALIDATION
    }

    abstract protected List<Path> getExpectedFiles(WorkflowStep workflowStep)
    abstract protected List<Path> getExpectedDirectories(WorkflowStep workflowStep)
    @SuppressWarnings("UnusedMethodParameter")
    protected void doFurtherValidation(WorkflowStep workflowStep) { }
    abstract protected void saveResult(WorkflowStep workflowStep)

    protected FileSystem getFileSystem(WorkflowStep workflowStep) {
        return fileSystemService.getRemoteFileSystem(workflowStep.workflowRun.project.realm)
    }
}
