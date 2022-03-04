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
package de.dkfz.tbi.otp.workflowExecution

import grails.validation.Validateable

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.SqlUtil
import de.dkfz.tbi.otp.config.ConfigService

import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

abstract class AbstractWorkflowRunController implements CheckAndCall {
    ConfigService configService
    JobService jobService
    WorkflowService workflowService
    WorkflowStateChangeService workflowStateChangeService

    static allowedMethods = [
            setFailedFinal: "POST",
            restartStep   : "POST",
            restartRun    : "POST",
    ]

    def setFailedFinal(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.setFailed") {
            workflowStateChangeService.changeStateToFinalFailed(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect uri: cmd.redirect
    }

    def restartStep(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.restartSteps") {
            jobService.createRestartedJobAfterJobFailures(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect uri: cmd.redirect
    }

    def restartPreviousStep(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.restartSteps") {
            jobService.createRestartedPreviousJobAfterJobFailures(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect uri: cmd.redirect
    }

    def restartRun(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.restartRuns") {
            workflowService.createRestartedWorkflows(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect uri: cmd.redirect
    }

    protected Closure getCriteria(Workflow workflow, List<WorkflowRun.State> states, String name) {
        return {
            if (name) {
                ilike("displayName", "%${SqlUtil.replaceWildcardCharactersInLikeExpression(name)}%")
            }
            if (states) {
                'in'("state", states)
            }
            if (workflow) {
                eq("workflow", workflow)
            }
            ne("state", WorkflowRun.State.LEGACY)
        }
    }

    protected LocalDateTime convertDateToLocalDateTime(Date date) {
        return date.toInstant().atZone(configService.timeZoneId).toLocalDateTime()
    }

    protected String getFormattedDuration(LocalDateTime start, LocalDateTime end) {
        long millis = Duration.between(start, end) toMillis()
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))
    }
}

class RunUpdateCommand implements Validateable {
    List<Long> step = []
    String redirect

    static constraints = {
        redirect(nullable: false, validator: { String val ->
            val.startsWith("/")
        })
    }
}
