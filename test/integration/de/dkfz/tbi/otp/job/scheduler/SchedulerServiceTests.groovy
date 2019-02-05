package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.integration.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.restarting.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.*
import org.junit.*

import java.util.concurrent.*

import static org.junit.Assert.*

class SchedulerServiceTests extends AbstractIntegrationTest {

    def grailsApplication

    RestartCheckerService restartCheckerService
    SchedulerService schedulerService

    Scheduler scheduler

    boolean originalSchedulerActive
    boolean originalStartupOk

    @Before
    void setUp() {
        originalSchedulerActive = schedulerService.schedulerActive
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth(ADMIN) {
            originalStartupOk = schedulerService.startupOk
        }
        schedulerService.schedulerActive = true
        schedulerService.startupOk = true
        schedulerService.queue.clear()
        schedulerService.running.clear()
        restartCheckerService.metaClass.canWorkflowBeRestarted = { ProcessingStep step -> false }
    }

    @After
    void tearDown() {
        schedulerService.schedulerActive = originalSchedulerActive
        schedulerService.startupOk = originalStartupOk
        TestCase.removeMetaClass(RestartCheckerService, restartCheckerService)
    }


    @Test
    void testEndOfProcess() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
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

        // there is no further Job in the Job Execution Plan. Executing should finish the Process
        assertFalse(process.finished)
        assertNull(step.jobClass)
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        assertTrue(process.finished)
        assertNotNull(step.jobClass)
        assertEquals(TestEndStateAwareJob.class.name, step.jobClass)
        assertQueueAndRunningToBeEmpty()
    }

    @Test
    void testCompleteProcess() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
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
        // first Job should be run
        assertFalse(process.finished)
        assertNull(step.jobClass)
        assertNull(step.next)
        assertNull(step.previous)
        assertEquals(1, ProcessingStep.countByProcess(process))
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        assertFalse(process.finished)
        assertNotNull(step.jobClass)
        assertEquals(de.dkfz.tbi.otp.job.jobs.TestJob.class.name, step.jobClass)
        assertNotNull(step.next)
        assertNull(step.previous)
        assertFalse(step.id == step.next.id)
        assertSame(step, step.next.previous)
        assertEquals(2, ProcessingStep.countByProcess(process))
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(3, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)

        // after running the second job the Process should be finished
        schedulerService.schedule()
        // TODO: why is that needed?
        process = Process.get(process.id)
        assertTrue(process.finished)
        assertQueueAndRunningToBeEmpty()
        assertEquals(2, ProcessingStep.countByProcess(process))

        step = ProcessingStep.findByProcessAndPrevious(process, step)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
    }

    @Test
    void testConstantParameterPassing() {
        assertQueueAndRunningToBeEmpty()
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
        JobDefinition jobDefinition3 = createTestEndStateAwareJob("test3", jep, jobDefinition2)
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save(flush: true))
        // third JobDefinition gets two constant parameters
        ParameterType constantParameterType = new ParameterType(jobDefinition: jobDefinition3, name: "constant", description: "test", parameterUsage: ParameterUsage.INPUT)
        ParameterType constantParameterType2 = new ParameterType(jobDefinition: jobDefinition3, name: "constant2", description: "test", parameterUsage: ParameterUsage.INPUT)
        assertNotNull(constantParameterType.save())
        assertNotNull(constantParameterType2.save())
        jobDefinition3.addToConstantParameters(new Parameter(type: constantParameterType, value: "constant1"))
        jobDefinition3.addToConstantParameters(new Parameter(type: constantParameterType2, value: "constant2"))
        assertNotNull(jobDefinition3.save(flush: true))
        // create the Process
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

        // running the JobExecutionPlan
        assertQueueAndRunningToBeEmpty()
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
        List<Parameter> parameters = step3.input.toList().sort { it.value }
        assertEquals(2, parameters.size())
        assertSame(constantParameterType, parameters[0].type)
        assertSame(constantParameterType2, parameters[1].type)
        assertEquals("constant1", parameters[0].value)
        assertEquals("constant2", parameters[1].value)
        // there should not have been a parameter created for the constant types
        assertEquals(1, Parameter.countByType(constantParameterType))
        assertEquals(1, Parameter.countByType(constantParameterType2))
        // continue
        schedulerService.schedule()
        // the third Job should be scheduled
        assertQueueAndRunningToBeEmpty()
        assertTrue(step3.process.finished)

        // run another process for same JobExecutionPlan, but trigger an exception by using an OUTPUT parameter as constant
        constantParameterType.refresh()
        constantParameterType.parameterUsage = ParameterUsage.OUTPUT
        assertNotNull(constantParameterType.save(flush: true))
        // create Process
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
        // running the JobExecutionPlan
        assertQueueAndRunningToBeEmpty()
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        schedulerService.schedule()
        // mapping the parameters from the second Job should have failed
        assert shouldFail(ExecutionException, {
            schedulerService.schedule()
        }).contains('SchedulerException')
        assertQueueAndRunningToBeEmpty()
        assertFalse(process.finished)
        // the last JobDefinition should have a ProcessingStep with a created and a failed update
        ProcessingStep failedStep = ProcessingStep.findAllByJobDefinition(jobDefinition3).toList().sort { it.id }.last()
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(failedStep)
        assertEquals(2, updates.size())
        assertEquals(ExecutionState.CREATED, updates.sort { it.id }.first().state)
        assertEquals(ExecutionState.FAILURE, updates.sort { it.id }.last().state)
    }

    @Test
    void testParameterMapping() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with three Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // second Job Definition
        JobDefinition jobDefinition2 = createTestJob("test2", jep, jobDefinition)
        ParameterMapping mapping = new ParameterMapping(job: jobDefinition2, from: ParameterType.findByJobDefinitionAndName(jobDefinition, "test"), to: ParameterType.findByJobDefinitionAndName(jobDefinition2, "input"))
        jobDefinition2.addToParameterMappings(mapping)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save())
        // third Job Definition
        JobDefinition jobDefinition3 = createTestEndStateAwareJob("test3", jep, jobDefinition2)
        mapping = new ParameterMapping(job: jobDefinition3, from: ParameterType.findByJobDefinitionAndName(jobDefinition2, "test"), to: ParameterType.findByJobDefinitionAndName(jobDefinition3, "input"))
        jobDefinition3.addToParameterMappings(mapping)
        mapping = new ParameterMapping(job: jobDefinition3, from: ParameterType.findByJobDefinitionAndName(jobDefinition2, "test2"), to: ParameterType.findByJobDefinitionAndName(jobDefinition3, "input2"))
        jobDefinition3.addToParameterMappings(mapping)
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save(flush: true))
        // third JobDefinition gets two constant parameters
        ParameterType constantParameterType = new ParameterType(jobDefinition: jobDefinition3, name: "constant", description: "test", parameterUsage: ParameterUsage.INPUT)
        ParameterType constantParameterType2 = new ParameterType(jobDefinition: jobDefinition3, name: "constant2", description: "test", parameterUsage: ParameterUsage.INPUT)
        assertNotNull(constantParameterType.save())
        assertNotNull(constantParameterType2.save())
        jobDefinition3.addToConstantParameters(new Parameter(type: constantParameterType, value: "constant1"))
        assertNotNull(jobDefinition3.save())
        jobDefinition3.addToConstantParameters(new Parameter(type: constantParameterType2, value: "constant2"))
        assertNotNull(jobDefinition3.save(flush: true))
        // create the Process
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

        // running the JobExecutionPlan
        assertQueueAndRunningToBeEmpty()
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
        List<Parameter> params = step3.input.toList().sort { it.value }
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
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // second Job Definition
        JobDefinition jobDefinition2 = createTestEndStateAwareJob("test2", jep, jobDefinition)
        ParameterType passThrough = new ParameterType(jobDefinition: jobDefinition2, name: "passthrough", description: "test", parameterUsage: ParameterUsage.PASSTHROUGH)
        assertNotNull(passThrough.save())
        ParameterMapping mapping = new ParameterMapping(job: jobDefinition2, from: ParameterType.findByJobDefinitionAndName(jobDefinition, "test"), to: passThrough)
        jobDefinition2.addToParameterMappings(mapping)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save())
        // create the Process
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

        // running the JobExecutionPlan
        assertQueueAndRunningToBeEmpty()
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
        assertQueueAndRunningToBeEmpty()
        // there should be three output parameters
        assertNotNull(step2.output)
        List<Parameter> params = step2.output.toList().sort { it.value }
        assertEquals(3, params.size())
        assertEquals("1234", params[0].value)
        assertEquals("1234", params[1].value)
        assertEquals("4321", params[2].value)
        assertTrue(params.contains(passThroughParameter))
        // should be finished
        assertTrue(process.finished)
    }

    /**
     * Test that one output parameter can be mapped into multiple input parameters
     * and passthrough parameters of the next Job.
     */
    @Test
    void testOneToManyParameterMapping() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // second Job Definition
        JobDefinition jobDefinition2 = createTestEndStateAwareJob("test2", jep, jobDefinition)
        ParameterType passThrough = new ParameterType(jobDefinition: jobDefinition2, name: "passthrough", description: "test", parameterUsage: ParameterUsage.PASSTHROUGH)
        assertNotNull(passThrough.save())
        ParameterType passThrough2 = new ParameterType(jobDefinition: jobDefinition2, name: "passthrough2", description: "test", parameterUsage: ParameterUsage.PASSTHROUGH)
        assertNotNull(passThrough2.save())
        // map output parameter test to our two passthrough parameters
        ParameterMapping mapping = new ParameterMapping(job: jobDefinition2, from: ParameterType.findByJobDefinitionAndName(jobDefinition, "test"), to: passThrough)
        jobDefinition2.addToParameterMappings(mapping)
        ParameterMapping mapping2 = new ParameterMapping(job: jobDefinition2, from: ParameterType.findByJobDefinitionAndName(jobDefinition, "test"), to: passThrough2)
        jobDefinition2.addToParameterMappings(mapping2)
        // pass the output parameter test also to both input parameters
        ParameterMapping mapping3 = new ParameterMapping(job: jobDefinition2, from: ParameterType.findByJobDefinitionAndName(jobDefinition, "test"),
                to: ParameterType.findByJobDefinitionAndName(jobDefinition2, "input"))
        jobDefinition2.addToParameterMappings(mapping3)
        ParameterMapping mapping4 = new ParameterMapping(job: jobDefinition2, from: ParameterType.findByJobDefinitionAndName(jobDefinition, "test"),
                to: ParameterType.findByJobDefinitionAndName(jobDefinition2, "input2"))
        jobDefinition2.addToParameterMappings(mapping4)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save())
        // create the Process
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
        // running the JobExecutionPlan
        assertQueueAndRunningToBeEmpty()
        assertTrue(schedulerService.queue.add(step))
        assertNull(Parameter.findByType(passThrough))
        schedulerService.schedule()
        Parameter passThroughParameter = Parameter.findByType(passThrough)
        assertNotNull(passThroughParameter)
        Parameter passThroughParameter2 = Parameter.findByType(passThrough2)
        assertNotNull(passThroughParameter2)
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // there should be a processing step for jobDefinition2 with two input parameter
        ProcessingStep step2 = ProcessingStep.findByJobDefinition(jobDefinition2)
        assertNotNull(step2)
        assertSame(step, step2.previous)
        assertSame(step2, step.next)
        assertNotNull(step2.input)
        assertEquals(2, step2.input.size())
        // schedule the next Job
        schedulerService.schedule()
        // no Job should be be scheduled
        assertQueueAndRunningToBeEmpty()
        // there should be three output parameters
        assertNotNull(step2.output)
        List<Parameter> params = step2.output.toList().sort { it.value }
        assertEquals(4, params.size())
        assertEquals("1234", params[0].value)
        assertEquals("1234", params[1].value)
        assertEquals("1234", params[2].value)
        assertEquals("4321", params[3].value)
        assertTrue(params.contains(passThroughParameter))
        assertTrue(params.contains(passThroughParameter2))
        // should be finished
        assertTrue(process.finished)
    }

    @Test
    void testCreateProcess() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with one Job Definition
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        // create the StartJobDefinition for the JobExecutionPlan
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        assertNotNull(startJob.save())
        jep.startJob = startJob
        assertNotNull(jep.save())
        // create first job definition
        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        assertNotNull(jep.save(flush: true))
        StartJob job = grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep
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
        assertQueueAndRunningToBeEmpty()
    }

    @Test
    void testCreateProcessWithDisabledScheduler() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with one Job Definition
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        // create the StartJobDefinition for the JobExecutionPlan
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        assertNotNull(startJob.save())
        jep.startJob = startJob
        assertNotNull(jep.save())
        // create first job definition
        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        assertNotNull(jep.save(flush: true))
        StartJob job = grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep
        assertNotNull(job)
        try {
            SpringSecurityUtils.doWithAuth(ADMIN) {
                schedulerService.suspendScheduler()
            }
            TestCase.shouldFailWithMessage(RuntimeException, "Scheduler is disabled", {
                schedulerService.createProcess(job, [])
            })
        } finally {
            SpringSecurityUtils.doWithAuth(ADMIN) {
                schedulerService.resumeScheduler()
            }
        }
    }

    @Test
    void testCreateProcessWithParameters() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with one Job Definition
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        // create the StartJobDefinition for the JobExecutionPlan
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        assertNotNull(startJob.save())
        jep.startJob = startJob
        assertNotNull(jep.save())
        // create first job definition
        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // create some Parameter Types
        ParameterType type1 = new ParameterType(name: "test", description: "StartJob Parameter", jobDefinition: startJob, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(type1.save())
        ParameterType type2 = new ParameterType(name: "test2", description: "StartJob Parameter", jobDefinition: startJob, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(type2.save())
        ParameterType passthrough = new ParameterType(name: "passthrough", description: "Job Passthrough Parameter", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.PASSTHROUGH)
        assertNotNull(passthrough.save())
        ParameterType input = new ParameterType(name: "input3", description: "Input Parameter", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
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
        StartJob job = grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep
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
        assertQueueAndRunningToBeEmpty()
        assertEquals(3, step.output.size())
    }

    @Test
    void testCreateProcessWithProcessParameter() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with one Job Definition
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        // create the StartJobDefinition for the JobExecutionPlan
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        assertNotNull(startJob.save())
        jep.startJob = startJob
        assertNotNull(jep.save())
        // create first job definition
        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // create some Parameter Types
        ParameterType type1 = new ParameterType(name: "test", description: "StartJob Parameter", jobDefinition: startJob, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(type1.save())
        ParameterType type2 = new ParameterType(name: "test2", description: "StartJob Parameter", jobDefinition: startJob, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(type2.save())
        ParameterType passthrough = new ParameterType(name: "passthrough", description: "Job Passthrough Parameter", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.PASSTHROUGH)
        assertNotNull(passthrough.save())
        ParameterType input = new ParameterType(name: "input3", description: "Input Parameter", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
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
        StartJob job = grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep
        assertNotNull(job)
        assertEquals(0, Process.count())
        assertEquals(0, ProcessingStep.count())
        // create ProcessParameter for handing over as additional parameter
        ProcessParameter processParameter = new ProcessParameter(className: SeqTrack.name, value: "test")
        // test for process parameter
        schedulerService.createProcess(job, [], processParameter)
        // verify that the Process is created
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(1, Process.count())
        assertEquals(1, ProcessingStep.count())
        Process process = Process.findByJobExecutionPlan(jep)
        assertNotNull(process)
        assertSame(schedulerService.queue.first().jobDefinition, jobDefinition)
        assertFalse(process.finished)
        schedulerService.schedule()
        assertQueueAndRunningToBeEmpty()
        // process parameter should be accessible
        assertEquals("test", ProcessParameter.findByProcess(process).value)
    }

    @Test
    void testDecisions() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with one Job Definition
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        // create the StartJobDefinition for the JobExecutionPlan
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        assertNotNull(startJob.save())
        jep.startJob = startJob
        assertNotNull(jep.save())
        DecidingJobDefinition decidingJobDefinition = new DecidingJobDefinition(name: "test", bean: "decisionTestJob", plan: jep)
        assertNotNull(decidingJobDefinition.save())
        // decisions for the job
        JobDecision decision1 = new JobDecision(jobDefinition: decidingJobDefinition, name: "outcome1", description: "test")
        JobDecision decision2 = new JobDecision(jobDefinition: decidingJobDefinition, name: "outcome2", description: "test")
        assertNotNull(decision1.save())
        assertNotNull(decision2.save())
        // create the JobDefinitions after the decision
        JobDefinition jobDefinition1 = createTestEndStateAwareJob("decision1", jep, decidingJobDefinition)
        JobDefinition jobDefinition2 = new JobDefinition(name: "decision2", bean: "directTestJob", plan: jep, previous: decidingJobDefinition)
        assertNotNull(jobDefinition1.save())
        assertNotNull(jobDefinition2.save())
        decidingJobDefinition.next = null
        assertNotNull(decidingJobDefinition.save())
        assertNull(decidingJobDefinition.next)
        assertNotNull(jobDefinition1.previous)
        assertNotNull(jobDefinition2.previous)
        DecisionMapping mapping1 = new DecisionMapping(decision: decision1, definition: jobDefinition1)
        DecisionMapping mapping2 = new DecisionMapping(decision: decision2, definition: jobDefinition2)
        assertNotNull(mapping1.save())
        assertNotNull(mapping2.save())
        assertNotNull(decidingJobDefinition.save())
        // set in the job execution plan
        jep.firstJob = decidingJobDefinition
        assertNotNull(jep.save(flush: true))
        // get the startjob
        StartJob job = grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep
        assertNotNull(job)
        assertEquals(0, Process.count())
        assertEquals(0, ProcessingStep.count())
        schedulerService.createProcess(job, [])
        // verify that the Process is created
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(1, Process.count())
        assertEquals(1, ProcessingStep.count())
        assertEquals(1, DecisionProcessingStep.count())
        ProcessingStep step = ProcessingStep.list().first()
        assertTrue(step instanceof DecisionProcessingStep)
        assertNull((step as DecisionProcessingStep).decision)
        Process process = Process.findByJobExecutionPlan(jep)
        assertNotNull(process)
        assertSame(schedulerService.queue.first().jobDefinition, decidingJobDefinition)
        // running the Job should queue another one
        schedulerService.schedule()
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        assertEquals(1, schedulerService.queue.size())
        assertEquals(2, ProcessingStep.count())
        assertEquals(1, DecisionProcessingStep.count())
        // the decision job decided for first decision
        assertEquals(decision1, (step as DecisionProcessingStep).decision)
        assertEquals(jobDefinition1, schedulerService.queue.first().jobDefinition)
        assertFalse(Process.list().first().finished)
        // let the job run
        schedulerService.schedule()
        assertQueueAndRunningToBeEmpty()
        assertTrue(Process.list().first().finished)
    }

    @Test
    void testFailingEndOfProcess() {
        assertQueueAndRunningToBeEmpty()
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
        // get the startjob
        StartJob job = grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep
        assertNotNull(job)
        assertEquals(0, Process.count())
        assertEquals(0, ProcessingStep.count())
        schedulerService.createProcess(job, [])

        // schedule
        assert shouldFail(ExecutionException, {
            schedulerService.schedule()
        }).contains('SchedulerException')
    }

    @Test
    void testFailingValidation() {
        assertQueueAndRunningToBeEmpty()
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        ValidatingJobDefinition validator = new ValidatingJobDefinition(name: "validator", bean: "failingValidatingTestJob", validatorFor: jobDefinition, plan: jep)
        assertNotNull(validator.save())
        jobDefinition.next = validator
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = createTestEndStateAwareJob("testEndStateAware", jep)
        assertNotNull(jobDefinition2.save())
        validator.next = jobDefinition2
        assertNotNull(validator.save())
        // Create the Process
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
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        assertFalse(process.finished)
        // verify first job
        assertEquals(3, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // executing the validator should fail the process
        schedulerService.schedule()
        // process should be finished and the step should be set to failure
        assertTrue(process.finished)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        // no further jobs should be scheduled
        assertQueueAndRunningToBeEmpty()
        // the validating job itself should be succeeded
        step = ProcessingStep.list().last()
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
    }

    @Test
    void testSuccessfulValidation() {
        assertQueueAndRunningToBeEmpty()
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        ValidatingJobDefinition validator = new ValidatingJobDefinition(name: "validator", bean: "validatingTestJob", validatorFor: jobDefinition, plan: jep)
        assertNotNull(validator.save())
        jobDefinition.next = validator
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = createTestEndStateAwareJob("testEndStateAware", jep)
        assertNotNull(jobDefinition2.save())
        validator.next = jobDefinition2
        assertNotNull(validator.save())
        // Create the Process
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
        assertTrue(schedulerService.queue.add(step))
        schedulerService.schedule()
        assertFalse(process.finished)
        // verify first job
        assertEquals(3, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        // another Job should be be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // executing the validator should fail the process
        schedulerService.schedule()
        // process should not yet be finished and the step should be set to success
        assertFalse(process.finished)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
        // the validating job itself should be succeeded
        step = ProcessingStep.findByJobDefinition(validator)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
        // another job should be scheduled
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // this should end the process
        schedulerService.schedule()
        assertTrue(process.finished)
        assertQueueAndRunningToBeEmpty()
    }

    @Test
    void testRestartProcessingStepInCorrectState() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        process.finished = true
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.CREATED,
                previous: null,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        // with a created event it should fail
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step)
        }
        // with a started event it should fail
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.STARTED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step)
        }
        // with a finished event it should fail
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FINISHED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step)
        }
        // with a success event it should fail
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.SUCCESS,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step)
        }
        // with a restarted event it should fail
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.RESTARTED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step)
        }
        // with a suspended event it should fail
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.SUSPENDED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step)
        }
        // with a resumed event it should fail
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.RESUMED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step)
        }

        // set to failed
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FINISHED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FAILURE,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        assertEquals(9, ProcessingStepUpdate.countByProcessingStep(step))
        // this one should succeed
        // do not queue
        schedulerService.restartProcessingStep(step, false)
        assertEquals(10, ProcessingStepUpdate.countByProcessingStep(step))
        assertEquals(ExecutionState.RESTARTED, ProcessingStepUpdate.findAllByProcessingStep(step).last().state)
        assertFalse(process.finished)
        assertQueueAndRunningToBeEmpty()
    }

    @Test
    void testRestartProcessingStepProcessFinished() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }
        assertNotNull(step.save())
        mockProcessingStepAsFailed(step)
        assertFalse(process.finished)
        // a not finished process should fail
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step, false)
        }
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        process.finished = true
        assertNotNull(process.save())
        assertTrue(process.finished)
        schedulerService.restartProcessingStep(step, false)
        assertEquals(5, ProcessingStepUpdate.countByProcessingStep(step))
        assertEquals(ExecutionState.RESTARTED, ProcessingStepUpdate.findAllByProcessingStep(step).last().state)
        assertFalse(process.finished)
        assertQueueAndRunningToBeEmpty()
    }

    @Test
    void testRestartProcessingStepHasUpdates() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        process.finished = true
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }
        assertNotNull(step.save())
        shouldFail(IncorrectProcessingException) {
            schedulerService.restartProcessingStep(step, false)
        }
        mockProcessingStepAsFailed(step)
        schedulerService.restartProcessingStep(step, false)
        assertEquals(5, ProcessingStepUpdate.countByProcessingStep(step))
        assertEquals(ExecutionState.RESTARTED, ProcessingStepUpdate.findAllByProcessingStep(step).last().state)
        assertFalse(process.finished)
        assertQueueAndRunningToBeEmpty()
    }

    @Test
    void testRestartProcessingStep() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        process.finished = true
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }
        assertNotNull(step.save())
        mockProcessingStepAsFailed(step)
        schedulerService.restartProcessingStep(step, false)
        assertEquals(5, ProcessingStepUpdate.countByProcessingStep(step))
        assertEquals(ExecutionState.RESTARTED, ProcessingStepUpdate.findAllByProcessingStep(step).last().state)
        assertFalse(process.finished)
        assertQueueAndRunningToBeEmpty()
        // now the same with schedule
        ProcessingStepUpdate update = ProcessingStepUpdate.findAllByProcessingStep(step).last()
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FINISHED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FAILURE,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        assertEquals(7, ProcessingStepUpdate.countByProcessingStep(step))
        process.finished = true
        assertNotNull(process.save())
        schedulerService.restartProcessingStep(step)
        assertEquals(8, ProcessingStepUpdate.countByProcessingStep(step))
        assertEquals(ExecutionState.RESTARTED, ProcessingStepUpdate.findAllByProcessingStep(step).last().state)
        assertFalse(process.finished)
        assertFalse(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
        // a restarted ProcessingStep should have been created
        RestartedProcessingStep restartedStep = RestartedProcessingStep.findAllByOriginal(step).last()
        assertNotNull(restartedStep)
        assertEquals(step, restartedStep.original)
        assertEquals(1, ProcessingStepUpdate.countByProcessingStep(restartedStep))
        assertEquals(ExecutionState.CREATED, ProcessingStepUpdate.findAllByProcessingStep(restartedStep).last().state)
        assertEquals(restartedStep, schedulerService.queue.first())
        schedulerService.schedule()
        assertEquals(8, ProcessingStepUpdate.countByProcessingStep(step))
        assertEquals(3, ProcessingStepUpdate.countByProcessingStep(restartedStep))
        assertEquals(ExecutionState.FINISHED, ProcessingStepUpdate.findAllByProcessingStep(restartedStep).last().state)
        assertFalse(process.finished)
        schedulerService.schedule()
        assertTrue(process.finished)
        assertQueueAndRunningToBeEmpty()
        assertTrue(process.finished)
    }

    /**
     * Tests that the previous next link is updated and the next job is run.
     */
    @Test
    void testRestartProcessingStepUpdatesLink() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        JobDefinition jobDefinition2 = createTestJob("test2", jep)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition3 = createTestEndStateAwareJob("test3", jep, jobDefinition2)
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        // let first ProcessingStep succeed
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }
        assertNotNull(step.save())
        mockProcessingStepAsSucceeded(step)
        ProcessingStep step2 = new ProcessingStep(jobDefinition: jobDefinition2, process: process, previous: step)
        step2.metaClass.belongsToMultiJob = { -> return false }
        assertNotNull(step2.save())
        step.next = step2
        assertNotNull(step.save())
        // verify links before anything is done
        assertNull(step.previous)
        assertEquals(step2, step.next)
        assertEquals(step, step2.previous)
        assertNull(step2.next)
        // mark step2 as failed
        mockProcessingStepAsFailed(step2)
        process.finished = true
        assertNotNull(process.save())
        // let's restart step2
        assertEquals(0, RestartedProcessingStep.count())
        schedulerService.restartProcessingStep(step2)
        assertEquals(1, RestartedProcessingStep.count())
        schedulerService.schedule()
        schedulerService.schedule()
        assertQueueAndRunningToBeEmpty()
        assertTrue(process.finished)
        // validate new chain
        step = ProcessingStep.findByJobDefinition(jobDefinition)
        assertNull(step.previous)
        RestartedProcessingStep restartedStep = RestartedProcessingStep.findByJobDefinition(jobDefinition2)
        assertEquals(restartedStep.previous, step)
        assertEquals(step.next, restartedStep)
        assertEquals(step2, restartedStep.original)
        assertNull(step2.next)
        ProcessingStep lastStep = ProcessingStep.findByJobDefinition(jobDefinition3)
        assertEquals(restartedStep.next, lastStep)
        assertEquals(lastStep.previous, restartedStep)
        assertNull(lastStep.next)
    }

    /**
     * Test that the branching is done correctly.
     */
    @Test
    void testRestartProcessingStepKeepsLinks() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        JobDefinition jobDefinition2 = createTestJob("test2", jep)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition3 = createTestJob("test3", jep)
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        JobDefinition jobDefinition4 = createTestEndStateAwareJob("test4", jep, jobDefinition3)
        jobDefinition3.next = jobDefinition4
        assertNotNull(jobDefinition3.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        ProcessingStep firstStep = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(firstStep.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.CREATED,
                previous: null,
                processingStep: firstStep
        )
        assertNotNull(update.save(flush: true))
        schedulerService.queue << firstStep
        schedulerService.schedule()
        schedulerService.schedule()
        schedulerService.schedule()
        schedulerService.schedule()
        assertQueueAndRunningToBeEmpty()
        assertTrue(process.finished)
        // let's get our ProcessingSteps
        ProcessingStep secondStep = ProcessingStep.findByJobDefinition(jobDefinition2)
        assertNotNull(secondStep)
        ProcessingStep thirdStep = secondStep.next
        assertNotNull(thirdStep)
        ProcessingStep fourthStep = thirdStep.next
        assertNotNull(fourthStep)
        // lets fail the second job
        ProcessingStepUpdate failureUpdate = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FAILURE,
                previous: ProcessingStepUpdate.findAllByProcessingStep(secondStep).last(),
                processingStep: secondStep
        )
        assertNotNull(failureUpdate.save())
        schedulerService.restartProcessingStep(secondStep)
        schedulerService.schedule()
        schedulerService.schedule()
        schedulerService.schedule()
        assertQueueAndRunningToBeEmpty()
        assertTrue(process.finished)
        // verify the chain of ProcessingSteps
        assertEquals(1, RestartedProcessingStep.count())
        assertEquals(1, ProcessingStep.countByJobDefinition(jobDefinition))
        assertEquals(2, ProcessingStep.countByJobDefinition(jobDefinition2))
        assertEquals(2, ProcessingStep.countByJobDefinition(jobDefinition3))
        assertEquals(2, ProcessingStep.countByJobDefinition(jobDefinition4))
        RestartedProcessingStep restartedStep = RestartedProcessingStep.list().last()
        assertEquals(firstStep.next, restartedStep)
        assertEquals(restartedStep.previous, firstStep)
        assertEquals(restartedStep.original, secondStep)
        assertTrue(restartedStep.next != thirdStep)
        assertEquals(secondStep.previous, firstStep)
        assertEquals(secondStep.next, thirdStep)
        assertEquals(thirdStep.previous, secondStep)
        assertEquals(thirdStep.next, fourthStep)
        assertEquals(fourthStep.previous, thirdStep)
        assertNull(fourthStep.next)
        ProcessingStep thirdStep2 = ProcessingStep.findAllByJobDefinition(jobDefinition3).last()
        assertTrue(thirdStep2 != thirdStep)
        assertEquals(restartedStep.next, thirdStep2)
        assertEquals(thirdStep2.previous, restartedStep)
        ProcessingStep fourthStep2 = ProcessingStep.findAllByJobDefinition(jobDefinition4).last()
        assertTrue(fourthStep2 != fourthStep)
        assertEquals(thirdStep2.next, fourthStep2)
        assertEquals(fourthStep2.previous, thirdStep2)
        assertNull(fourthStep2.next)
    }

    /**
     * Test for BUG: #OTP-57
     */
    @Test
    void testRestartProcessingStepKeepsParameters() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition1 = createTestJob("test", jep)
        jep.firstJob = jobDefinition1
        assertNotNull(jep.save())
        JobDefinition jobDefinition2 = createTestJob("test2", jep)
        jobDefinition1.next = jobDefinition2
        assertNotNull(jobDefinition1.save())
        JobDefinition jobDefinition3 = createTestEndStateAwareJob("test4", jep, jobDefinition2)
        jobDefinition2.next = jobDefinition3
        assertNotNull(jobDefinition2.save())
        assertNotNull(jep.save(flush: true))

        // create some parameter types
        ParameterType type4 = new ParameterType(name: "passthrough", jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.PASSTHROUGH)
        assertNotNull(type4.save())
        ParameterType type5 = new ParameterType(name: "constant", jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.INPUT)
        assertNotNull(type5.save())

        // create the Mappings
        ParameterMapping mapping1 = new ParameterMapping(from: ParameterType.findByNameAndJobDefinition("test", jobDefinition1), to: ParameterType.findByNameAndJobDefinition("input", jobDefinition2), job: jobDefinition2)
        jobDefinition2.addToParameterMappings(mapping1)
        ParameterMapping mapping2 = new ParameterMapping(from: ParameterType.findByNameAndJobDefinition("test2", jobDefinition1), to: type4, job: jobDefinition2)
        jobDefinition2.addToParameterMappings(mapping2)
        assertNotNull(jobDefinition2.save(flush: true))

        // create the constant parameter for jobDefinition2
        jobDefinition2.addToConstantParameters(new Parameter(type: type5, value: "foobar"))
        assertNotNull(jobDefinition2.save(flush: true))

        // start the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests")
        assertNotNull(process.save())
        ProcessingStep firstStep = new ProcessingStep(jobDefinition: jobDefinition1, process: process)
        firstStep.metaClass.belongsToMultiJob = { -> return false }
        assertNotNull(firstStep.save())
        mockProcessingStepAsFinished(firstStep)
        // create some output parameters for the step
        firstStep.addToOutput(new Parameter(type: ParameterType.findByNameAndJobDefinition("test", jobDefinition1), value: "bar"))
        firstStep.addToOutput(new Parameter(type: ParameterType.findByNameAndJobDefinition("test2", jobDefinition1), value: "foo"))
        assertNotNull(firstStep.save(flush: true))

        // create second ProcessingStep
        ProcessingStep secondStep = new ProcessingStep(jobDefinition: jobDefinition2, process: process, previous: firstStep)
        secondStep.metaClass.belongsToMultiJob = { -> return false }
        assertNotNull(secondStep.save())
        firstStep.next = secondStep
        assertNotNull(firstStep.save(flush: true))
        mockProcessingStepAsFailed(secondStep)
        process.finished = true
        assertNotNull(process.save(flush: true))

        // now let's restart the failed Processing Step
        schedulerService.restartProcessingStep(secondStep, false)
        RestartedProcessingStep restartedStep = RestartedProcessingStep.list().last()
        assertNotNull(restartedStep)
        assertSame(secondStep, restartedStep.original)
        assertFalse(restartedStep.input.empty)
        List<String> values = restartedStep.input.collect { it.value }.sort()
        assertEquals(2, values.size())
        assertEquals("bar", values[0])
        assertEquals("foobar", values[1])
        assertFalse(restartedStep.output.empty)
        assertEquals(1, restartedStep.output.size())
        assertEquals("foo", restartedStep.output[0].value)
    }

    private ProcessingStep createFailedProcessingStep() {
        assertQueueAndRunningToBeEmpty()
        // create the JobExecutionPlan with two Job Definitions
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0)
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        assertNotNull(jobDefinition.save())
        assertNotNull(jep.save(flush: true))
        // create the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: this.class.name)
        process.finished = true
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return true }
        assertNotNull(step.save())
        mockProcessingStepAsFailed(step)
        return step
    }

    @Test
    void testRestartProcessingStep_WhenMultiJobResumeTrue() {
        ProcessingStep step = createFailedProcessingStep()
        schedulerService.restartProcessingStep(step, false, true)
        assertEquals(5, ProcessingStepUpdate.countByProcessingStep(step))
        assertEquals(ExecutionState.SUSPENDED, ProcessingStepUpdate.findAllByProcessingStep(step).last().state)
        // no RestartedProcessingSteps should be created
        assertEquals([], RestartedProcessingStep.findAllByOriginal(step))
        assertFalse(step.process.finished)
        assertQueueAndRunningToBeEmpty()
    }

    @Test
    void testRestartProcessingStep_WhenMultiJobResumeFalse() {
        ProcessingStep step = createFailedProcessingStep()
        schedulerService.restartProcessingStep(step)
        assertEquals(5, ProcessingStepUpdate.countByProcessingStep(step))
        assertEquals(ExecutionState.RESTARTED, ProcessingStepUpdate.findAllByProcessingStep(step).last().state)
        // RestartedProcessingStep should be created
        assertEquals(1, RestartedProcessingStep.count())
        assertFalse(step.process.finished)
    }

    @Test
    void testIsJobResumable_notResumable() {
        final ProcessingStep processingStep = new ProcessingStep(jobClass: TestJob.class.name)
        assert schedulerService.isJobResumable(processingStep) == false
    }

    @Test
    void testIsJobResumable_resumable() {
        final ProcessingStep processingStep = new ProcessingStep(jobClass: ResumableTestJob.class.name)
        assert schedulerService.isJobResumable(processingStep) == true
    }

    @Test
    void testIsJobResumable_resumableSometimesResumable() {
        final ProcessingStep processingStep = new ProcessingStep(jobClass: ResumableSometimesResumableTestJob.class.name)
        assert schedulerService.isJobResumable(processingStep) == true
    }

    final Closure testIsJobResumable_sometimesResumable = { final boolean resumable ->
        final ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(SometimesResumableTestJob.class.name)
        final SometimesResumableTestJob job = new SometimesResumableTestJob()
        job.processingStep = processingStep
        job.resumable = resumable
        schedulerService.running << job
        assert schedulerService.isJobResumable(processingStep) == resumable
    }

    @Test
    void testIsJobResumable_sometimesResumable_true() {
        testIsJobResumable_sometimesResumable true
    }

    @Test
    void testIsJobResumable_sometimesResumable_false() {
        testIsJobResumable_sometimesResumable false
    }

    @Test
    void testIsJobResumable_sometimesResumable_noRunningJob() {
        final ProcessingStep processingStep = new ProcessingStep(jobClass: SometimesResumableTestJob.class.name)
        shouldFail RuntimeException, { schedulerService.isJobResumable(processingStep) }
    }

    private void assertQueueAndRunningToBeEmpty() {
        assertTrue(schedulerService.queue.isEmpty())
        assertTrue(schedulerService.running.isEmpty())
    }

    private ProcessingStepUpdate mockProcessingStepAsFinished(ProcessingStep step) {
        ProcessingStepUpdate update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.CREATED,
                previous: null,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.STARTED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FINISHED,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
        return update
    }

    private void mockProcessingStepAsFailed(ProcessingStep step) {
        ProcessingStepUpdate update = mockProcessingStepAsFinished(step)
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.FAILURE,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
    }

    private void mockProcessingStepAsSucceeded(ProcessingStep step) {
        ProcessingStepUpdate update = mockProcessingStepAsFinished(step)
        update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.SUCCESS,
                previous: update,
                processingStep: step
        )
        assertNotNull(update.save(flush: true))
    }
}
