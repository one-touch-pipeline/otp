package de.dkfz.tbi.otp.job.jobs

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.*

/**
 * Helper class which returns job system related database objects
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

    public static Collection<Process> findProcessesForPlanName(String planName) {
        return Process.findAllByJobExecutionPlan(findJobExecutionPlan(planName))
    }

    public static JobExecutionPlan findJobExecutionPlan(String planName) {
        return exactlyOneElement(JobExecutionPlan.findAllByName(planName))
    }
}
