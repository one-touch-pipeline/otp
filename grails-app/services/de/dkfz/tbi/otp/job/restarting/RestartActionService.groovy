package de.dkfz.tbi.otp.job.restarting

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import org.springframework.context.*

class RestartActionService {

    static String JOB_NOT_AUTO_RESTARTABLE = 'Could not restart job, because job is not auto-restartable.'
    static String WORKFLOW_RESTARTED = 'Workflow restarted.'
    static String WORKFLOW_NOT_AUTO_RESTARTABLE = 'Could not restart workflow, because workflow is not auto-restartable.'


    ApplicationContext context

    SchedulerService schedulerService

    CommentService commentService


    void handleAction(JobErrorDefinition.Action action, Job job) {
        if (!action) {
            job.log.debug("Stopping, because action is null.")
            return
        }

        job.log.debug("Handling action ${action}.")

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

    private void restartJob(Job job) {
        ProcessingStep step = job.processingStep

        if (job instanceof AutoRestartableJob) {
            schedulerService.restartProcessingStep(step)
            job.log.debug('Job restarted.')
        } else {
            throw new RuntimeException(JOB_NOT_AUTO_RESTARTABLE)
        }
    }

    private void restartWorkflow(Job job) {
        ProcessingStep step = job.processingStep
        JobExecutionPlan plan = step.jobExecutionPlan
        StartJob startJob = context.getBean(plan.startJob.bean)

        //This check is here because of OTP-1012
        assert plan.name == startJob.getJobExecutionPlanName()

        if (startJob instanceof RestartableStartJob) {
            ((RestartableStartJob) startJob).restart()
            logInCommentAndJobLog(job, WORKFLOW_RESTARTED)
        } else {
            throw new RuntimeException(WORKFLOW_NOT_AUTO_RESTARTABLE)
        }
    }


    void logInCommentAndJobLog(Job job, String message) {
        job.log.debug(message)
        ProcessingStep step = job.processingStep
        Process process = step.process
        Comment comment = process.comment ?: new Comment()

        String commentMessage
        if (comment.comment) {
            commentMessage = "${comment.comment}\n\n${message}"
        } else {
            commentMessage = message
        }

        commentService.createOrUpdateComment(process, commentMessage, 'auto-restart handler')
    }

}
