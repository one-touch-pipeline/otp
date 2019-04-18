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
