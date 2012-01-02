package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ProcessingStep)
class ProcessingStepTests {

    @SuppressWarnings("EmptyMethod")
    void setUp() {
        // Setup logic here
    }

    @SuppressWarnings("EmptyMethod")
    void tearDown() {
        // Tear down logic here
    }

    void disabledTestInput() {
        Process process = new Process()
        JobDefinition jobDefinition = new JobDefinition(id: 1)
        JobDefinition jobDefinition2 = new JobDefinition(id: 2)
        mockDomain(Process, [process])
        mockDomain(JobDefinition, [jobDefinition, jobDefinition2])
        mockForConstraintsTests(ProcessingStep, [])
        
        ProcessingStep step = new ProcessingStep(jobClass: "foo",
            jobVersion: "bar",
            process: process,
            jobDefinition: jobDefinition
            )
        // simple ProcessingStep which should validate
        assertTrue(step.validate())
        // prepare a Parameter
        ParameterType type = new ParameterType(name: "test", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        mockForConstraintsTests(ParameterType, [])
        assertTrue(type.validate())
        mockForConstraintsTests(Parameter, [])
        Parameter parameter = new Parameter(value: "1234", type: type)
        assertTrue(parameter.validate())
        step.addToInput(parameter)
        assertTrue(step.validate())
        // create a Parameter for an incorrect Parameter type
        ParameterType type2 = new ParameterType(name: "other job definition", jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.INPUT)
        assertTrue(type2.validate())
        Parameter failingParam1 = new Parameter(value: "1234", type: type2)
        assertTrue(failingParam1.validate())
        step.addToInput(failingParam1)
        assertFalse(step.validate())
        assertEquals("invalid.jobDefinition", step.errors["input"])
        type2.jobDefinition = jobDefinition
        assertTrue(step.validate())
        // create a Parameter for an Output parameter
        ParameterType outputType = new ParameterType(name: "output", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        assertTrue(outputType.validate())
        Parameter failingParam2 = new Parameter(value: "output", type: outputType)
        assertTrue(failingParam2.validate())
        step.addToInput(failingParam2)
        assertFalse(step.validate())
        assertEquals("invalid.parameterUsage", step.errors["input"])
        outputType.parameterUsage = ParameterUsage.INPUT
        assertTrue(step.validate())
        // create a Parameter for a PassThrough parameter
        ParameterType passThroughType = new ParameterType(name: "passthrough", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.PASSTHROUGH)
        assertTrue(passThroughType.validate())
        Parameter failingParam3 = new Parameter(value: "passthrough", type: passThroughType)
        assertTrue(failingParam3.validate())
        step.addToInput(failingParam3)
        assertFalse(step.validate())
        assertEquals("invalid.parameterUsage", step.errors["input"])
        passThroughType.parameterUsage = ParameterUsage.INPUT
        assertTrue(step.validate())
        // create a Parameter for an Output type for a different job definition
        ParameterType otherType = new ParameterType(name: "testing", jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.OUTPUT)
        assertTrue(otherType.validate())
        Parameter failingParam4 = new Parameter(value: "bar", type: otherType)
        assertTrue(failingParam4.validate())
        step.addToInput(failingParam4)
        assertFalse(step.validate())
        assertEquals("invalid.jobDefinition", step.errors["input"])
        otherType.jobDefinition = jobDefinition
        assertFalse(step.validate())
        assertEquals("invalid.parameterUsage", step.errors["input"])
        otherType.parameterUsage = ParameterUsage.INPUT
        assertTrue(step.validate())

        // create two parameters for the same type should fail - to better test use a new ProcessingStep
        step = new ProcessingStep(jobClass: "foo",
            jobVersion: "bar",
            process: process,
            jobDefinition: jobDefinition
            )
        assertTrue(step.validate())
        Parameter param1 = new Parameter(value: "foo", type: type)
        assertTrue(param1.validate())
        step.addToInput(param1)
        assertTrue(step.validate())
        Parameter param2 = new Parameter(value: "bar", type: type)
        assertTrue(param2.validate())
        step.addToInput(param2)
        assertFalse(step.validate())
        assertEquals("unique.type", step.errors["input"])
        // output type was changed above to work
        param2.type = outputType
        assertTrue(step.validate())
    }

    void disabledTestOutput() {
        Process process = new Process()
        JobDefinition jobDefinition = new JobDefinition(id: 1)
        JobDefinition jobDefinition2 = new JobDefinition(id: 2)
        mockDomain(Process, [process])
        mockDomain(JobDefinition, [jobDefinition, jobDefinition2])
        mockForConstraintsTests(ProcessingStep, [])

        ProcessingStep step = new ProcessingStep(jobClass: "foo",
            jobVersion: "bar",
            process: process,
            jobDefinition: jobDefinition
            )
        // simple ProcessingStep which should validate
        assertTrue(step.validate())
        // prepare a Parameter
        ParameterType type = new ParameterType(name: "test", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        mockForConstraintsTests(ParameterType, [])
        assertTrue(type.validate())
        mockForConstraintsTests(Parameter, [])
        Parameter parameter = new Parameter(value: "1234", type: type)
        assertTrue(parameter.validate())
        step.addToOutput(parameter)
        assertTrue(step.validate())
        // use a passthrough parameter
        ParameterType passThroughType = new ParameterType(name: "passthrough", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.PASSTHROUGH)
        assertTrue(passThroughType.validate())
        Parameter passThrough = new Parameter(value: "passThrough", type: passThroughType)
        assertTrue(passThrough.validate())
        step.addToOutput(passThrough)
        assertTrue(step.validate())
        // a Parameter for another job definition should fail
        ParameterType otherJobType = new ParameterType(name: "other", jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.OUTPUT)
        assertTrue(otherJobType.validate())
        Parameter otherJob = new Parameter(value: "other", type: otherJobType)
        assertTrue(otherJob.validate())
        step.addToOutput(otherJob)
        assertFalse(step.validate())
        assertEquals("invalid.jobDefinition", step.errors["output"])
        otherJobType.jobDefinition = jobDefinition
        assertTrue(step.validate())
        // a Parameter for usage input should fail
        ParameterType inputType = new ParameterType(name: "input", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        assertTrue(inputType.validate())
        Parameter input = new Parameter(value: "input", type: inputType)
        assertTrue(input.validate())
        step.addToOutput(input)
        assertFalse(step.validate())
        assertEquals("invalid.parameterUsage", step.errors["output"])
        inputType.parameterUsage = ParameterUsage.PASSTHROUGH
        assertTrue(step.validate())
        // a Parameter for a wrong job definition and wrong usage should fail twice
        ParameterType doubleFailType = new ParameterType(name: "two", jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.INPUT)
        assertTrue(doubleFailType.validate())
        Parameter doubleFail = new Parameter(value: "two", type: doubleFailType)
        assertTrue(doubleFail.validate())
        step.addToOutput(doubleFail)
        assertFalse(step.validate())
        assertEquals("invalid.jobDefinition", step.errors["output"])
        doubleFailType.jobDefinition = jobDefinition
        assertFalse(step.validate())
        assertEquals("invalid.parameterUsage", step.errors["output"])
        doubleFailType.parameterUsage = ParameterUsage.OUTPUT
        assertTrue(step.validate())

        // create two parameters for the same type should fail - to better test use a new ProcessingStep
        step = new ProcessingStep(jobClass: "foo",
            jobVersion: "bar",
            process: process,
            jobDefinition: jobDefinition
            )
        assertTrue(step.validate())
        Parameter param1 = new Parameter(value: "foo", type: type)
        assertTrue(param1.validate())
        step.addToOutput(param1)
        assertTrue(step.validate())
        Parameter param2 = new Parameter(value: "bar", type: type)
        assertTrue(param2.validate())
        step.addToOutput(param2)
        assertFalse(step.validate())
        assertEquals("unique.type", step.errors["output"])
        // output type was changed above to work
        param2.type = otherJobType
        assertTrue(step.validate())
    }

    @Test
    void testNullable() {
        ProcessingStep step = new ProcessingStep()
        mockForConstraintsTests(ProcessingStep, [])
        assertFalse(step.validate())
        assertEquals("nullable", step.errors["jobDefinition"])
        assertEquals("nullable", step.errors["process"])
        Process process = new Process()
        mockDomain(Process, [process])
        step.process = process
        assertFalse(step.validate())
        assertEquals("nullable", step.errors["jobDefinition"])
        assertNull(step.errors["process"])
        JobDefinition jobDefinition = new JobDefinition()
        mockDomain(JobDefinition, [jobDefinition])
        step.jobDefinition = jobDefinition
        assertTrue(step.validate())
    }

    @Test
    void testPrevious() {
        Process process = new Process(id: 1)
        Process process2 = new Process(id: 2)
        JobDefinition jobDefinition = new JobDefinition(id: 1)
        JobDefinition jobDefinition2 = new JobDefinition(id: 2)
        ProcessingStep testStep1 = new ProcessingStep(process: process2, jobDefinition: jobDefinition)
        ProcessingStep testStep2 = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        ProcessingStep testStep3 = new ProcessingStep(process: process, jobDefinition: jobDefinition2)
        mockDomain(Process, [process, process2])
        mockDomain(JobDefinition, [jobDefinition, jobDefinition2])
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        mockForConstraintsTests(ProcessingStep, [testStep1, testStep2])
        assertTrue(step.validate())
        step.previous = testStep1
        assertFalse(step.validate())
        assertEquals("process", step.errors["previous"])
        step.previous = testStep2
        assertFalse(step.validate())
        assertEquals("jobDefinition", step.errors["previous"])
        step.previous = testStep3
        step.next = testStep3
        assertFalse(step.validate())
        assertEquals("next", step.errors["previous"])
        assertEquals("previous", step.errors["next"])
        step.next = null
        assertTrue(step.validate())
    }

    @Test
    void testNext() {
        Process process = new Process(id: 1)
        Process process2 = new Process(id: 2)
        JobDefinition jobDefinition = new JobDefinition(id: 1)
        JobDefinition jobDefinition2 = new JobDefinition(id: 2)
        ProcessingStep testStep1 = new ProcessingStep(process: process2, jobDefinition: jobDefinition)
        ProcessingStep testStep2 = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        ProcessingStep testStep3 = new ProcessingStep(process: process, jobDefinition: jobDefinition2)
        mockDomain(Process, [process, process2])
        mockDomain(JobDefinition, [jobDefinition, jobDefinition2])
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        mockForConstraintsTests(ProcessingStep, [testStep1, testStep2])
        assertTrue(step.validate())
        step.next = testStep1
        assertFalse(step.validate())
        assertEquals("process", step.errors["next"])
        step.next = testStep2
        assertFalse(step.validate())
        assertEquals("jobDefinition", step.errors["next"])
        step.previous = testStep3
        step.next = testStep3
        assertFalse(step.validate())
        assertEquals("next", step.errors["previous"])
        assertEquals("previous", step.errors["next"])
        step.previous = null
        assertTrue(step.validate())
    }

    @Test
    void testProcess() {
        JobExecutionPlan plan1 = new JobExecutionPlan(id: 1)
        JobExecutionPlan plan2 = new JobExecutionPlan(id: 2)
        mockDomain(JobExecutionPlan, [plan1, plan2])
        JobDefinition jobDefinition = new JobDefinition(plan: plan1)
        JobDefinition jobDefinition2 = new JobDefinition(plan: plan2)
        mockDomain(JobDefinition, [jobDefinition, jobDefinition2])
        Process process = new Process(jobExecutionPlan: plan2)
        mockDomain(Process, [process])
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        mockForConstraintsTests(ProcessingStep, [])
        assertFalse(step.validate())
        assertEquals("jobExecutionPlan", step.errors["process"])
        process.jobExecutionPlan = plan1
        assertTrue(step.validate())
    }
}
