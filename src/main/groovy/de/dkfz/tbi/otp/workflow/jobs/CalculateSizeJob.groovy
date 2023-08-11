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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.text.NumberFormat

/**
 * Calculate the size of the uuid work folder
 */
@Component
@Slf4j
class CalculateSizeJob extends AbstractJob {

    @Autowired
    FileService fileService

    @Override
    void execute(WorkflowStep workflowStep) {
        if (workflowStep.workflowRun.workFolder) {
            Path workDirectory = getWorkDirectory(workflowStep)
            logService.addSimpleLogEntry(workflowStep, "Start calculation size of uuid folder: ${workDirectory}")

            long size = fileService.calculateSizeRecursive(workDirectory)
            WorkFolder workFolder = workflowStep.workflowRun.workFolder
            workFolder.size = size
            workFolder.save(flush: true)

            NumberFormat numberFormat = NumberFormat.integerInstance
            logService.addSimpleLogEntry(workflowStep, "Calculated size: ${numberFormat.format(size)}")
        } else {
            logService.addSimpleLogEntry(workflowStep, "Skip size calculation, since no uuid work folder")
        }

        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @Override
    final JobStage getJobStage() {
        return JobStage.CALCULATE_SIZE
    }
}
