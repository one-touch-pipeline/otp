package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*

import grails.test.mixin.*
import org.junit.*
import grails.test.mixin.support.*
import de.dkfz.tbi.otp.job.plan.DecidingJobDefinition
import de.dkfz.tbi.otp.job.plan.JobDecision
import de.dkfz.tbi.otp.job.plan.JobDefinition

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(DecisionProcessingStep)
class DecisionProcessingStepTests {

    @Test
    void testDecision() {
        Process process = new Process()
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
        JobDefinition jobDefinition2 = new JobDefinition()
        mockDomain(Process, [process])
        mockDomain(JobDefinition, [jobDefinition2])
        mockDomain(DecidingJobDefinition, [jobDefinition])
        mockForConstraintsTests(DecisionProcessingStep, [])

        DecisionProcessingStep step = new DecisionProcessingStep(process: process, jobDefinition: jobDefinition)
        assertTrue(step.validate())
        JobDecision decision = new JobDecision(jobDefinition: jobDefinition2)
        JobDecision decision2 = new JobDecision(jobDefinition: jobDefinition)
        mockDomain(JobDecision, [decision, decision2])
        step.decision = decision
        assertFalse(step.validate())
        assertEquals("jobDefinition", step.errors["decision"])
        step.decision = decision2
        assertTrue(step.validate())
    }
}
