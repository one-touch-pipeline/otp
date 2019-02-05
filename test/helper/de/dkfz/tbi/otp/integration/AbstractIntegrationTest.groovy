/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.integration

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.security.UserAndRoles

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

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
