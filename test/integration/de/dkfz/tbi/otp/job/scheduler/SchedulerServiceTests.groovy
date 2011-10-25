package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterMapping
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.StartJob
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import org.junit.*

class SchedulerServiceTests extends AbstractIntegrationTest {
    /**
     * Dependency Injection of grailsApplication
     */
    def grailsApplication
    /**
     * Dependency Injection of schedulerService
     */
    def schedulerService

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
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
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
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        jobDefinition.next = createTestJob("test2", jep, jobDefinition)
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
        // first Job should be run
        assertFalse(process.finished)
        assertNull(step.jobClass)
        assertNull(step.jobVersion)
        assertNull(step.next)
        assertNull(step.previous)
        assertEquals(1, ProcessingStep.countByProcess(process))
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
        assertEquals(2, ProcessingStep.countByProcess(process))
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        
        // after running the second job the Process should be finished
        schedulerService.schedule()
        // TODO: why is that needed?
        process = Process.get(process.id)
        assertTrue(process.finished)
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(2, ProcessingStep.countByProcess(process))
        ProcessingStep.findAllByProcess(process).each { ProcessingStep s ->
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
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // second Job Definition
        JobDefinition jobDefinition2 = createTestJob("test2", jep, jobDefinition)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // third Job Definition
        JobDefinition jobDefinition3 = createTestJob("test3", jep, jobDefinition2)
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
        assertNull(step2.input)
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
        List<Parameter> parameters = step3.input.toList().sort{ it.value }
        assertEquals(2, parameters.size())
        assertSame(constantParameterType, parameters[0].type)
        assertSame(constantParameterType, parameters[1].type)
        assertEquals("constant1", parameters[0].value)
        assertEquals("constant2", parameters[1].value)
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

    @Test
    void testParameterMapping() {
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // create the JobExecutionPlan with three Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // second Job Definition
        JobDefinition jobDefinition2 = createTestJob("test2", jep, jobDefinition)
        ParameterMapping mapping = new ParameterMapping(job: jobDefinition2, from: ParameterType.findByJobDefinitionAndName(jobDefinition, "test"), to: ParameterType.findByJobDefinitionAndName(jobDefinition2, "test"))
        jobDefinition2.addToParameterMappings(mapping)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save())
        // third Job Definition
        JobDefinition jobDefinition3 = createTestJob("test3", jep, jobDefinition2)
        mapping = new ParameterMapping(job: jobDefinition3, from: ParameterType.findByJobDefinitionAndName(jobDefinition2, "test"), to: ParameterType.findByJobDefinitionAndName(jobDefinition3, "test"))
        jobDefinition3.addToParameterMappings(mapping)
        mapping = new ParameterMapping(job: jobDefinition3, from: ParameterType.findByJobDefinitionAndName(jobDefinition2, "test2"), to: ParameterType.findByJobDefinitionAndName(jobDefinition3, "test2"))
        jobDefinition3.addToParameterMappings(mapping)
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save(flush: true))
        // third JobDefinition gets two constant parameters
        ParameterType constantParameterType = new ParameterType(jobDefinition: jobDefinition3, name: "constant", description: "test", usage: ParameterUsage.INPUT)
        assertNotNull(constantParameterType.save())
        jobDefinition3.addToConstantParameters(new Parameter(type: constantParameterType, value: "constant1"))
        assertNotNull(jobDefinition3.save())
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

