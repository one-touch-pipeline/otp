package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.jobs.roddyAlignment.RoddyAlignmentStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component('testRoddyAlignmentStartJob')
@Scope('singleton')
class TestRoddyAlignmentStartJob extends RoddyAlignmentStartJob {

    JobExecutionPlan jep

    @Override
    JobExecutionPlan getExecutionPlan() {
        return jep
    }

    @Override
    protected String getJobExecutionPlanName() {
        throw new UnsupportedOperationException()
    }
}
