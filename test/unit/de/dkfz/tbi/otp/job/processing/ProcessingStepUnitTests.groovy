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

package de.dkfz.tbi.otp.job.processing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan

import static org.junit.Assert.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ProcessingStep)
@Build([ProcessingStep])
class ProcessingStepUnitTests {

    @Test
    void testNullable() {
        ProcessingStep step = new ProcessingStep()
        assertFalse(step.validate())
        assertEquals("nullable", step.errors["jobDefinition"].code)
        assertEquals("nullable", step.errors["process"].code)

        Process process = new Process()
        step.process = process
        assertFalse(step.validate())
        assertEquals("nullable", step.errors["jobDefinition"].code)
        assertNull(step.errors["process"])

        JobDefinition jobDefinition = new JobDefinition()
        step.jobDefinition = jobDefinition
        assertTrue(step.validate())
    }

    @Test
    void testPrevious() {
        Process process = Process.build(id: 1)
        Process process2 = Process.build(id: 2)
        JobDefinition jobDefinition = JobDefinition.build(id: 1, plan: process.jobExecutionPlan)
        JobDefinition jobDefinition2 = JobDefinition.build(id: 2, plan: process2.jobExecutionPlan)

        ProcessingStep step = ProcessingStep.build(process: process, jobDefinition: jobDefinition)
        assertTrue(step.validate())

        ProcessingStep testStep1 = ProcessingStep.build(process: process2, jobDefinition: jobDefinition2)
        step.previous = testStep1
        assertFalse(step.validate())
        assertEquals("process", step.errors["previous"].code)

        ProcessingStep testStep2 = ProcessingStep.build(process: process, jobDefinition: jobDefinition)
        step.previous = testStep2
        assertFalse(step.validate())
        assertEquals("jobDefinition", step.errors["previous"].code)

        jobDefinition2.plan = process.jobExecutionPlan
        ProcessingStep testStep3 = ProcessingStep.build(process: process, jobDefinition: jobDefinition2)

        step.previous = testStep3
        step.next = testStep3
        assertFalse(step.validate())
        assertEquals("next", step.errors["previous"].code)
        assertEquals("previous", step.errors["next"].code)

        step.next = null
        assertTrue(step.validate())
    }

    @Test
    void testNext() {
        Process process = Process.build(id: 1)
        Process process2 = Process.build(id: 2)
        JobDefinition jobDefinition = JobDefinition.build(id: 1, plan: process.jobExecutionPlan)
        JobDefinition jobDefinition2 = JobDefinition.build(id: 2, plan: process2.jobExecutionPlan)

        ProcessingStep step = ProcessingStep.build(process: process, jobDefinition: jobDefinition)
        assertTrue(step.validate())

        ProcessingStep testStep1 = ProcessingStep.build(process: process2, jobDefinition: jobDefinition2)
        step.next = testStep1
        assertFalse(step.validate())
        assertEquals("process", step.errors["next"].code)

        ProcessingStep testStep2 = ProcessingStep.build(process: process, jobDefinition: jobDefinition)
        step.next = testStep2
        assertFalse(step.validate())
        assertEquals("jobDefinition", step.errors["next"].code)

        jobDefinition2.plan = process.jobExecutionPlan
        ProcessingStep testStep3 = ProcessingStep.build(process: process, jobDefinition: jobDefinition2)
        step.previous = testStep3
        step.next = testStep3
        assertFalse(step.validate())
        assertEquals("next", step.errors["previous"].code)
        assertEquals("previous", step.errors["next"].code)

        step.previous = null
        assertTrue(step.validate())
    }

    @Test
    void testProcess() {
        JobExecutionPlan plan1 = JobExecutionPlan.build(id: 1)
        JobExecutionPlan plan2 = JobExecutionPlan.build(id: 2)
        JobDefinition jobDefinition = JobDefinition.build(plan: plan1)
        Process process = Process.build(jobExecutionPlan: plan2)
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)

        assertFalse(step.validate())
        assertEquals("jobExecutionPlan", step.errors["process"].code)
        process.jobExecutionPlan = plan1
        assertTrue(step.validate())
    }
}
