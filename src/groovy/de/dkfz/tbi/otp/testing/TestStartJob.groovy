package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.StartJob

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple Test implementation of the StartJob interface.
 * Does nothing useful.
 *
 */
@Component("testStartJob")
@Scope("prototype")
class TestStartJob implements StartJob {
    private JobExecutionPlan plan
    
    TestStartJob() {}
    TestStartJob(JobExecutionPlan plan) {
        this.plan = plan
    }

    @Override
    public JobExecutionPlan getExecutionPlan() {
        return plan
    }

    @Override
    public String getVersion() {
        return ""
    }

}
