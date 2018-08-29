package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import static de.dkfz.tbi.otp.ngsdata.DomainFactory.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * Helper class which returns job system related database objects
 */
class TestJobHelper {

    public static TestJob createTestJobWithProcessingStep(final ProcessParameterObject processParameterValue = null) {
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
