package de.dkfz.tbi.otp.job.plan

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.junit.Assert.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(JobDefinition)
class JobDefinitionTests {

    @Test
    void testConstraints() {
        JobDefinition jobDefinition = new JobDefinition()
        assertFalse(jobDefinition.validate())
        assertEquals("nullable", jobDefinition.errors["name"].code)
        assertEquals("nullable", jobDefinition.errors["bean"].code)
        assertEquals("nullable", jobDefinition.errors["plan"].code)

        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan(name: 'some name')
        jobDefinition.plan = jobExecutionPlan
        assertFalse(jobDefinition.validate())

        jobDefinition.name = "testDefinition"
        assertFalse(jobDefinition.validate())

        jobDefinition.bean = "testBean"
        assertTrue(jobDefinition.validate())

        JobDefinition previous = new JobDefinition()
        jobDefinition.previous = previous
        assertTrue(jobDefinition.validate())

        JobDefinition next = new JobDefinition()
        jobDefinition.next = next
        assertTrue(jobDefinition.validate())
    }
}
