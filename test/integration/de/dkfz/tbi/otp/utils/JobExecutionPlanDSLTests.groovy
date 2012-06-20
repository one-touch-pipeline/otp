package de.dkfz.tbi.otp.utils

import static org.junit.Assert.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan

class JobExecutionPlanDSLTests {
    def grailsApplication
    def planValidatorService

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
