package de.dkfz.tbi.otp.testing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.GrantedAuthorityImpl
import grails.plugin.springsecurity.SpringSecurityUtils

/**
 * Abstract base class for all tests to have some shared functionality.
 *
 */
abstract class AbstractIntegrationTest implements UserAndRoles{

   /**
    * Dependency injection of Spring security's authentication manager
    */
    def authenticationManager

    @SuppressWarnings("CatchException")
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

  /**
   * Sets an authentication based on username and password.
   * @param username The name of the user
   * @param password The password of the user.
   * @return The Authentication object
   */
  protected def authenticate(String username, String password) {
      def authToken = new UsernamePasswordAuthenticationToken(username, password)
      def auth = authenticationManager.authenticate(authToken)
      SecurityContextHolder.getContext().setAuthentication(auth)
      return auth
  }

  /**
   * Modifies the ifAnyGranted method of SpringSecurityUtils to return @p admin value.
   * @param admin if @c true, all access to ifAnyGranted returns @c true, @c false otherwise
   */
  protected void processAdminUser(boolean admin) {
      SpringSecurityUtils.metaClass.'static'.ifAnyGranted = { String parameter ->
          return admin
      }
  }

    /**
     * Sets the current authentication to testuser and does not process as admin user.
     * @return The testusers authentication
     */
    protected def authenticateAsTestUser() {
        processAdminUser(false)
        return authenticate("testuser", "secret")
    }

    /**
     * Sets the current authentication to user and does not process as admin user.
     * @return The users authentication
     */
    protected def authenticateAsUser() {
        processAdminUser(false)
        return authenticate("user", "verysecret")
    }

    /**
     * Sets the current authentication to admin and processes as admin user.
     * @return The admin authentication
     */
    protected def authenticateAsAdmin() {
        processAdminUser(true)
        return authenticate("admin", "1234")
    }

     /**
     * Sets an anonymous authentication and does not process as admin user.
     * @return The anonymous authentication
     */
    protected def authenticateAnonymous() {
        processAdminUser(false)
        def auth = new AnonymousAuthenticationToken("test", "Anonymous", [ new GrantedAuthorityImpl("ROLE_ANONYMOUS")])
        SecurityContextHolder.getContext().setAuthentication(auth)
        return auth
    }
}
