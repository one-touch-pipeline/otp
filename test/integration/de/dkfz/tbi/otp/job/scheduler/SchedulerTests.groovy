package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import org.junit.*

class SchedulerTests {
    /**
     * Dependency Injection of Grails Application
     */
    def grailsApplication

    def shouldFail = { exception, code ->
        try {
            code.call()
            fail("Exception of type ${exception} was expected")
        } catch (Exception e) {
            if (!exception.isAssignableFrom(e.class)) {
                fail("Exception of type ${exception} expected but got ${e.class}")
            }
        }
    }

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testNormalJobExecution() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "testJob", plan: jep)
        jep.addToJobDefinitions(jobDefinition)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job job = grailsApplication.mainContext.getBean("testJob", step, []) as Job
        // There is no Created ProcessingStep update - execution should fail
        shouldFail(RuntimeException) {
            job.execute()
        }
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null
            )
        step.addToUpdates(update)
        assertNotNull(step.save(flush: true))
        shouldFail(InvalidStateException) {
            job.getOutputParameters()
        }
        job.execute()
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(3, step.updates.size())
        List<ProcessingStepUpdate> updates = step.updates.toList().sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        // and there should be some output parameters
        List<Parameter> params = step.output.toList().sort { it.key }
        assertEquals(2, params.size())
        assertEquals("test", params[0].key)
        assertEquals("1234", params[0].value)
        assertFalse(params[0].referencesDomainClass)
        assertEquals("test2", params[1].key)
        assertEquals("4321", params[1].value)
        assertTrue(params[1].referencesDomainClass)
    }

    @Test
    void testFailingExecution() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "failingTestJob", plan: jep)
        jep.addToJobDefinitions(jobDefinition)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        assertNotNull(step.save())
        Job job = grailsApplication.mainContext.getBean("failingTestJob", step, []) as Job
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null
            )
        step.addToUpdates(update)
        assertNotNull(step.save(flush: true))
        shouldFail(Exception) {
            job.execute()
        }
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(3, step.updates.size())
        List<ProcessingStepUpdate> updates = step.updates.toList().sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FAILURE, updates[2].state)
        assertNotNull(updates[2].error)
        assertEquals("Testing", updates[2].error.errorMessage)
    }
}
