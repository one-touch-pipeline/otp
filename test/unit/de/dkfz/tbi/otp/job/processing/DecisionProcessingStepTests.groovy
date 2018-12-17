package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import de.dkfz.tbi.otp.job.plan.*

import static org.junit.Assert.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(DecisionProcessingStep)
class DecisionProcessingStepTests {

    @Test
    void testDecision() {
        Process process = new Process()
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
        JobDefinition jobDefinition2 = new JobDefinition()

        DecisionProcessingStep step = new DecisionProcessingStep(process: process, jobDefinition: jobDefinition)
        assertTrue(step.validate())
        JobDecision decision = new JobDecision(jobDefinition: jobDefinition2)
        JobDecision decision2 = new JobDecision(jobDefinition: jobDefinition)
        step.decision = decision
        assertFalse(step.validate())
        assertEquals("jobDefinition", step.errors["decision"].code)
        step.decision = decision2
        assertTrue(step.validate())
    }
}
