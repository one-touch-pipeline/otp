package de.dkfz.tbi.otp.utils

import org.junit.Test

import de.dkfz.tbi.otp.integration.AbstractIntegrationTest
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.PlanValidatorService

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.plan
import static org.junit.Assert.*

class JobExecutionPlanDSLTests extends AbstractIntegrationTest {
    PlanValidatorService planValidatorService

    @Test
    void testEmptyPlan() {
        assertEquals(0, JobExecutionPlan.count())
        plan("test") {
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        assertFalse(planValidatorService.validate(jep).isEmpty())
        plan("test2") {
            start("startJob", "testStartJob")
        }
        jep = JobExecutionPlan.list().last()
        assertFalse(planValidatorService.validate(jep).isEmpty())
    }

    @Test
    void testWatchDog() {
        assertEquals(0, JobExecutionPlan.count())
        plan("test") {
            start("startJob", "testStartJob")
            job("test", "testJob") {
                watchdog("testEndStateAwareJob")
            }
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        def errors = planValidatorService.validate(jep)
        assertTrue(errors.isEmpty())
    }
}
