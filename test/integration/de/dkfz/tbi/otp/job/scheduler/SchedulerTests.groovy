package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
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
        ProcessingStep step = new ProcessingStep()
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
        job.execute()
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(3, step.updates.size())
        List<ProcessingStepUpdate> updates = step.updates.toList().sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
    }

    @Test
    void testFailingExecution() {
        ProcessingStep step = new ProcessingStep()
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
