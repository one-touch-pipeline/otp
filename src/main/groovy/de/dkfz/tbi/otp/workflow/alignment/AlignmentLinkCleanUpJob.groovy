/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentLinkFileServiceFactoryService
import de.dkfz.tbi.otp.workflow.jobs.AbstractJob
import de.dkfz.tbi.otp.workflow.jobs.JobStage
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

@Component
@Slf4j
class AlignmentLinkCleanUpJob extends AbstractJob implements AlignmentWorkflowShared<AbstractBamFile> {

    @Autowired
    AlignmentLinkFileServiceFactoryService alignmentLinkFileServiceFactoryService

    @Autowired
    FileService fileService

    @Override
    void execute(WorkflowStep workflowStep) {
        AbstractBamFile bamFile = getBamFile(workflowStep)
        Path baseDir = alignmentLinkFileServiceFactoryService.getService(bamFile).getDirectoryPath(bamFile)
        logService.addSimpleLogEntry(workflowStep, "Cleaning up old alignment links in the view-by-pid structure ${baseDir}")
        if (Files.exists(baseDir)) {
            Files.list(baseDir).withCloseable { stream ->
                stream.each { Path file ->
                    // keep externally imported data, which is not managed by OTP
                    if (file.fileName.toString() != ExternallyProcessedBamFile.NON_OTP) {
                        log.info("Deleting old alignment link entry: ${file}")
                        fileService.deleteDirectoryRecursively(file)
                    }
                }
            }
        }
        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @Override
    JobStage getJobStage() {
        return JobStage.CLEANUP
    }
}
