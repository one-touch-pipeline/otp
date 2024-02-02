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
package de.dkfz.tbi.otp.job.restarting

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.plan.JobErrorDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService

@CompileDynamic
@Transactional
class RestartActionService {

    static final String JOB_NOT_AUTO_RESTARTABLE = 'Could not restart job, because job is not auto-restartable.'
    static final String WORKFLOW_RESTARTED = 'Workflow restarted.'
    static final String WORKFLOW_NOT_AUTO_RESTARTABLE = 'Could not restart workflow, because workflow is not auto-restartable.'
    static final String WORKFLOW_NOT_RESTARTED = 'Could not restart workflow, because workflow is not in failed state or already restarted.'

    @Autowired
    ApplicationContext context

    SchedulerService schedulerService

    CommentService commentService

    ProcessService processService

    void handleAction(JobErrorDefinition.Action action, Job job) {
        if (!action) {
            job.log.debug("Stopping, because action is null.")
            return
        }

        job.log.debug("Handling action {}.", action)

        switch (action) {
            case JobErrorDefinition.Action.STOP:
                break

            case JobErrorDefinition.Action.RESTART_JOB:
                restartJob(job)
                break

            case JobErrorDefinition.Action.RESTART_WF:
                restartWorkflow(job)
                break

            case JobErrorDefinition.Action.CHECK_FURTHER:
                throw new UnsupportedOperationException('Action is CHECK_FURTHER. That should have been handled earlier.')

            default:
                throw new UnsupportedOperationException("Unknown Action: ${action}")
        }
    }

    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    private void restartJob(Job job) {
        ProcessingStep step = job.processingStep

        if (job instanceof AutoRestartableJob) {
            schedulerService.restartProcessingStep(step)
            job.log.debug('Job restarted.')
        } else {
            throw new RuntimeException(JOB_NOT_AUTO_RESTARTABLE)
        }
    }

    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    private void restartWorkflow(Job job) {
        ProcessingStep step = job.processingStep
        StartJob startJob = getStartJob(step.process)

        if (startJob instanceof RestartableStartJob) {
            restart(startJob, step.process)
            logInCommentAndJobLog(job, WORKFLOW_RESTARTED)
        } else {
            throw new RuntimeException(WORKFLOW_NOT_AUTO_RESTARTABLE)
        }
    }

    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void restartWorkflowWithProcess(Process process) {
        StartJob startJob = getStartJob(process)
        ProcessingStepUpdate processingStepUpdate = processService.getLatestProcessingStepUpdate(processService.getLatestProcessingStep(process))

        if (processingStepUpdate.state == ExecutionState.FAILURE && !Process.findAllByRestarted(process)) {
            restart(startJob, process)
            logInComment(process, WORKFLOW_RESTARTED)
        } else {
            throw new RuntimeException(WORKFLOW_NOT_RESTARTED)
        }
    }

    StartJob getStartJob(Process process) {
        JobExecutionPlan plan = process.jobExecutionPlan
        StartJob startJob = context.getBean(plan.startJob.bean)
        return startJob
    }

    void restart(StartJob startJob, Process process) {
        Process process1 = ((RestartableStartJob) startJob).restart(process)
        process1.restarted = process
        assert process1.save(flush: true)
    }

    void logInCommentAndJobLog(Job job, String message) {
        job.log.debug(message)
        ProcessingStep step = job.processingStep
        Process process = step.process
        commentService.createOrUpdateComment(process, commentMessage(process, message), 'auto-restart handler')
    }

    void logInComment(Process process, String message) {
        commentService.saveComment(process, commentMessage(process, message))
    }

    String commentMessage(Process process, String message) {
        Comment comment = process.comment ?: new Comment()
        if (comment.comment) {
            return "${comment.comment}\n\n${message}"
        }
        return message
    }
}
