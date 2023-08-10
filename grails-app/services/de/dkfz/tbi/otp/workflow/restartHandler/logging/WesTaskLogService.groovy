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

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.workflow.restartHandler.LogWithIdentifier
import de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepService
import de.dkfz.tbi.otp.workflowExecution.wes.WesLog
import de.dkfz.tbi.otp.workflowExecution.wes.WesRun

@Transactional
class WesTaskLogService implements RestartHandlerLogService {

    WorkflowStepService workflowStepService

    @Override
    WorkflowJobErrorDefinition.SourceType getSourceType() {
        return WorkflowJobErrorDefinition.SourceType.WES_TASK_LOG
    }

    @Override
    Collection<LogWithIdentifier> createLogsWithIdentifier(WorkflowStep workflowStep) {
        WorkflowStep prevRunningWorkflowStep = workflowStepService.getPreviousRunningWorkflowStep(workflowStep)
        Collection<LogWithIdentifier> logsWithIdentifier = []

        prevRunningWorkflowStep.wesRuns.<WesRun>each { WesRun run ->
            if (run.wesRunLog && run.wesRunLog.taskLogs) {
                run.wesRunLog.taskLogs.each { WesLog log ->
                    String identifierPrefix = "${run.wesIdentifier}-${log.name}"
                    LogWithIdentifier stdoutLogWithIdentifier = createLogWithIdentifier(log.stdout, "${identifierPrefix}-stdout", workflowStep)

                    if (stdoutLogWithIdentifier) {
                        logsWithIdentifier.add(stdoutLogWithIdentifier)
                    }

                    LogWithIdentifier stderrLogWithIdentifier = createLogWithIdentifier(log.stderr, "${identifierPrefix}-stderr", workflowStep)

                    if (stderrLogWithIdentifier) {
                        logsWithIdentifier.add(stderrLogWithIdentifier)
                    }
                }
            } else {
                logService.addSimpleLogEntry(workflowStep, "No log available for ${run.wesIdentifier}.")
            }
        }

        return logsWithIdentifier
    }
}
