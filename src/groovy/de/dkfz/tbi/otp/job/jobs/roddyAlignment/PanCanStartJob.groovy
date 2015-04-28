package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component('panCanStartJob')
@Scope('singleton')
class PanCanStartJob extends RoddyAlignmentStartJob {

    @Override
    protected String getJobExecutionPlanName() {
        return 'PanCanWorkflow'
    }
}
