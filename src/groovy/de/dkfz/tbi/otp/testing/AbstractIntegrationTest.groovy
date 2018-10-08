package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*

import static org.junit.Assert.*
/**
 * Abstract base class for all tests to have some shared functionality.
 */
abstract class AbstractIntegrationTest implements UserAndRoles {

   /**
    * Dependency injection of Spring security's authentication manager
    */
    def authenticationManager

    @Deprecated
    protected def shouldFail = { exception, code ->
        try {
            code.call()
            fail("Exception of type ${exception} expected, but none was thrown.")
        } catch (Exception e) {
            boolean foundException = false
            if (exception.isAssignableFrom(e.class)) {
                foundException = true
            }
            Throwable cause = e.getCause()
            while (!foundException && cause) {
                if (exception.isAssignableFrom(cause.class)) {
                    foundException = true
                }
                cause = cause.getCause()
            }
            if (!foundException) {
                throw new Error("Exception of type ${exception} expected, but got ${e.class}.", e)
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
        ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType test2 = new ParameterType(name: "test2", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType input = new ParameterType(name: "input", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        ParameterType input2 = new ParameterType(name: "input2", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
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
   protected JobDefinition createTestEndStateAwareJob(String name, JobExecutionPlan jep, JobDefinition previous = null, String beanName = "testEndStateAwareJob") {
       JobDefinition jobDefinition = new JobDefinition(name: name, bean: beanName, plan: jep, previous: previous)
       assertNotNull(jobDefinition.save())
       ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
       ParameterType test2 = new ParameterType(name: "test2", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
       ParameterType input = new ParameterType(name: "input", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
       ParameterType input2 = new ParameterType(name: "input2", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
       assertNotNull(test.save())
       assertNotNull(test2.save())
       assertNotNull(input.save())
       assertNotNull(input2.save())
       return jobDefinition
   }
}
