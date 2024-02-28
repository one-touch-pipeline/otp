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

import io.swagger.client.wes.model.State
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepService
import de.dkfz.tbi.otp.workflowExecution.wes.WesRun

/**
 * Base job to do validation after an WES pipeline {@link AbstractExecuteWesPipelineJob} has run.
 *
 * It provides the implementation of {@link #ensureExternalJobsRunThrough} for the {@link AbstractExecuteWesPipelineJob}
 */
abstract class AbstractWesValidationJob extends AbstractValidationJob {

    @Autowired
    WorkflowStepService workflowStepService

    @Override
    protected void ensureExternalJobsRunThrough(WorkflowStep workflowStep) {
        Set<WesRun> wesRuns = workflowStepService.getPreviousRunningWorkflowStep(workflowStep).wesRuns
        if (!wesRuns) {
            logService.addSimpleLogEntry(workflowStep, "No cluster job found to be validated.")
            return
        }

        List<String> errorMessages = []
        wesRuns.each { WesRun wesRun ->
            if (!(wesRun.wesRunLog.state == State.COMPLETE)) {
                errorMessages.add("State for WES job '${wesRun.wesIdentifier}' is '${wesRun.wesRunLog.state}' and not 'COMPLETE'.".toString())
            } else if (!(wesRun.wesRunLog.runLog.exitCode == 0)) {
                errorMessages.add("Exit code of WES job '${wesRun.wesRunLogId}': ${wesRun.wesRunLog.runLog.exitCode}.".toString())
            }
        }

        if (errorMessages.empty) {
            logService.addSimpleLogEntry(workflowStep, "All WES jobs have finished successfully.")
        } else {
            String message = "${errorMessages.size()} WES errors occured in the Workflowstep {workflowStep}:\n${errorMessages.join('\n')}."
            logService.addSimpleLogEntry(workflowStep, message)
            throw new ValidationJobFailedException(message)
        }
    }
}
