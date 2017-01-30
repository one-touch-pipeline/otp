package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl

/**
 * Test implementation of {@link AbstractStartJobImpl} for use by
 * {@link de.dkfz.tbi.otp.job.processing.AbstractStartJobImplTests}. Necessary because mocking methods via metaClass
 * does not work in this case (for whatever reason).
 */
@Component('testStartJob2')
@Scope('prototype')
class TestStartJob2 extends AbstractStartJobImpl {

    JobExecutionPlan jep
    int totalSlots
    int slotsReservedForFastTrack

    @Override
    JobExecutionPlan getJobExecutionPlan() {
        return jep
    }

    @Override
    protected int getConfiguredSlotCount(final JobExecutionPlan plan, final String optionName, final int defaultValue) {
        assert plan == jep
        if (optionName == TOTAL_SLOTS_OPTION_NAME) {
            assert defaultValue == 1
            return totalSlots
        } else if (optionName == SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME) {
            assert defaultValue == 0
            return slotsReservedForFastTrack
        } else {
            throw new AssertionError("Unexpected value for optionName: ${optionName}")
        }
    }

    @Override
    void execute() {
        throw new UnsupportedOperationException()
    }
}