        // running the JobExecutionPlan
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // there should be a processing step for jobDefinition2 with one input parameters generated by the testJob
        ProcessingStep step2 = ProcessingStep.findByJobDefinition(jobDefinition2)
        assertNotNull(step2)
        assertSame(step, step2.previous)
        assertSame(step2, step.next)
        assertNotNull(step2.input)
        assertEquals(1, step2.input.size())
        assertEquals("1234", step2.input.toList().first().value)
        // schedule the second Job
        schedulerService.schedule()
        // there should be a processing step for the third job
        ProcessingStep step3 = ProcessingStep.findByJobDefinition(jobDefinition3)
        assertNotNull(step3)
        assertSame(step2, step3.previous)
        // constant parameters and passed parameters
        assertNotNull(step3.input)
        List<Parameter> params = step3.input.toList().sort{ it.value }
        assertEquals(4, params.size())
        assertEquals("1234", params[0].value)
        assertEquals("4321", params[1].value)
        assertEquals("constant1", params[2].value)
        assertEquals("constant2", params[3].value)
        // schedule the last job
        schedulerService.schedule()
        assertTrue(process.finished)
    }

    @Test
    void testPassthroughParameters() {
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // second Job Definition
        JobDefinition jobDefinition2 = createTestJob("test2", jep, jobDefinition)
        ParameterType passThrough = new ParameterType(jobDefinition: jobDefinition2, name: "passthrough", description: "test", usage: ParameterUsage.PASSTHROUGH)
        assertNotNull(passThrough.save())
        ParameterMapping mapping = new ParameterMapping(job: jobDefinition2, from: ParameterType.findByJobDefinitionAndName(jobDefinition, "test"), to: passThrough)
        jobDefinition2.addToParameterMappings(mapping)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save())
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

        // running the JobExecutionPlan
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertTrue(schedulerService.queue.add(step))
        assertNull(Parameter.findByType(passThrough))
        schedulerService.schedule()
        Parameter passThroughParameter = Parameter.findByType(passThrough)
        assertNotNull(passThroughParameter)
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // there should be a processing step for jobDefinition2 with no input parameter
        ProcessingStep step2 = ProcessingStep.findByJobDefinition(jobDefinition2)
        assertNotNull(step2)
        assertSame(step, step2.previous)
        assertSame(step2, step.next)
        assertNull(step2.input)
        // schedule the next Job
        schedulerService.schedule()
        // no Job should be be scheduled
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // there should be three output parameters
        assertNotNull(step2.output)
        List<Parameter> params = step2.output.toList().sort{ it.value }
        assertEquals(3, params.size())
        assertEquals("1234", params[0].value)
        assertEquals("1234", params[1].value)
        assertEquals("4321", params[2].value)
        assertTrue(params.contains(passThroughParameter))
        // should be finished
        assertTrue(process.finished)
    }

    @Test
    void testCreateProcess() {
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // create the JobExecutionPlan with one Job Definition
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        // create the StartJobDefinition for the JobExecutionPlan
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        assertNotNull(startJob.save())
        jep.startJob = startJob
        assertNotNull(jep.save())
        // create first job definition
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        assertNotNull(jep.save(flush: true))
        StartJob job = grailsApplication.mainContext.getBean("testStartJob", jep) as StartJob
        assertNotNull(job)
        assertEquals(0, Process.count())
        assertEquals(0, ProcessingStep.count())
        schedulerService.createProcess(job, [])
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(1, Process.count())
        assertEquals(1, ProcessingStep.count())
        Process process = Process.findByJobExecutionPlan(jep)
        assertNotNull(process)
        assertSame(schedulerService.queue.first().jobDefinition, jobDefinition)
        assertFalse(process.finished)
        // running the job should work
        schedulerService.schedule()
        assertTrue(process.finished)
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
    }

    @Test
    void testCreateProcessWithParameters() {
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // create the JobExecutionPlan with one Job Definition
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        // create the StartJobDefinition for the JobExecutionPlan
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        assertNotNull(startJob.save())
        jep.startJob = startJob
        assertNotNull(jep.save())
        // create first job definition
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // create some Parameter Types
        ParameterType type1 = new ParameterType(name: "test", description: "StartJob Parameter", jobDefinition: startJob, usage: ParameterUsage.OUTPUT)
        assertNotNull(type1.save())
        ParameterType type2 = new ParameterType(name: "test2", description: "StartJob Parameter", jobDefinition: startJob, usage: ParameterUsage.OUTPUT)
        assertNotNull(type2.save())
        ParameterType passthrough = new ParameterType(name: "passthrough", description: "Job Passthrough Parameter", jobDefinition: jobDefinition, usage: ParameterUsage.PASSTHROUGH)
        assertNotNull(passthrough.save())
        ParameterType input = new ParameterType(name: "input", description: "Input Parameter", jobDefinition: jobDefinition, usage: ParameterUsage.INPUT)
        assertNotNull(input.save())
        // is StartJobDefinition a Problem for ParameterType?
        assertSame(startJob, type1.jobDefinition)
        assertSame(startJob, type2.jobDefinition)
        assertSame(jobDefinition, passthrough.jobDefinition)
        assertSame(jobDefinition, input.jobDefinition)
        // create the ParameterMapping
        ParameterMapping mapping1 = new ParameterMapping(from: type1, to: passthrough, jobDefinition: jobDefinition)
        ParameterMapping mapping2 = new ParameterMapping(from: type2, to: input, jobDefinition: jobDefinition)
        jobDefinition.addToParameterMappings(mapping1)
        jobDefinition.addToParameterMappings(mapping2)
        assertNotNull(jobDefinition.save(flush: true))
        // get the startjob
        StartJob job = grailsApplication.mainContext.getBean("testStartJob", jep) as StartJob
        assertNotNull(job)
        assertEquals(0, Process.count())
        assertEquals(0, ProcessingStep.count())
        schedulerService.createProcess(job, [new Parameter(value: "1234", type: type1), new Parameter(value: "abcd", type: type2)])
        // verify that the Process is created
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(1, Process.count())
        assertEquals(1, ProcessingStep.count())
        Process process = Process.findByJobExecutionPlan(jep)
        assertNotNull(process)
        assertSame(schedulerService.queue.first().jobDefinition, jobDefinition)
        assertFalse(process.finished)
        // first processing step should have one input parameter and one output parameter
        ProcessingStep step = schedulerService.queue.first()
        assertEquals(1, step.input.size())
        assertEquals(1, step.output.size())
        assertSame(passthrough, step.output.toList()[0].type)
        assertSame(input, step.input.toList()[0].type)
        // running the Job should create more output params
        schedulerService.schedule()
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(3, step.output.size())
    }
}
