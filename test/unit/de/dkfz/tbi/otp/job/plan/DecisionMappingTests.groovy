package de.dkfz.tbi.otp.job.plan

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(DecisionMapping)
class DecisionMappingTests {

    @Test
    void testJobDefinition() {
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
        mockDomain(DecidingJobDefinition, [jobDefinition])
        JobDefinition jobDefinition2 = new JobDefinition()
        JobDefinition jobDefinition3 = new JobDefinition()
        mockDomain(JobDefinition, [jobDefinition2, jobDefinition3])
        JobDecision decision = new JobDecision(jobDefinition: jobDefinition)
        mockDomain(JobDecision, [decision])
        mockForConstraintsTests(DecisionMapping, [])

        DecisionMapping mapping = new DecisionMapping()
        assertFalse(mapping.validate())
        assertEquals("nullable", mapping.errors["decision"])
        assertEquals("nullable", mapping.errors["definition"])

        mapping.decision = decision
        assertFalse(mapping.validate())
        assertEquals("nullable", mapping.errors["definition"])
        assertNull(mapping.errors["decision"])

        mapping.definition = jobDefinition
        assertFalse(mapping.validate())
        assertEquals("recursive", mapping.errors["definition"])

        mapping.definition = jobDefinition2
        assertTrue(mapping.validate())
    }

    @Test
    void testJobExecutionPlan() {
        JobExecutionPlan plan = new JobExecutionPlan()
        JobExecutionPlan plan1 = new JobExecutionPlan()
        mockDomain(JobExecutionPlan, [plan, plan1])
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition(plan: plan)
        mockDomain(DecidingJobDefinition, [jobDefinition])
        JobDefinition jobDefinition2 = new JobDefinition(plan: plan1)
        JobDefinition jobDefinition3 = new JobDefinition(plan: plan)
        mockDomain(JobDefinition, [jobDefinition2, jobDefinition3])
        JobDecision decision = new JobDecision(jobDefinition: jobDefinition)
        mockDomain(JobDecision, [decision])
        mockForConstraintsTests(DecisionMapping, [])

        DecisionMapping mapping = new DecisionMapping(decision: decision, definition: jobDefinition2)
        assertFalse(mapping.validate())
        assertEquals("plan", mapping.errors["definition"])
        mapping.definition = jobDefinition3
        assertTrue(mapping.validate())
    }

    @Test
    void testUniqueness() {
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
        mockDomain(DecidingJobDefinition, [jobDefinition])
        JobDefinition jobDefinition2 = new JobDefinition()
        JobDefinition jobDefinition3 = new JobDefinition()
        mockDomain(JobDefinition, [jobDefinition2, jobDefinition3])
        JobDecision decision = new JobDecision(jobDefinition: jobDefinition)
        JobDecision decision2 = new JobDecision(jobDefinition: jobDefinition)
        mockDomain(JobDecision, [decision, decision2])
        mockForConstraintsTests(DecisionMapping, [new DecisionMapping(decision: decision2, definition: jobDefinition2)])

        DecisionMapping mapping = new DecisionMapping(decision: decision2, definition: jobDefinition3)
        assertFalse(mapping.validate())
        assertEquals("unique", mapping.errors["decision"])
        mapping.decision = decision
        assertTrue(mapping.validate())
    }
}
