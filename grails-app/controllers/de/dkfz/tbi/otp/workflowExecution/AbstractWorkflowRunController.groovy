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
package de.dkfz.tbi.otp.workflowExecution

import grails.validation.Validateable
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
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
            setFailedFinal     : "POST",
            toggleFailedWaiting: "POST",
            restartStep        : "POST",
            restartPreviousStep: "POST",
            restartRun         : "POST",
            killRun            : "POST",
    ]

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def setFailedFinal(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.setFailedFinal") {
            List<Long> stepIds = cmd.step.findAll()
            if (stepIds) {
                workflowStateChangeService.changeStateToFinalFailed(stepIds.collect { WorkflowStep.get(it) })
            } else {
                resolveRunsFromRunIds(cmd).each { workflowStateChangeService.changeStateToFinalFailed(it) }
            }
        }
        redirect uri: cmd.redirect
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def toggleFailedWaiting(RunUpdateCommand cmd) {
        WorkflowRun workflowRun = workflowService.getUniqueWorkflowFromWorkflowSteps(cmd.step)
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(
                cmd,
                (workflowRun.state == WorkflowRun.State.FAILED_WAITING) ? "workflowRun.list.removeFailedWaiting" : "workflowRun.list.setFailedWaiting"
        ) {
            assert cmd.step: 'No steps defined.'
            workflowStateChangeService.toggleFailedWaitingState(workflowRun)
        }
        redirect uri: cmd.redirect
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def restartStep(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.restartSteps") {
            assert cmd.step: 'No steps defined.'
            jobService.createRestartedJobAfterJobFailures(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect uri: cmd.redirect
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def restartPreviousStep(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.restartSteps") {
            assert cmd.step: 'No steps defined.'
            jobService.createRestartedPreviousJobAfterJobFailures(cmd.step.collect { WorkflowStep.get(it) })
        }
        redirect uri: cmd.redirect
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def restartRun(RunUpdateCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.restartRuns") {
            List<Long> stepIds = cmd.step.findAll()
            if (stepIds) {
                workflowService.createRestartedWorkflows(stepIds.collect { WorkflowStep.get(it) })
            } else {
                resolveRunsFromRunIds(cmd).each { workflowService.createRestartedWorkflow(it) }
            }
        }
        redirect uri: cmd.redirect
    }

    /**
     * Resolves the WorkflowRuns referred to by a RunUpdateCommand's run id(s). Used only as a
     * fallback when no step id was given at all (e.g. a run killed while still PENDING, which
     * never had a WorkflowStep) — whenever step ids are present, the original step-based path
     * is used instead so step-specific data (e.g. WorkflowStep#jobFinished) is not lost.
     */
    private List<WorkflowRun> resolveRunsFromRunIds(RunUpdateCommand cmd) {
        assert cmd.run: 'No runs defined.'
        List<WorkflowRun> runs = cmd.run.collect { WorkflowRun.get(it) }
        assert runs.every(): 'No runs defined.'
        return runs
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def killRun(RunKillCommand cmd) {
        checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, "workflowRun.list.killRuns") {
            assert cmd.run: "No WorkflowRun provided"
            WorkflowRun workflowRun = WorkflowRun.get(cmd.run)
            assert workflowRun: "WorkflowRun with id ${cmd.run} does not exist"
            workflowService.killWorkflowRun(workflowRun)
        }
        redirect uri: cmd.redirect
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
    List<Long> run = []
    String redirect

    static constraints = {
        redirect(nullable: false, validator: { String val ->
            val.startsWith("/")
        })
    }
}

class RunKillCommand implements Validateable {
    Long run
    String redirect

    static constraints = {
        run(nullable: false, validator: { Long val ->
            val != null && WorkflowRun.exists(val)
        })
        redirect(nullable: false, validator: { String val ->
            val.startsWith("/")
        })
    }
}
