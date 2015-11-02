package de.dkfz.tbi.otp.job.processing

import grails.buildtestdata.mixin.Build

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
@Build([ProcessingStep])
class ProcessingStepUnitTests {

    @Test
    void testNullable() {
        ProcessingStep step = new ProcessingStep()
        assertFalse(step.validate())
        assertEquals("nullable", step.errors["jobDefinition"].code)
        assertEquals("nullable", step.errors["process"].code)

        Process process = new Process()
        step.process = process
        assertFalse(step.validate())
        assertEquals("nullable", step.errors["jobDefinition"].code)
        assertNull(step.errors["process"])

        JobDefinition jobDefinition = new JobDefinition()
        step.jobDefinition = jobDefinition
        assertTrue(step.validate())
    }

    @Test
    void testPrevious() {
        Process process = Process.build(id: 1)
        Process process2 = Process.build(id: 2)
        JobDefinition jobDefinition = JobDefinition.build(id: 1, plan: process.jobExecutionPlan)
        JobDefinition jobDefinition2 = JobDefinition.build(id: 2, plan: process2.jobExecutionPlan)

        ProcessingStep step = ProcessingStep.build(process: process, jobDefinition: jobDefinition)
        assertTrue(step.validate())

        ProcessingStep testStep1 = ProcessingStep.build(process: process2, jobDefinition: jobDefinition2)
        step.previous = testStep1
        assertFalse(step.validate())
        assertEquals("process", step.errors["previous"].code)

        ProcessingStep testStep2 = ProcessingStep.build(process: process, jobDefinition: jobDefinition)
        step.previous = testStep2
        assertFalse(step.validate())
        assertEquals("jobDefinition", step.errors["previous"].code)

        jobDefinition2.plan = process.jobExecutionPlan
        ProcessingStep testStep3 = ProcessingStep.build(process: process, jobDefinition: jobDefinition2)

        step.previous = testStep3
        step.next = testStep3
        assertFalse(step.validate())
        assertEquals("next", step.errors["previous"].code)
        assertEquals("previous", step.errors["next"].code)

        step.next = null
        assertTrue(step.validate())
    }

    @Test
    void testNext() {
        Process process = Process.build(id: 1)
        Process process2 = Process.build(id: 2)
        JobDefinition jobDefinition = JobDefinition.build(id: 1, plan: process.jobExecutionPlan)
        JobDefinition jobDefinition2 = JobDefinition.build(id: 2, plan: process2.jobExecutionPlan)

        ProcessingStep step = ProcessingStep.build(process: process, jobDefinition: jobDefinition)
        assertTrue(step.validate())

        ProcessingStep testStep1 = ProcessingStep.build(process: process2, jobDefinition: jobDefinition2)
        step.next = testStep1
        assertFalse(step.validate())
        assertEquals("process", step.errors["next"].code)

        ProcessingStep testStep2 = ProcessingStep.build(process: process, jobDefinition: jobDefinition)
        step.next = testStep2
        assertFalse(step.validate())
        assertEquals("jobDefinition", step.errors["next"].code)

        jobDefinition2.plan = process.jobExecutionPlan
        ProcessingStep testStep3 = ProcessingStep.build(process: process, jobDefinition: jobDefinition2)
        step.previous = testStep3
        step.next = testStep3
        assertFalse(step.validate())
        assertEquals("next", step.errors["previous"].code)
        assertEquals("previous", step.errors["next"].code)

        step.previous = null
        assertTrue(step.validate())
    }

    @Test
    void testProcess() {
        JobExecutionPlan plan1 = JobExecutionPlan.build(id: 1)
        JobExecutionPlan plan2 = JobExecutionPlan.build(id: 2)
        JobDefinition jobDefinition = JobDefinition.build(plan: plan1)
        Process process = Process.build(jobExecutionPlan: plan2)
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)

        assertFalse(step.validate())
        assertEquals("jobExecutionPlan", step.errors["process"].code)
        process.jobExecutionPlan = plan1
        assertTrue(step.validate())
    }

    @Test
    void testPbsJobDescription() {
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.build(
                name: "testWorkFlow"
        )
        Process process = Process.build(
                jobExecutionPlan: jobExecutionPlan
        )
        ProcessingStep step = ProcessingStep.build(
                id: 9999999,
                jobClass: "foo",
                process: process,
        )
        assertEquals(step.getPbsJobDescription(), "otp_TEST_testWorkFlow_9999999_foo")
    }

    @Test
    void testPbsJobDescriptionNull() {
        shouldFail() {
            null.getPbsJobDescription()
        }
    }
}
