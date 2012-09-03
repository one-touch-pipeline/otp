package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.test.ControllerUnitTestCase

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid
import org.junit.*
import org.springframework.security.acls.domain.BasePermission

import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole

class RunControllerTests extends ControllerUnitTestCase {
    def springSecurityService
    def aclUtilService

    @Before
    void setUp() {
        super.setUp()
        createUserAndRoles()
    }

    private void createUserAndRoles() {
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
      User operator = new User(username: "operator",
              password: springSecurityService.encodePassword("verysecret"),
              userRealName: "Operator",
              email: "test2@test.com",
              enabled: true,
              accountExpired: false,
              accountLocked: false,
              passwordExpired: false)
      assertNotNull(operator.save())
      assertNotNull(new AclSid(sid: operator.username, principal: true).save(flush: true))
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
      Role operatorRole = new Role(authority: "ROLE_OPERATOR")
      assertNotNull(operatorRole.save())
      UserRole.create(operator, operatorRole, true)
  }

    void testDisplayRedirect() {
        controller.display()
        assertEquals("show", controller.redirectArgs.action)
    }

    void testShowRunNonExisting() {
        SpringSecurityUtils.doWithAuth("testuser") {
            controller.params.id = "0"
            controller.show()
            assertEquals(404, controller.response.status)
        }
    }

    void testShowRunMissingId() {
        SpringSecurityUtils.doWithAuth("testuser") {
            controller.show()
            assertEquals(404, controller.response.status)
        }
    }

    void testShowRunIdNoLong() {
        SpringSecurityUtils.doWithAuth("testuser") {
            controller.params.id = "test"
            controller.show()
            assertEquals(404, controller.response.status)
        }
    }

    @Ignore
    void testShowRunMinimalData() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())

        // test the outcome
        controller.params.id = run.id.toString()
        def model = controller.show()
        assertEquals(run, model.run)
        assertEquals(0, model.finalPaths.size())
        assertNull(model.keys[0])
        assertNull(model.keys[1])
        assertTrue(model.processParameters.isEmpty())
        assertTrue(model.metaDataFiles.isEmpty())
        assertTrue(model.seqTracks.isEmpty())
        assertNull(model.nextRun)
        assertNull(model.previousRun)
    }

    @Ignore
    void testShowRunByName() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())

        // test the outcome
        controller.params.id = "test"
        def model = controller.show()
        assertEquals(run, model.run)
        assertEquals(0, model.finalPaths.size())
        assertNull(model.keys[0])
        assertNull(model.keys[1])
        assertTrue(model.processParameters.isEmpty())
        assertTrue(model.metaDataFiles.isEmpty())
        assertTrue(model.seqTracks.isEmpty())
        assertNull(model.nextRun)
        assertNull(model.previousRun)
    }

    @Ignore
    void testShowRunWithNextRun() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())
        Run run2 = new Run(name: "test1", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run2.save())
        Run run3 = new Run(name: "test2", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run3.save())
        // test the outcome
        controller.params.id = run.id.toString()
        def model = controller.show()
        assertEquals(run, model.run)
        assertEquals(run2, model.nextRun)
        assertNull(model.previousRun)
    }

    void testShowRunWithPrevRun() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run.save())
        Run run2 = new Run(name: "test1", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run2.save())
        Run run3 = new Run(name: "test2", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run3.save())
        // add permissions
        SpringSecurityUtils.doWithAuth("admin") {
            aclUtilService.addPermission(seqCenter, "testuser", BasePermission.READ)
        }
        SpringSecurityUtils.doWithAuth("testuser") {
            // test the outcome
            controller.params.id = run3.id.toString()
            def model = controller.show()
            assertEquals(run3, model.run)
            assertEquals(run2, model.previousRun)
            assertNull(model.nextRun)
        }
    }

    void testShowRunWithPrevNextRun() {
        SeqPlatform seqPlatform = new SeqPlatform(name: "test")
        assertNotNull(seqPlatform.save())
        SeqCenter seqCenter = new SeqCenter(name: "test", dirName: "directory")
        assertNotNull(seqCenter.save())
        Run run1 = new Run(name: "test", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run1.save())
        Run run2 = new Run(name: "test1", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run2.save())
        Run run3 = new Run(name: "test2", seqCenter: seqCenter, seqPlatform: seqPlatform)
        assertNotNull(run3.save())
        // add permissions
        SpringSecurityUtils.doWithAuth("admin") {
            aclUtilService.addPermission(seqCenter, "testuser", BasePermission.READ)
        }
        SpringSecurityUtils.doWithAuth("testuser") {
            // test the outcome
            controller.params.id = run2.id.toString()
            def model = controller.show()
            assertEquals(run2, model.run)
            assertEquals(run1, model.previousRun)
            assertEquals(run3, model.nextRun)
        }
    }
}
