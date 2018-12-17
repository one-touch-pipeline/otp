package de.dkfz.tbi.otp.job.restarting

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*

class RestartCheckerService {

    @Autowired
    ApplicationContext context

    boolean canJobBeRestarted(Job job) {
        return job instanceof AutoRestartableJob
    }

    boolean canWorkflowBeRestarted(ProcessingStep step) {
        JobExecutionPlan plan = step.process.jobExecutionPlan
        StartJob startJob = context.getBean(plan.startJob.bean)
        return startJob instanceof RestartableStartJob
    }

    boolean isJobAlreadyRestarted(ProcessingStep step) {
        return step instanceof RestartedProcessingStep
    }

    boolean isWorkflowAlreadyRestarted(ProcessingStep step) {
        return step.process.restarted
    }
}
