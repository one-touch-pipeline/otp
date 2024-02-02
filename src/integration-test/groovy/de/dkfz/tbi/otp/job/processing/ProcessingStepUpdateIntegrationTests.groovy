/*
 * Copyright 2011-2024 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.Test

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan

import static org.junit.Assert.*

@Rollback
@Integration
class ProcessingStepUpdateIntegrationTests {

    @Test
    void testConstraints() {
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan()
        jobExecutionPlan.name = "testPlan"
        jobExecutionPlan.planVersion = 0
        jobExecutionPlan.previousPlan = null
        jobExecutionPlan.save(flush: true)
        assertTrue(jobExecutionPlan.validate())
        Process process = new Process()
        process.jobExecutionPlan = jobExecutionPlan
        process.started = new Date(System.currentTimeMillis())
        process.startJobClass = "testStartJob"
        process.save(flush: true)
        assertTrue(process.validate())
        JobDefinition jobDefinition = new JobDefinition()
        jobDefinition.name = "testJobDefinition"
        jobDefinition.bean = "testJob"
        jobDefinition.plan = jobExecutionPlan
        jobDefinition.save(flush: true)
        assertTrue(jobDefinition.validate())
        // Initialize 1. step
        ProcessingStep processingStep = new ProcessingStep()
        processingStep.process = process
        processingStep.jobDefinition = jobDefinition
        processingStep.save(flush: true)
        assertTrue(processingStep.validate())
        // Initialize 1. update
        ProcessingStepUpdate processingStepUpdate = new ProcessingStepUpdate()
        processingStepUpdate.previous = null
        processingStepUpdate.state = null
        processingStepUpdate.date = null
        processingStepUpdate.processingStep = processingStep
        // Should not validate
        assertFalse(processingStepUpdate.validate())
        processingStepUpdate.state = ExecutionState.CREATED
        processingStepUpdate.date = new Date(System.currentTimeMillis())
        processingStepUpdate.save(flush: true)
        // Should validate
        assertTrue(processingStepUpdate.validate())
        processingStepUpdate.processingStep = processingStep
        // Should validate
        assertTrue(processingStepUpdate.validate())
        // Initialize 2. update
        ProcessingStepUpdate processingStepUpdate2 = new ProcessingStepUpdate()
        processingStepUpdate2.previous = null
        processingStepUpdate2.state = null
        processingStepUpdate2.date = null
        processingStepUpdate2.processingStep = processingStep
        // Should not validate
        assertFalse(processingStepUpdate2.validate())
        // Add missing values
        processingStepUpdate2.state = ExecutionState.STARTED
        processingStepUpdate2.previous = processingStepUpdate
        processingStepUpdate2.date = new Date(System.currentTimeMillis())
        processingStepUpdate2.processingStep = processingStep
        // Should validate
        assertTrue(processingStepUpdate2.validate())
        assertTrue(processingStepUpdate.validate())

        JobDefinition jobDefinition2 = new JobDefinition()
        jobDefinition2.name = "testJobDefinition2"
        jobDefinition2.bean = "testJob"
        jobDefinition2.plan = jobExecutionPlan
        assertNotNull(jobDefinition2.save(flush: true))
        // Initialize 2. step
        ProcessingStep processingStep2 = new ProcessingStep()
        processingStep2.process = process
        processingStep2.jobDefinition = jobDefinition2
        assertTrue(processingStep2.validate())
        assertNotNull(processingStep2.save(flush: true))
        // Should crash tests, updates must belong to same step
        processingStepUpdate2.processingStep = processingStep2
        // Should not validate
        assertFalse(processingStepUpdate2.validate())
        // Make this work again
        processingStepUpdate2.processingStep = processingStep
        // Set wrong value for Execution state
        processingStepUpdate2.state = ExecutionState.CREATED
        // Should not validate
        assertFalse(processingStepUpdate2.validate())
        // Make it work again
        processingStepUpdate2.state = ExecutionState.STARTED
        assertTrue(processingStepUpdate2.validate())
        // Set wrong date
        processingStepUpdate2.date = new Date(System.currentTimeMillis() - 20000)
        // Should not validate
        assertFalse(processingStepUpdate2.validate())
        // Set appropriate date again
        processingStepUpdate2.date = new Date(System.currentTimeMillis())
        // Should validate
        assertTrue(processingStepUpdate2.validate())
        // Initialize error object
        ProcessingError processingError = new ProcessingError()
        processingError.errorMessage = "To fail the test."
        assertTrue(processingError.validate())
        // Reassign state to STARTED to produce error
        processingStepUpdate2.state = ExecutionState.STARTED
        processingStepUpdate2.error = processingError
        // Should not validate
        assertFalse(processingStepUpdate2.validate())
        // Set state to FAILURE again
        processingStepUpdate2.state = ExecutionState.FAILURE
        // Should validate
        assertTrue(processingStepUpdate2.validate())
        // To fail the previous constraint, the order is reversed
        processingStepUpdate2.previous = null
        processingStepUpdate.previous = processingStepUpdate2
        // Should not validate
        assertFalse(processingStepUpdate2.validate())
    }
}
