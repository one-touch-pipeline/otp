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

package de.dkfz.tbi.otp.job.scheduler

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.core.GrailsApplication
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.integration.AbstractIntegrationTest
import de.dkfz.tbi.otp.job.JobMailService
import de.dkfz.tbi.otp.job.jobs.FailingTestJob
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.restarting.RestartHandlerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory

import java.util.regex.Pattern

import static org.junit.Assert.*

@Rollback
@Integration
class SchedulerTests extends AbstractIntegrationTest {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    Scheduler scheduler

    void setupData() {
        scheduler.schedulerService.metaClass.executeInNewThread = { Closure job ->
            job()
        }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(JobMailService, scheduler.jobMailService)
        TestCase.removeMetaClass(RestartHandlerService, scheduler.restartHandlerService)
        TestCase.removeMetaClass(SchedulerService, scheduler.schedulerService)
    }

    @Test
    void testNormalJobExecution() {
        setupData()
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job job = grailsApplication.mainContext.getBean("testEndStateAwareJob") as Job
        job.processingStep = step
        // There is no Created ProcessingStep update - execution should fail
        shouldFail(RuntimeException) {
            scheduler.executeJob(job)
        }
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        shouldFail(InvalidStateException) {
            job.getOutputParameters()
        }
        scheduler.executeJob(job)
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
        // and there should be some output parameters
        List<Parameter> params = step.output.toList().sort { it.type.name }
        assertEquals(2, params.size())
        assertEquals("test", params[0].type.name)
        assertEquals("1234", params[0].value)
        assertNull(params[0].type.className)
        assertSame(jobDefinition, params[0].type.jobDefinition)
        assertEquals("test2", params[1].type.name)
        assertEquals("4321", params[1].value)
        assertNull(params[1].type.className)
        assertSame(jobDefinition, params[1].type.jobDefinition)
    }

