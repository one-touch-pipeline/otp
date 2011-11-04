package de.dkfz.tbi.otp.job.plan

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(JobDecision)
class JobDecisionTests {

    @Test
    void testNullableAndBlank() {
       DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
       mockDomain(DecidingJobDefinition, [jobDefinition])
       mockForConstraintsTests(JobDecision, [])

       JobDecision decision = new JobDecision()
       assertFalse(decision.validate())
       assertEquals("nullable", decision.errors["jobDefinition"])
       assertEquals("nullable", decision.errors["name"])
       assertEquals("nullable", decision.errors["description"])

       decision.jobDefinition = jobDefinition
       decision.name = ""
       decision.description = ""
       assertFalse(decision.validate())
       assertEquals("blank", decision.errors["name"])
       assertNull(decision.errors["jobDefinition"])
       assertNull(decision.errors["description"])

       decision.name = "test"
       decision.description = "test"
       assertTrue(decision.validate())
    }

    @Test
    void testUniqueness() {
       DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
       mockDomain(DecidingJobDefinition, [jobDefinition])
       mockForConstraintsTests(JobDecision, [new JobDecision(jobDefinition: jobDefinition, name: "test", description: "")])

       JobDecision decision = new JobDecision(jobDefinition: jobDefinition, name: "test", description: "")
       assertFalse(decision.validate())
       assertEquals("unique", decision.errors["name"])
       decision.name = "test2"
       decision.validate()
       println decision.errors
       assertTrue(decision.validate())
    }
}
