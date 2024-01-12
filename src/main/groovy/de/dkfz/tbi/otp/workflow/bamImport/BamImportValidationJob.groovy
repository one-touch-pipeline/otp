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
package de.dkfz.tbi.otp.workflow.bamImport

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.workflow.jobs.AbstractOtpClusterValidationJob
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class BamImportValidationJob extends AbstractOtpClusterValidationJob implements BamImportShared {

    @Autowired
    ExternallyProcessedBamFileService externallyProcessedBamFileService

    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        ExternallyProcessedBamFile bamFile = getBamFile(workflowStep)

        return [
                externallyProcessedBamFileService.getBamFile(bamFile),
                externallyProcessedBamFileService.getBaiFile(bamFile),
        ] + externallyProcessedBamFileService.getFurtherFiles(bamFile)
    }

    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void doFurtherValidation(WorkflowStep workflowStep) throws ValidationJobFailedException {
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
    }
}
