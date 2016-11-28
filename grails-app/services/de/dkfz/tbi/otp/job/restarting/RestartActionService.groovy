package de.dkfz.tbi.otp.job.restarting

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*
import org.springframework.security.access.prepost.*

class RestartActionService {

    static String JOB_NOT_AUTO_RESTARTABLE = 'Could not restart job, because job is not auto-restartable.'
    static String WORKFLOW_RESTARTED = 'Workflow restarted.'
    static String WORKFLOW_NOT_AUTO_RESTARTABLE = 'Could not restart workflow, because workflow is not auto-restartable.'
    static String WORKFLOW_NOT_RESTARTED = 'Could not restart workflow, because workflow is not in failed state or already restarted.'

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
        StartJob startJob = getStartJob(step.process)

        if (startJob instanceof RestartableStartJob) {
            restart(startJob, step.process)
            logInCommentAndJobLog(job, WORKFLOW_RESTARTED)
        } else {
            throw new RuntimeException(WORKFLOW_NOT_AUTO_RESTARTABLE)
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    protected void restartWorkflowWithProcess(long processId) {
        Process process = processService.getProcess(processId)
        StartJob startJob = getStartJob(process)
        ProcessingStepUpdate processingStepUpdate = processService.getLatestProcessingStepUpdate(processService.getLatestProcessingStep(process))

        if (processingStepUpdate.state == ExecutionState.FAILURE && !Process.findByRestarted(process)) {
            restart(startJob, process)
            logInComment(process, WORKFLOW_RESTARTED)
        } else {
            throw new RuntimeException(WORKFLOW_NOT_RESTARTED)
        }
    }

    StartJob getStartJob(Process process) {
        JobExecutionPlan plan = process.jobExecutionPlan
        StartJob startJob = context.getBean(plan.startJob.bean)

        //This check is here because of OTP-1012
        assert plan.name == startJob.getJobExecutionPlanName()

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

    String commentMessage(Process process, String message){
        Comment comment = process.comment ?: new Comment()
        if (comment.comment) {
            return "${comment.comment}\n\n${message}"
        } else {
            return message
        }
    }
}
