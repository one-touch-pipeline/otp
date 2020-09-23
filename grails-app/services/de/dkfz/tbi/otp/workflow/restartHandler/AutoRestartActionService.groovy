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
package de.dkfz.tbi.otp.workflow.restartHandler

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.workflowExecution.*

/**
 * Service to do the {@link WorkflowJobErrorDefinition.Action}  defined in {@link AutoRestartHandlerService}.
 *
 * Should only used by {@link AutoRestartHandlerService}.
 */
@Transactional
class AutoRestartActionService {

    ErrorNotificationService errorNotificationService

    JobService jobService

    LogService logService

    WorkflowService workflowService

    void handleActionAndSendMail(WorkflowStep workflowStep, List<JobErrorDefinitionWithLogWithIdentifier> matches, WorkflowJobErrorDefinition.Action action,
                                 String beanToRestart) {
        switch (action) {
            case WorkflowJobErrorDefinition.Action.RESTART_WORKFLOW:
                workflowService.createRestartedWorkflow(workflowStep, true)
                logService.addSimpleLogEntry(workflowStep, "Create restarted workflow")
                break
            case WorkflowJobErrorDefinition.Action.RESTART_JOB:
                jobService.createRestartedJobAfterJobFailure(jobService.searchForJobToRestart(workflowStep, beanToRestart))
                logService.addSimpleLogEntry(workflowStep, "Create restarted job")
                break
            case WorkflowJobErrorDefinition.Action.STOP:
                logService.addSimpleLogEntry(workflowStep, "only sending email, no restarting action")
                break
            default:
                logService.addSimpleLogEntry(workflowStep, "unknown action: ${action}, change to STOP")
                errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP,
                        "STOP, since how to handle ${action} is unknown", matches)
                return
        }
        errorNotificationService.send(workflowStep, action, "combining matches rules say ${action}", matches)
    }
}
