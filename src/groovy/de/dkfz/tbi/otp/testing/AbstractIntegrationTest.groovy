package de.dkfz.tbi.otp.testing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage

/**
 * Abstract base class for all tests to have some shared functionality.
 *
 */
abstract class AbstractIntegrationTest {
    protected def shouldFail = { exception, code ->
        try {
            code.call()
            fail("Exception of type ${exception} was expected")
        } catch (Exception e) {
            if (!exception.isAssignableFrom(e.class)) {
                fail("Exception of type ${exception} expected but got ${e.class}")
            }
        }
    }

    /**
     * Creates a JobDefinition for the testJob.
     * @param name Name of the JobDefinition
     * @param jep The JobExecutionPlan this JobDefinition will belong to
     * @param previous The previous Job Execution plan (optional)
     * @return Created JobDefinition
     */
    protected JobDefinition createTestJob(String name, JobExecutionPlan jep, JobDefinition previous = null) {
        JobDefinition jobDefinition = new JobDefinition(name: name, bean: "testJob", plan: jep, previous: previous)
        assertNotNull(jobDefinition.save())
        ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.OUTPUT)
        ParameterType test2 = new ParameterType(name: "test2", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.OUTPUT)
        ParameterType input = new ParameterType(name: "input", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.INPUT)
        ParameterType input2 = new ParameterType(name: "input2", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.INPUT)
        assertNotNull(test.save())
        assertNotNull(test2.save())
        assertNotNull(input.save())
        assertNotNull(input2.save())
        return jobDefinition
    }

   /**
    * Creates a JobDefinition for the testEndStateAwareJob.
    * @param name Name of the JobDefinition
    * @param jep The JobExecutionPlan this JobDefinition will belong to
    * @param previous The previous Job Execution plan (optional)
    * @return Created JobDefinition
    */
   protected JobDefinition createTestEndStateAwareJob(String name, JobExecutionPlan jep, JobDefinition previous = null) {
       JobDefinition jobDefinition = new JobDefinition(name: name, bean: "testEndStateAwareJob", plan: jep, previous: previous)
       assertNotNull(jobDefinition.save())
       ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.OUTPUT)
       ParameterType test2 = new ParameterType(name: "test2", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.OUTPUT)
       assertNotNull(test.save())
       assertNotNull(test2.save())
       return jobDefinition
   }
}