    @Test
    void testNormalEndStateAwareJobExecution() {
        setupData()
        JobExecutionPlan jep = new JobExecutionPlan(name: "testEndStateAware", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestEndStateAwareJob("testEndStateAware", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job endStateAwareJob = grailsApplication.mainContext.getBean("testEndStateAwareJob") as Job
        endStateAwareJob.processingStep = step
        // There is no Created ProcessingStep update - execution should fail
        shouldFail(RuntimeException) {
            scheduler.executeJob(endStateAwareJob)
        }
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        shouldFail(InvalidStateException) {
            endStateAwareJob.getOutputParameters()
        }
        shouldFail(InvalidStateException) {
            endStateAwareJob.getEndState()
        }
        scheduler.restartHandlerService.metaClass.handleRestart = { Job job ->
            assert false: 'Should not reach this point'
        }
        scheduler.executeJob(endStateAwareJob)
        assertEquals(ExecutionState.SUCCESS, endStateAwareJob.endState)
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
        // and there should be some output parameters
        List<Parameter> params = step.output.toList().sort { it.type.name }
        assertEquals(2, params.size())
        assertEquals("test", params[0].type.name)
        assertEquals("1234", params[0].value)
        assertNull(params[0].type.className)
        assertSame(jobDefinition, params[0].type.jobDefinition)
        assertEquals("test2", params[1].type.name)
        assertEquals("4321", params[1].value)
        assertNull(params[1].type.className)
        assertSame(jobDefinition, params[1].type.jobDefinition)
    }

    @Test
    void testFailingEndStateAwareJobExecution() {
        setupData()
        JobExecutionPlan jep = new JobExecutionPlan(name: "testFailureEndStateAware", planVersion: 0, startJobBean: "testStartJob")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestEndStateAwareJob("testEndStateAware", jep, null, "testFailureEndStateAwareJob")
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job endStateAwareJob = grailsApplication.mainContext.getBean("testFailureEndStateAwareJob") as Job
        endStateAwareJob.processingStep = step
        // There is no Created ProcessingStep update - execution should fail
        int executedCounter = 0
        scheduler.jobMailService.metaClass.sendErrorNotification = { Job job2, String message ->
            executedCounter++
        }
        shouldFail(RuntimeException) {
            scheduler.executeJob(endStateAwareJob)
        }
        assert 1 == executedCounter
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        shouldFail(InvalidStateException) {
            endStateAwareJob.getOutputParameters()
        }
        shouldFail(InvalidStateException) {
            endStateAwareJob.getEndState()
        }
        scheduler.executeJob(endStateAwareJob)
        assertEquals(ExecutionState.FAILURE, endStateAwareJob.endState)
        step.refresh()
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertTrue(process.finished)
        assert 2 == executedCounter
    }

    @Test
    void testFailingExecutionWithoutRestart() {
        setupData()
        helperForFailingExecution(/nonTesting/, [ExecutionState.CREATED, ExecutionState.STARTED, ExecutionState.FAILURE])
    }

    @Test
    void testFailingExecutionWithRestart() {
        setupData()
        helperForFailingExecution(Pattern.quote(FailingTestJob.EXCEPTION_MESSAGE), [ExecutionState.CREATED, ExecutionState.STARTED, ExecutionState.FAILURE, ExecutionState.RESTARTED])
    }

    void helperForFailingExecution(String errorExpression, List<ExecutionState> expectedExecutionStates) {
        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan(name: 'testStartJob')
        JobDefinition jobDefinition = DomainFactory.createJobDefinition(name: 'test', bean: 'failingTestJob', plan: jep)
        DomainFactory.createJobErrorDefinition(
                action: JobErrorDefinition.Action.RESTART_JOB,
                type: JobErrorDefinition.Type.MESSAGE,
                errorExpression: errorExpression,
                jobDefinitions: [jobDefinition],
        )
        jep.firstJob = jobDefinition
        assert jep.save(flush: true)
        Process process = DomainFactory.createProcess(jobExecutionPlan: jep, startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        ProcessingStep step = DomainFactory.createProcessingStep(process: process, jobDefinition: jobDefinition, jobClass: FailingTestJob.name)
        Job job = grailsApplication.mainContext.getBean("failingTestJob") as Job
        job.processingStep = step
        DomainFactory.createProcessingStepUpdate(
            state: ExecutionState.CREATED,
            processingStep: step
            )
        boolean notified = false
        scheduler.jobMailService.metaClass.sendErrorNotification = { Job job2, Throwable exceptionToBeHandled ->
            if (notified) {
                assert false: 'called twiced'
            } else {
                notified = true
            }
        }

        String message = shouldFail(Exception) {
            scheduler.executeJob(job)
        }
        assert message.contains(FailingTestJob.EXCEPTION_MESSAGE)
        step.refresh()
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assert expectedExecutionStates == updates*.state
        assertNotNull(updates[2].error)
        assertEquals(FailingTestJob.EXCEPTION_MESSAGE, updates[2].error.errorMessage)
        assert notified
    }

    @Test
    void testMissingOutputParameter() {
        setupData()
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // create a third parameter type for which the job does not create a parameter
        ParameterType type = new ParameterType(name: "fail", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(type.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        // run the Job
        Job job = grailsApplication.mainContext.getBean("testJob") as Job
        job.processingStep = step
        scheduler.executeJob(job)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("Required Output Parameter of type ${type.id} is not set.".toString(), updates[3].error.errorMessage)
    }

    @Test
    void testInputAsOutputParameter() {
        setupData()
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "directTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "directTestJob", plan: jep, previous: jobDefinition)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        assertNotNull(test.save())
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        Job job = grailsApplication.mainContext.getBean("directTestJob") as Job
        job.processingStep = step
        // run the Job
        scheduler.executeJob(job)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("Parameter abcd is either not defined for JobDefintion ${jobDefinition.id} or not of type Output.".toString(), updates[3].error.errorMessage)
        // verify that the test works if we use a proper parameter type
        test.parameterUsage = ParameterUsage.OUTPUT
        assertNotNull(test.save())
        process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
        )
        assertNotNull(update.save(flush: true))
        job = grailsApplication.mainContext.getBean("directTestJob") as Job
        job.processingStep = step
        // run the Job
        scheduler.executeJob(job)
        assertEquals(3, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
    }

    static void assertFailed(final Job job, final String expectedErrorMessage, final boolean twice = false) {
        final ProcessingStep step = job.processingStep
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assert updates.size() == twice ? 4 : 3
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FAILURE, updates[2].state)
        assertNotNull(updates[2].error)
        assert updates[2].error.errorMessage.contains(expectedErrorMessage)
        if (twice) {
            // We get the failure twice because the exception is caught and handled on the other thread and then rethrown
            // (from the Future.get() method), such that it is thrown from the job's execute() method as well.
            assertEquals(ExecutionState.FAILURE, updates[3].state)
            assertNotNull(updates[3].error)
            assert updates[3].error.errorMessage.contains(expectedErrorMessage)
        }
        assertTrue(step.process.finished)
    }

    static void assertSucceeded(final EndStateAwareJob job) {
        assertEquals(ExecutionState.SUCCESS, job.endState)
        final ProcessingStep step = job.processingStep
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assert updates.size() == 4
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
        assertTrue(step.process.finished)
    }
}
