package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.JobExecutionPlanChangedEvent
import de.dkfz.tbi.otp.job.processing.StartJob

import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple Test implementation of the StartJob interface.
 * Does nothing useful.
 * This is similar to TestStartJob with the difference that it is a singleton
 * instance and allows to change the JobExecutionPlan during testing. For that
 * it does not inherit from AbstractStartJob, but just implements the StartJob
 * interface.
 */
@Component("testSingletonStartJob")
@Scope("singleton")
class TestSingletonStartJob implements StartJob, ApplicationListener<JobExecutionPlanChangedEvent> {
    private JobExecutionPlan plan
    private boolean planNeedsRefresh = false

    TestSingletonStartJob() {}
    TestSingletonStartJob(JobExecutionPlan plan) {
        this.plan = plan
    }

    @Override
    JobExecutionPlan getJobExecutionPlan() {
        if (planNeedsRefresh) {
            plan = JobExecutionPlan.get(plan.id)
            planNeedsRefresh = false
        }
        return plan
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    void execute() {}

    void setExecutionPlan(JobExecutionPlan plan) {
        this.plan = JobExecutionPlan.get(plan?.id)
    }

    void onApplicationEvent(JobExecutionPlanChangedEvent event) {
        if (this.plan && event.planId == this.plan.id) {
            planNeedsRefresh = true
        }
    }
}
