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

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.cluster.logs.JobStatusLoggingFileService

import java.nio.file.Path

/**
 * Base job that does validation after an external pipeline {@link AbstractExecutePipelineJob} ran.
 *
 * Class provides an interface for the typicall checks to do after running an external pipeline:
 * - did the pipeline finish (using the callback {@link #ensureExternalJobsRunThrough})
 * - have all expected directories been created (Directories are fetched via callback {@link #getExpectedDirectories})
 * - have all expected files been created (Files are fetched via callback {@link #getExpectedDirectories})
 * - allows to do any further checking using using the callback {@link #doFurtherValidation}
 *
 * Moreover, it allows to do updates of {@link #saveResult}.
 *
 * Currently there exist three types of pipelines, for each a subclass exists with an implementation of {@link #ensureExternalJobsRunThrough}:
 * - Otp pipeline: {@link AbstractOtpClusterValidationJob}
 * - Roddy pipeline: {@link AbstractRoddyClusterValidationJob}
 * - Wes pipeline: {@link AbstractWesValidationJob}
 *
 * Usually, this base job should not be used directly, but instead one of its subclasses which provide an implementation of
 * {@link #ensureExternalJobsRunThrough}.
 */
abstract class AbstractValidationJob extends AbstractJob {

    @Autowired
    JobStatusLoggingFileService jobStatusLoggingFileService

    @Override
    final void execute(WorkflowStep workflowStep) {
        ensureExternalJobsRunThrough(workflowStep)

        List<String> errors = []

        getExpectedFiles(workflowStep).each {
            try {
                FileService.ensureFileIsReadableAndNotEmpty(it)
            } catch (AssertionError e) {
                errors << "Expected file ${it} not found, ${e.message}"
            }
        }

        getExpectedDirectories(workflowStep).each {
            try {
                FileService.ensureDirIsReadable(it)
            } catch (AssertionError e) {
                errors << "Expected directory ${it} not found, ${e.message}"
            }
        }

        try {
            errors.addAll(doFurtherValidationAndReturnProblems(workflowStep))
        } catch (OtpRuntimeException e) {
            errors << "Further validation failed with exception, ${e.message}"
        }

        if (errors) {
            String message = "${errors.size()} errors occured:\n${errors.join("\n")}"
            logService.addSimpleLogEntry(workflowStep, message)
            throw new ValidationJobFailedException(message)
        }
        saveResult(workflowStep)
        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @Override
    final JobStage getJobStage() {
        return JobStage.VALIDATION
    }

    /**
     * callback to do pipeline system depending checks of finishing the jobs. It should be exist one implementation per pipeline type.
     */
    abstract protected void ensureExternalJobsRunThrough(WorkflowStep workflowStep)

    /**
     * returns the files, which should be check for existence and not be empty
     */
    abstract protected List<Path> getExpectedFiles(WorkflowStep workflowStep)

    /**
     * returns the directory, which should be check for existence
     */
    abstract protected List<Path> getExpectedDirectories(WorkflowStep workflowStep)

    /**
     * callback to do further checks. It should collect problems and return them as List of Strings
     */
    @SuppressWarnings("UnusedMethodParameter")
    protected List<String> doFurtherValidationAndReturnProblems(WorkflowStep workflowStep) {
    }

    /**
     * callback to do database updates, if needed.
     */
    abstract protected void saveResult(WorkflowStep workflowStep)
}
