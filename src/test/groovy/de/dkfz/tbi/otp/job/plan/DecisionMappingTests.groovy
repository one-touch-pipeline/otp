/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.job.plan

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.junit.Assert.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(DecisionMapping)
@Build([JobExecutionPlan, JobDecision, JobDefinition])
class DecisionMappingTests {

    @Test
    void testJobDefinition() {
        DecidingJobDefinition jobDefinition = new DecidingJobDefinition()
        JobDefinition jobDefinition2 = new JobDefinition()
        JobDecision decision = new JobDecision(jobDefinition: jobDefinition)

        DecisionMapping mapping = new DecisionMapping()
        assertFalse(mapping.validate())
        assertEquals("nullable", mapping.errors["decision"].code)
        assertEquals("nullable", mapping.errors["definition"].code)

        mapping.decision = decision
        assertFalse(mapping.validate())
        assertEquals("nullable", mapping.errors["definition"].code)
        assertNull(mapping.errors["decision"])

        mapping.definition = jobDefinition
        assertFalse(mapping.validate())
        assertEquals("recursive", mapping.errors["definition"].code)

        mapping.definition = jobDefinition2
        assertTrue(mapping.validate())
    }

    @Test
    void testJobExecutionPlan() {
        JobExecutionPlan plan = new JobExecutionPlan()
        JobExecutionPlan plan1 = new JobExecutionPlan()

        DecidingJobDefinition jobDefinition = new DecidingJobDefinition(plan: plan)

        JobDefinition jobDefinition2 = new JobDefinition(plan: plan1)
        JobDefinition jobDefinition3 = new JobDefinition(plan: plan)
        JobDecision decision = new JobDecision(jobDefinition: jobDefinition)

        DecisionMapping mapping = new DecisionMapping(decision: decision, definition: jobDefinition2)
        assertFalse(mapping.validate())
        assertEquals("plan", mapping.errors["definition"].code)

        mapping.definition = jobDefinition3
        assertTrue(mapping.validate())
    }

    @Test
    void testUniqueness() {
        JobExecutionPlan jep = JobExecutionPlan.build(name: "some name", planVersion: 0)

        DecidingJobDefinition decidingJobDefinition1 = new DecidingJobDefinition(name: "some name", bean: "someBean", plan: jep).save(flush: true)
        DecidingJobDefinition decidingJobDefinition2 = new DecidingJobDefinition(name: "other name", bean: "someOtherBean", plan: jep).save(flush: true)

        JobDecision decision = JobDecision.build(jobDefinition: decidingJobDefinition1).save(flush: true)
        JobDecision decision2 = JobDecision.build(jobDefinition: decidingJobDefinition1).save(flush: true)

        DecisionMapping mapping1 = new DecisionMapping(decision: decision, definition: decidingJobDefinition2)
        assert mapping1.save(flush: true)

        DecisionMapping mapping2 = new DecisionMapping(decision: decision, definition: decidingJobDefinition2)

        assertFalse(mapping2.validate())
        assertEquals("unique", mapping2.errors["decision"].code)

        mapping2.decision = decision2
        assertTrue(mapping2.validate())
    }
}
