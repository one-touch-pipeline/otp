package de.dkfz.tbi.otp.job.plan

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

import de.dkfz.tbi.otp.job.processing.ProcessParameter;

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(JobExecutionPlan)
class JobExecutionPlanTests {

    void testConstraints() {
        mockForConstraintsTests(JobExecutionPlan, [])
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan()
        assertFalse(jobExecutionPlan.validate())
        assertEquals("nullable", jobExecutionPlan.errors["name"])

        // mock the JobDefinition
        JobDefinition jobDefinition = new JobDefinition()
        JobDefinition jobDefinition2 = new JobDefinition()
        mockDomain(JobDefinition, [jobDefinition, jobDefinition2])
        jobExecutionPlan.firstJob = jobDefinition
        assertFalse(jobExecutionPlan.validate())

        // mock the StartJobDefinition
        StartJobDefinition startJobDefinition = new StartJobDefinition()
        StartJobDefinition startJobDefinition2 = new StartJobDefinition()
        mockDomain(StartJobDefinition, [startJobDefinition, startJobDefinition2])
        jobExecutionPlan.startJob = startJobDefinition
        assertFalse(jobExecutionPlan.validate())

        // mock the previous JobExecutionPlan
        JobExecutionPlan previous = new JobExecutionPlan()
        mockDomain(JobExecutionPlan, [previous])
        jobExecutionPlan.previousPlan = previous
        assertFalse(jobExecutionPlan.validate())
        assertEquals("validator", jobExecutionPlan.errors["previousPlan"])

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
        assertTrue(jobExecutionPlan.validate())
    }

    void testProcessParameters() {
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan()
        assertFalse(jobExecutionPlan.validate())
        jobExecutionPlan.name = "testPlan"
        assertTrue(jobExecutionPlan.validate())
        // mock the JobExecutionPlan to be able to assign it to the processParameterType
        mockDomain(JobExecutionPlan, [jobExecutionPlan])
        // mock the ProcessParameterType
        ProcessParameter processParameter = new ProcessParameter(value: "test")
        // mock the ProcessParameter
        mockDomain(ProcessParameter, [processParameter])
        // Add the processParameter to the jobExeceutionPlan
        jobExecutionPlan.processParameter = processParameter
        assertTrue(jobExecutionPlan.validate())
        assertTrue(jobExecutionPlan.processParameter.is(processParameter))
        assertEquals("test".toString(), processParameter.value)
    }
}
