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
package de.dkfz.tbi.otp.workflow.restartHandler

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Transactional
class AutoRestartHandlerService {

    ErrorNotificationService errorNotificationService

    LogService logService

    AutoRestartActionService autoRestartActionService

    WorkflowJobErrorDefinitionService workflowJobErrorDefinitionService

    void handleRestarts(WorkflowStep workflowStep) {
        logService.addSimpleLogEntry(workflowStep, "AutoRestartHandler starting")
        List<JobErrorDefinitionWithLogWithIdentifier> matches = workflowJobErrorDefinitionService.findMatchingJobErrorDefinition(workflowStep)
        if (!matches) {
            logService.addSimpleLogEntry(workflowStep, "found no matching ErrorDefinition over all logs")
            errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, "No matching ErrorDefinitions found", matches)
            return
        }
        logService.addSimpleLogEntry(workflowStep, "found ${matches.size()} matching ErrorDefinition over all logs")

        List<WorkflowJobErrorDefinition.Action> actions = matches*.errorDefinition*.action.unique()
        if (actions.size() > 1) {
            logService.addSimpleLogEntry(workflowStep, "found following actions: ${actions.join(', ')}")
            errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP,
                    "STOP, since multiple actions found: ${actions.join(', ')}", matches)
            return
        }
        WorkflowJobErrorDefinition.Action action = actions[0]
        logService.addSimpleLogEntry(workflowStep, "found single action: ${action}")

        String beanToRestart
        if (action == WorkflowJobErrorDefinition.Action.RESTART_JOB) {
            List<String> beansToRestart = matches*.errorDefinition*.beanToRestart.unique()
            if (beansToRestart.size() > 1) {
                logService.addSimpleLogEntry(workflowStep, "found following beans for job restart: ${beansToRestart.join(', ')}")
                errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP,
                        "STOP, since multiple beans for restart job found: ${beansToRestart.join(', ')}", matches)
                return
            }
            beanToRestart = beansToRestart[0]
            logService.addSimpleLogEntry(workflowStep, "found single bean for job restart: ${beanToRestart}")
        } else {
            beanToRestart = null
        }

        if (action != WorkflowJobErrorDefinition.Action.STOP) {
            int restartCount = workflowStep.restartCount + workflowStep.workflowRun.restartCount
            int allowRestartCount = matches*.errorDefinition*.allowRestartingCount.max()
            logService.addSimpleLogEntry(workflowStep,
                    "Job/WorkflowRun was ${restartCount} times restarted, ErrorDefinition allows ${allowRestartCount} times")
            if (allowRestartCount <= restartCount) {
                errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP,
                        "STOP, since allowed restart count of ${allowRestartCount} was reached with ${restartCount}", matches)
                return
            }
        }
        autoRestartActionService.handleActionAndSendMail(workflowStep, matches, action, beanToRestart)
    }
}
