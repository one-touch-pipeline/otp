package de.dkfz.tbi.otp.job.plan

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

import de.dkfz.tbi.otp.job.plan.JobDefinition;
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan;

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(JobDefinition)
class JobDefinitionTests {

    void testConstraints() {
        mockForConstraintsTests(JobDefinition, [])
        JobDefinition jobDefinition = new JobDefinition()
        assertFalse(jobDefinition.validate())
        assertEquals("nullable", jobDefinition.errors["name"])
        assertEquals("nullable", jobDefinition.errors["bean"])
        assertEquals("nullable", jobDefinition.errors["plan"])

        // mock the JobExecutionPlan
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan()
        mockDomain(JobExecutionPlan, [jobExecutionPlan])
        jobDefinition.plan = jobExecutionPlan
        assertFalse(jobDefinition.validate())

        // mock the previous JobDefinition
        JobDefinition previous = new JobDefinition()
        mockDomain(JobDefinition, [previous])
        jobDefinition.previous = previous
        assertFalse(jobDefinition.validate())

        // mock the next JobDefinition
        JobDefinition next = new JobDefinition()
        mockDomain(JobDefinition, [next])
        jobDefinition.next = next
        assertFalse(jobDefinition.validate())

        jobDefinition.name = "testDefinition"
        assertFalse(jobDefinition.validate())
        jobDefinition.bean = "testBean"
        assertTrue(jobDefinition.validate())
    }
}
