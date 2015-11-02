package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole
import static org.junit.Assert.assertNotNull
import grails.plugin.springsecurity.acl.AclSid
import grails.plugin.springsecurity.acl.AclUtilService
import grails.plugin.springsecurity.SpringSecurityService

trait UserAndRoles {

    SpringSecurityService springSecurityService
    AclUtilService aclUtilService

    /**
     * Creates four users and their roles:
     * @li testuser with password secret and role ROLE_USER
     * @li user with password verysecret and role ROLE_USER
     * @li operator with password verysecret and ROLE_OPERATOR and ROLE_USER
     * @li admin with password 1234 and ROLE_ADMIN and ROLE_USER
     */
    void createUserAndRoles() {
        User user = new User(username: "testuser",
                password: springSecurityService.encodePassword("secret"),
                userRealName: "Test",
                email: "test@test.com",
                enabled: true,
                accountExpired: false,
                accountLocked: false,
                passwordExpired: false)
        assertNotNull(user.save(flush: true))
        assertNotNull(new AclSid(sid: user.username, principal: true).save(flush: true))
        User user2 = new User(username: "user",
                password: springSecurityService.encodePassword("verysecret"),
                userRealName: "Test2",
                email: "test2@test.com",
                enabled: true,
                accountExpired: false,
                accountLocked: false,
                passwordExpired: false)
        assertNotNull(user2.save(flush: true))
        assertNotNull(new AclSid(sid: user2.username, principal: true).save(flush: true))
        User operator = new User(username: "operator",
                password: springSecurityService.encodePassword("verysecret"),
                userRealName: "Operator",
                email: "test2@test.com",
                enabled: true,
                accountExpired: false,
                accountLocked: false,
                passwordExpired: false)
        assertNotNull(operator.save(flush: true))
        assertNotNull(new AclSid(sid: operator.username, principal: true).save(flush: true))
        User admin = new User(username: "admin",
                password: springSecurityService.encodePassword("1234"),
                userRealName: "Administrator",
                email: "admin@test.com",
                enabled: true,
                accountExpired: false,
                accountLocked: false,
                passwordExpired: false)
        assertNotNull(admin.save(flush: true))
        assertNotNull(new AclSid(sid: admin.username, principal: true).save(flush: true))
        Role userRole = new Role(authority: "ROLE_USER")
        assertNotNull(userRole.save(flush: true))
        UserRole.create(user, userRole, false)
        UserRole.create(user2, userRole, false)
        UserRole.create(admin, userRole, false)
        Role adminRole = new Role(authority: "ROLE_ADMIN")
        assertNotNull(adminRole.save(flush: true))
        UserRole.create(admin, adminRole, false)
        Role operatorRole = new Role(authority: "ROLE_OPERATOR")
        assertNotNull(operatorRole.save(flush: true))
        UserRole.create(operator, operatorRole, true)
    }
}