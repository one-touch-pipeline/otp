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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.filestore.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Attach a uuid work folder to the {@link WorkflowRun}, if not already one is attached
 */
@Component
@Slf4j
class AttachUuidJob extends AbstractJob {

    @Autowired
    FilestoreService filestoreService

    @Override
    void execute(WorkflowStep workflowStep) {
        if (workflowStep.workflowRun.workFolder) {
            logService.addSimpleLogEntry(workflowStep, "Reuse existing attached workfolder: ${workflowStep.workflowRun.workFolder}")
        } else {
            BaseFolder baseFolder = filestoreService.findAnyWritableBaseFolder()
            WorkFolder workFolder = filestoreService.createWorkFolder(baseFolder)
            filestoreService.attachWorkFolder(workflowStep.workflowRun, workFolder)
            logService.addSimpleLogEntry(workflowStep, "Attach new workfolder: ${workflowStep.workflowRun.workFolder}")
        }

        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @Override
    final JobStage getJobStage() {
        return JobStage.ATTACH_UUID
    }
}
