/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.fastqc

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.workflow.jobs.AbstractOtpClusterValidationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
@CompileStatic
class FastqcValidationJob extends AbstractOtpClusterValidationJob implements FastqcShared {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        return getFastqcProcessedFiles(workflowStep).collect { FastqcProcessedFile fastqcProcessedFile ->
            fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile)
        }
    }

    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
    }

    @Override
    protected List<String> doFurtherValidationAndReturnProblems(WorkflowStep workflowStep) {
        return []
    }
}
