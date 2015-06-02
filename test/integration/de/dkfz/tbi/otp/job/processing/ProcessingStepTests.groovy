package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.jobs.merging.MergingJob
import org.junit.Test

class ProcessingStepTests  {

    @Test
    void testBelongsToMultiJob_WhenJobIsMultiJob_ShouldReturnTrue() {
        // AbstractOtpJob as dummy for Multijob
        Class testMultiJob = AbstractOtpJob
        ProcessingStep p = ProcessingStep.build([jobClass: testMultiJob.getName()])
        assert p.belongsToMultiJob()
    }

    @Test
    void testBelongsToMultiJob_WhenJobIsNoMultiJob_ShouldReturnFalse() {
        // MergingJob as dummy for non-Multijob
        Class testJob = MergingJob
        ProcessingStep p = ProcessingStep.build([jobClass: testJob.getName()])
        assert !p.belongsToMultiJob()
    }
}
