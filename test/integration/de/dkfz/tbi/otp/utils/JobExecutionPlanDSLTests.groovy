package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.testing.*
import org.junit.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
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
