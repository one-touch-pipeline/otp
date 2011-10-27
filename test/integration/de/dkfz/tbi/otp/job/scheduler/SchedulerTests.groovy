package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import org.junit.*

class SchedulerTests extends AbstractIntegrationTest {
    /**
     * Dependency Injection of Grails Application
     */
    def grailsApplication

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
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job job = grailsApplication.mainContext.getBean("testJob", step, [] as Set) as Job
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
        JobExecutionPlan jep = new JobExecutionPlan(name: "testEndStateAware", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestEndStateAwareJob("testEndStateAware", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job endStateAwareJob = grailsApplication.mainContext.getBean("testEndStateAwareJob", step, [] as Set) as Job
        // There is no Created ProcessingStep update - execution should fail
        shouldFail(RuntimeException) {
            endStateAwareJob.execute()
        }
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null
            )
        step.addToUpdates(update)
        assertNotNull(step.save(flush: true))
        shouldFail(InvalidStateException) {
            endStateAwareJob.getOutputParameters()
        }
        endStateAwareJob.execute()
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(4, step.updates.size())
        List<ProcessingStepUpdate> updates = step.updates.toList().sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
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
    void testFailingExecution() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "failingTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        assertNotNull(step.save())
        Job job = grailsApplication.mainContext.getBean("failingTestJob", step, [] as Set) as Job
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

    @Test
    void testMissingOutputParameter() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // create a third parameter type for which the job does not create a parameter
        ParameterType type = new ParameterType(name: "fail", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.OUTPUT)
        assertNotNull(type.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null
            )
        step.addToUpdates(update)
        assertNotNull(step.save(flush: true))
        // run the Job
        Job job = grailsApplication.mainContext.getBean("testJob", step, [] as Set) as Job
        job.execute()
        assertEquals(4, step.updates.size())
        List<ProcessingStepUpdate> updates = step.updates.toList().sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("Required Output Parameter of type ${type.id} is not set.".toString(), updates[3].error.errorMessage)
    }

    @Test
    void testInputAsOutputParameter() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "directTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.INPUT)
        assertNotNull(test.save())
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null
            )
        step.addToUpdates(update)
        assertNotNull(step.save(flush: true))
        Job job = grailsApplication.mainContext.getBean("directTestJob", step, [] as Set) as Job
        // run the Job
        job.execute()
        assertEquals(4, step.updates.size())
        List<ProcessingStepUpdate> updates = step.updates.toList().sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("Parameter abcd is either not defined for JobDefintion ${jobDefinition.id} or not of type Output.".toString(), updates[3].error.errorMessage)
        // verify that the test works if we use a proper parameter type
        test.usage = ParameterUsage.OUTPUT
        assertNotNull(test.save())
        process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null
            )
        step.addToUpdates(update)
        assertNotNull(step.save(flush: true))
        job = grailsApplication.mainContext.getBean("directTestJob", step, [] as Set) as Job
        // run the Job
        job.execute()
        assertEquals(3, step.updates.size())
        updates = step.updates.toList().sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
    }
}
