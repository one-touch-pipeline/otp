package de.dkfz.tbi.otp.job.jobs.snvcalling

import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl

@Component('samplePairDiscoveryStartJob')
@Scope('singleton')
class SamplePairDiscoveryStartJob extends AbstractStartJobImpl {

    @Override
    @Scheduled(cron = '0 7 12,20 * * *')
    void execute() {
        if (freeSlotAvailable) {
            createProcess([])
        }
    }

    @Override
    String getJobExecutionPlanName() {
        return 'SamplePairDiscoveryWorkflow'
    }
}
