package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.jobs.TestJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.*

/**
 * Helper class which returns a TestJob with a processingStep
 *
 */
class TestJobHelper {

    public static TestJob createTestJobWithProcessingStep(final Object processParameterValue = null) {
        ProcessingStep processingStep = createAndSaveProcessingStep()
        if (processParameterValue != null) {
            assert createProcessParameter(processingStep.process, processParameterValue).save(failOnError: true)
        }
        TestJob testJob = new TestJob(processingStep, [])
        return testJob
    }
}
