package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import org.junit.*

class SchedulerServiceTests {
    /**
     * Dependency Injection of grailsApplication
     */
    def grailsApplication
    /**
     * Dependency Injection of schedulerService
     */
    def schedulerService

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
        schedulerService.queue.clear()
        schedulerService.running.clear()
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testEndOfProcess() {
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // create the JobExecutionPlan
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
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null
            )
        step.addToUpdates(update)
        assertNotNull(step.save(flush: true))

        // there is no further Job in the Job Execution Plan. Executing should finish the Process
        assertFalse(process.finished)
        assertNull(step.jobClass)
        assertNull(step.jobVersion)
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        assertTrue(process.finished)
        assertNotNull(step.jobClass)
        assertEquals(de.dkfz.tbi.otp.job.jobs.TestJob.toString(), step.jobClass)
        assertNotNull(step.jobVersion)
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
    }
    
    @Test
    void testCompleteProcess() {
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "testJob", plan: jep)
        jep.addToJobDefinitions(jobDefinition)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "testJob", plan: jep, previous: jobDefinition)
        jep.addToJobDefinitions(jobDefinition2)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
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
        process.addToProcessingSteps(step)
        assertNotNull(process.save(flush: true))
        // first Job should be run
        assertFalse(process.finished)
        assertNull(step.jobClass)
        assertNull(step.jobVersion)
        assertNull(step.next)
        assertNull(step.previous)
        assertEquals(1, process.processingSteps.size())
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        assertFalse(process.finished)
        assertNotNull(step.jobClass)
        assertEquals(de.dkfz.tbi.otp.job.jobs.TestJob.toString(), step.jobClass)
        assertNotNull(step.jobVersion)
        assertNotNull(step.next)
        assertNull(step.previous)
        assertFalse(step.id == step.next.id)
        assertSame(step, step.next.previous)
        assertEquals(2, process.processingSteps.size())
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        
        // after running the second job the Process should be finished
        schedulerService.schedule()
        assertTrue(process.finished)
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(2, process.processingSteps.size())
        process.processingSteps.each { ProcessingStep s ->
            assertEquals(3, s.updates.size())
            List<ProcessingStepUpdate> updates = s.updates.toList().sort { it.id }
            assertEquals(ExecutionState.CREATED, updates[0].state)
            assertEquals(ExecutionState.STARTED, updates[1].state)
            assertEquals(ExecutionState.FINISHED, updates[2].state)
        }
    }

    @Test
    void testConstantParameterPassing() {
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // create the JobExecutionPlan with three Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "testJob", plan: jep)
        jep.addToJobDefinitions(jobDefinition)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // second Job Definition
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "testJob", plan: jep, previous: jobDefinition)
        jep.addToJobDefinitions(jobDefinition2)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // third Job Definition
        JobDefinition jobDefinition3 = new JobDefinition(name: "test3", bean: "testJob", plan: jep, previous: jobDefinition2)
        jep.addToJobDefinitions(jobDefinition3)
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save(flush: true))
        // third JobDefinition gets two constant parameters
        ParameterType constantParameterType = new ParameterType(jobDefinition: jobDefinition3, name: "constant", description: "test", usage: ParameterUsage.INPUT)
        assertNotNull(constantParameterType.save())
        jobDefinition3.addToConstantParameters(new Parameter(type: constantParameterType, value: "constant1"))
        jobDefinition3.addToConstantParameters(new Parameter(type: constantParameterType, value: "constant2"))
        assertNotNull(jobDefinition3.save(flush: true))
        // create the Process
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
        process.addToProcessingSteps(step)
        assertNotNull(process.save(flush: true))

        // running the JobExecutionPlan
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // there should be a processing step for jobDefinition2 with two input parameters generated by the testJob
        ProcessingStep step2 = ProcessingStep.findByJobDefinition(jobDefinition2)
        assertNotNull(step2)
        assertSame(step, step2.previous)
        assertSame(step2, step.next)
        List<Parameter> parameters = step2.input.toList().sort{ it.type.id }
        assertEquals(2, parameters.size())
        assertEquals("test", parameters[0].type.name)
        assertEquals("test2", parameters[1].type.name)
        assertEquals("1234", parameters[0].value)
        assertEquals("4321", parameters[1].value)
        // continue
        schedulerService.schedule()
        // the third Job should be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // there should be a processing step for jobDefinition3 with two input parameters generated by the testJob
        // and two constant parameters
        ProcessingStep step3 = ProcessingStep.findByJobDefinition(jobDefinition3)
        assertNotNull(step3)
        assertSame(step2, step3.previous)
        assertSame(step3, step2.next)
        assertNull(step3.next)
        parameters = step3.input.toList().sort{ it.value }
        assertEquals(4, parameters.size())
        assertEquals("test", parameters[0].type.name)
        assertEquals("test2", parameters[1].type.name)
        assertSame(constantParameterType, parameters[2].type)
        assertSame(constantParameterType, parameters[3].type)
        assertEquals("1234", parameters[0].value)
        assertEquals("4321", parameters[1].value)
        assertEquals("constant1", parameters[2].value)
        assertEquals("constant2", parameters[3].value)
        // there should not have been a parameter created for the constant types
        assertEquals(2, Parameter.countByType(constantParameterType))
        // continue
        schedulerService.schedule()
        // the third Job should be scheduled
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertTrue(step3.process.finished)

        // run another process for same JobExecutionPlan, but trigger an exception by using an OUTPUT parameter as constant
        constantParameterType.refresh()
        constantParameterType.usage = ParameterUsage.OUTPUT
        assertNotNull(constantParameterType.save(flush: true))
        // create Process
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
        process.addToProcessingSteps(step)
        assertNotNull(process.save(flush: true))
        // running the JobExecutionPlan
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        schedulerService.schedule()
        // mapping the parameters from the second Job should have failed
        shouldFail(RuntimeException) {
            schedulerService.schedule()
        }
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertFalse(process.finished)
        // the last JobDefinition should have a ProcessingStep with a created and a failed update
        ProcessingStep failedStep = ProcessingStep.findAllByJobDefinition(jobDefinition3).toList().sort { it.id }.last()
        assertEquals(2, failedStep.updates.size())
        assertEquals(ExecutionState.CREATED, failedStep.updates.toList().sort { it.id }.first().state )
        assertEquals(ExecutionState.FAILURE, failedStep.updates.toList().sort { it.id }.last().state )
    }
}
