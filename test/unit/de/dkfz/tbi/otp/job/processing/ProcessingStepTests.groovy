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

    void setUp() {
        // Setup logic here
    }

    void tearDown() {
        // Tear down logic here
    }

    void testInput() {
        Process process = new Process()
        JobDefinition jobDefinition = new JobDefinition()
        JobDefinition jobDefinition2 = new JobDefinition()
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
        ParameterType type = new ParameterType(name: "test", jobDefinition: jobDefinition, usage: ParameterUsage.INPUT)
        mockForConstraintsTests(ParameterType, [])
        assertTrue(type.validate())
        mockForConstraintsTests(Parameter, [])
        Parameter parameter = new Parameter(value: "1234", type: type)
        assertTrue(parameter.validate())
        step.addToInput(parameter)
        assertTrue(step.validate())
        // create a Parameter for an incorrect Parameter type
        ParameterType type2 = new ParameterType(name: "other job definition", jobDefinition: jobDefinition2, usage: ParameterUsage.INPUT)
        assertTrue(type2.validate())
        Parameter failingParam1 = new Parameter(value: "1234", type: type2)
        assertTrue(failingParam1.validate())
        step.addToInput(failingParam1)
        assertFalse(step.validate())
        assertEquals("invalid.jobDefinition", step.errors["input"])
        failingParam1.type = type
        assertTrue(step.validate())
        // create a Parameter for an Output parameter
        ParameterType outputType = new ParameterType(name: "output", jobDefinition: jobDefinition, usage: ParameterUsage.OUTPUT)
        assertTrue(outputType.validate())
        Parameter failingParam2 = new Parameter(value: "output", type: outputType)
        assertTrue(failingParam2.validate())
        step.addToInput(failingParam2)
        assertFalse(step.validate())
        assertEquals("invalid.usage", step.errors["input"])
        failingParam2.type = type
        assertTrue(step.validate())
        // create a Parameter for a PassThrough parameter
        ParameterType passThroughType = new ParameterType(name: "passthrough", jobDefinition: jobDefinition, usage: ParameterUsage.PASSTHROUGH)
        assertTrue(passThroughType.validate())
        Parameter failingParam3 = new Parameter(value: "passthrough", type: passThroughType)
        assertTrue(failingParam3.validate())
        step.addToInput(failingParam3)
        assertFalse(step.validate())
        assertEquals("invalid.usage", step.errors["input"])
        failingParam3.type = type
        assertTrue(step.validate())
        // create a Parameter for an Output type for a different job definition
        ParameterType otherType = new ParameterType(name: "testing", jobDefinition: jobDefinition2, usage: ParameterUsage.OUTPUT)
        assertTrue(otherType.validate())
        Parameter failingParam4 = new Parameter(value: "bar", type: otherType)
        assertTrue(failingParam4.validate())
        step.addToInput(failingParam4)
        assertFalse(step.validate())
        assertEquals("invalid.jobDefinition", step.errors["input"])
        otherType.jobDefinition = jobDefinition
        assertFalse(step.validate())
        assertEquals("invalid.usage", step.errors["input"])
        otherType.usage = ParameterUsage.INPUT
        assertTrue(step.validate())
    }

    void testOutput() {
        Process process = new Process()
        JobDefinition jobDefinition = new JobDefinition()
        JobDefinition jobDefinition2 = new JobDefinition()
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
        ParameterType type = new ParameterType(name: "test", jobDefinition: jobDefinition, usage: ParameterUsage.OUTPUT)
        mockForConstraintsTests(ParameterType, [])
        assertTrue(type.validate())
        mockForConstraintsTests(Parameter, [])
        Parameter parameter = new Parameter(value: "1234", type: type)
        assertTrue(parameter.validate())
        step.addToOutput(parameter)
        assertTrue(step.validate())
        // use a passthrough parameter
        ParameterType passThroughType = new ParameterType(name: "passthrough", jobDefinition: jobDefinition, usage: ParameterUsage.PASSTHROUGH)
        assertTrue(passThroughType.validate())
        Parameter passThrough = new Parameter(value: "passThrough", type: passThroughType)
        assertTrue(passThrough.validate())
        step.addToOutput(passThrough)
        assertTrue(step.validate())
        // a Parameter for another job definition should fail
        ParameterType otherJobType = new ParameterType(name: "other", jobDefinition: jobDefinition2, usage: ParameterUsage.OUTPUT)
        assertTrue(otherJobType.validate())
        Parameter otherJob = new Parameter(value: "other", type: otherJobType)
        assertTrue(otherJob.validate())
        step.addToOutput(otherJob)
        assertFalse(step.validate())
        assertEquals("invalid.jobDefinition", step.errors["output"])
        otherJob.type = type
        assertTrue(step.validate())
        // a Parameter for usage input should fail
        ParameterType inputType = new ParameterType(name: "input", jobDefinition: jobDefinition, usage: ParameterUsage.INPUT)
        assertTrue(inputType.validate())
        Parameter input = new Parameter(value: "input", type: inputType)
        assertTrue(input.validate())
        step.addToOutput(input)
        assertFalse(step.validate())
        assertEquals("invalid.usage", step.errors["output"])
        inputType.usage = ParameterUsage.PASSTHROUGH
        assertTrue(step.validate())
        // a Parameter for a wrong job definition and wrong usage should fail twice
        ParameterType doubleFailType = new ParameterType(name: "two", jobDefinition: jobDefinition2, usage: ParameterUsage.INPUT)
        assertTrue(doubleFailType.validate())
        Parameter doubleFail = new Parameter(value: "two", type: doubleFailType)
        assertTrue(doubleFail.validate())
        step.addToOutput(doubleFail)
        assertFalse(step.validate())
        assertEquals("invalid.jobDefinition", step.errors["output"])
        doubleFailType.jobDefinition = jobDefinition
        assertFalse(step.validate())
        assertEquals("invalid.usage", step.errors["output"])
        doubleFailType.usage = ParameterUsage.OUTPUT
        assertTrue(step.validate())
    }
}
