package de.dkfz.tbi.otp.job.restarting

import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*

class RestartCheckerService {

    @Autowired
    ApplicationContext context

    boolean canJobBeRestarted(Job job) {
        return job instanceof AutoRestartableJob
    }

    boolean canWorkflowBeRestarted(ProcessingStep step) {
        JobExecutionPlan plan = step.process.jobExecutionPlan
        StartJob startJob = context.getBean(plan.startJob.bean)

        //This check is here because of OTP-1012
        assert plan.name == startJob.getJobExecutionPlanName()

        return startJob instanceof RestartableStartJob
    }

    boolean isJobAlreadyRestarted(ProcessingStep step) {
        return step instanceof RestartedProcessingStep
    }

    boolean isWorkflowAlreadyRestarted(ProcessingStep step) {
        return step.process.restarted
    }
}
