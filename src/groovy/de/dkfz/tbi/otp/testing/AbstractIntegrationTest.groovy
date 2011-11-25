package de.dkfz.tbi.otp.testing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid

/**
 * Abstract base class for all tests to have some shared functionality.
 *
 */
abstract class AbstractIntegrationTest {

   /**
    * Dependency injection of Spring security's authentication manager
    */
    def authenticationManager
    /**
     * Dependency injection of Spring security service
     */
    def springSecurityService

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
       ParameterType input = new ParameterType(name: "input", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.INPUT)
       ParameterType input2 = new ParameterType(name: "input2", description: "Test description", jobDefinition: jobDefinition, usage: ParameterUsage.INPUT)
       assertNotNull(test.save())
       assertNotNull(test2.save())
       assertNotNull(input.save())
       assertNotNull(input2.save())
       return jobDefinition
   }

  /**
   * Creates three users and their roles:
   * @li testuser with password secret and role ROLE_USER
   * @li user with password verysecret and role ROLE_USER
   * @li admin with password 1234 and ROLE_ADMIN and ROLE_USER
   */
  protected void createUserAndRoles() {
      User user = new User(username: "testuser",
              password: springSecurityService.encodePassword("secret"),
              userRealName: "Test",
              email: "test@test.com",
              enabled: true,
              accountExpired: false,
              accountLocked: false,
              passwordExpired: false)
      assertNotNull(user.save())
      assertNotNull(new AclSid(sid: user.username, principal: true).save(flush: true))
      User user2 = new User(username: "user",
              password: springSecurityService.encodePassword("verysecret"),
              userRealName: "Test2",
              email: "test2@test.com",
              enabled: true,
              accountExpired: false,
              accountLocked: false,
              passwordExpired: false)
      assertNotNull(user2.save())
      assertNotNull(new AclSid(sid: user2.username, principal: true).save(flush: true))
      User admin = new User(username: "admin",
              password: springSecurityService.encodePassword("1234"),
              userRealName: "Administrator",
              email: "admin@test.com",
              enabled: true,
              accountExpired: false,
              accountLocked: false,
              passwordExpired: false)
      assertNotNull(admin.save())
      assertNotNull(new AclSid(sid: admin.username, principal: true).save(flush: true))
      Role userRole = new Role(authority: "ROLE_USER")
      assertNotNull(userRole.save())
      UserRole.create(user, userRole, false)
      UserRole.create(user2, userRole, false)
      UserRole.create(admin, userRole, false)
      Role adminRole = new Role(authority: "ROLE_ADMIN")
      assertNotNull(adminRole.save())
      UserRole.create(admin, adminRole, false)
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
