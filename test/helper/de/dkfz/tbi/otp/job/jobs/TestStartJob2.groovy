package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

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
    protected int getConfiguredSlotCount(final JobExecutionPlan plan, final OptionName optionName, final int defaultValue) {
        assert plan == jep
        if (optionName == OptionName.MAXIMUM_NUMBER_OF_JOBS) {
            assert defaultValue == 1
            return totalSlots
        } else if (optionName == OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK) {
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
