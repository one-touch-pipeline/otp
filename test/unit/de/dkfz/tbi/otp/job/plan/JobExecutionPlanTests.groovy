package de.dkfz.tbi.otp.job.plan

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.Test

import de.dkfz.tbi.otp.job.processing.ProcessParameter

import static org.junit.Assert.*

@TestMixin(ControllerUnitTestMixin)
@TestFor(JobExecutionPlan)
class JobExecutionPlanTests {

    @Test
    void testConstraints() {
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan()
        assertFalse(jobExecutionPlan.validate())
        assertEquals("nullable", jobExecutionPlan.errors["name"].code)

        JobDefinition jobDefinition = new JobDefinition()
        jobExecutionPlan.firstJob = jobDefinition
        assertFalse(jobExecutionPlan.validate())

        StartJobDefinition startJobDefinition = new StartJobDefinition()
        jobExecutionPlan.startJob = startJobDefinition
        assertFalse(jobExecutionPlan.validate())

        JobExecutionPlan previous = new JobExecutionPlan()
        jobExecutionPlan.previousPlan = previous
        assertFalse(jobExecutionPlan.validate())
        assertEquals("validator.invalid", jobExecutionPlan.errors["previousPlan"].code)

        jobExecutionPlan.name = "testPlan"
        // Assign smallest possible planVersion
        jobExecutionPlan.planVersion = 0
        assertFalse(jobExecutionPlan.validate())
        // Set previousPlan to null to pass validation
        jobExecutionPlan.previousPlan = null
        assertTrue(jobExecutionPlan.validate())
        jobExecutionPlan.previousPlan = previous
        // Assign smallest possible planVersion
        jobExecutionPlan.planVersion = 0
        // Assign higher planVersion value for previousPlan
        jobExecutionPlan.previousPlan.planVersion = 1
        assertFalse(jobExecutionPlan.validate())
        // Assign higher value to planVersion
        jobExecutionPlan.planVersion = 1
        // Assign small planVersion value for previousPlan
        jobExecutionPlan.previousPlan.planVersion = 0
        jobExecutionPlan.previousPlan.name = "testPlan"
        assertTrue(jobExecutionPlan.validate())
    }

    @Test
    void testProcessParameters() {
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan()
        assertFalse(jobExecutionPlan.validate())

        jobExecutionPlan.name = "testPlan"
        assertTrue(jobExecutionPlan.validate())
        ProcessParameter processParameter = new ProcessParameter(value: "test")
        jobExecutionPlan.processParameter = processParameter

        assertTrue(jobExecutionPlan.validate())
        assertTrue(jobExecutionPlan.processParameter.is(processParameter))
        assertEquals("test".toString(), processParameter.value)
    }
}
