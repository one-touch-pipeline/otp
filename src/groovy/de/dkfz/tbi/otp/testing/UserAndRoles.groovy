package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.security.*
import grails.plugin.springsecurity.*
import grails.plugin.springsecurity.acl.*
import org.springframework.security.authentication.*
import org.springframework.security.core.*
import org.springframework.security.core.authority.*
import org.springframework.security.core.context.*

import static org.junit.Assert.*

trait UserAndRoles {

    SpringSecurityService springSecurityService
    AclUtilService aclUtilService

    final static String ADMIN = "admin"
    final static String OPERATOR = "operator"
    final static String TESTUSER = "testuser"
    final static String USER = "user"

    /**
     * Creates four users and their roles:
     * @li testuser with password secret and role ROLE_USER
     * @li user with password verysecret and role ROLE_USER
     * @li operator with password verysecret and ROLE_OPERATOR and ROLE_USER
     * @li admin with password 1234 and ROLE_ADMIN and ROLE_USER
     */
    void createUserAndRoles() {
        User user = new User(username: TESTUSER,
                password: springSecurityService.encodePassword("secret"),
                userRealName: "Test",
                email: "test@test.com",
                enabled: true,
                accountExpired: false,
                accountLocked: false,
                passwordExpired: false)
        assertNotNull(user.save(flush: true))
        assertNotNull(new AclSid(sid: user.username, principal: true).save(flush: true))
        User user2 = new User(username: USER,
                password: springSecurityService.encodePassword("verysecret"),
                userRealName: "Test2",
                email: "test2@test.com",
                enabled: true,
                accountExpired: false,
                accountLocked: false,
                passwordExpired: false)
        assertNotNull(user2.save(flush: true))
        assertNotNull(new AclSid(sid: user2.username, principal: true).save(flush: true))
        User operator = new User(username: OPERATOR,
                password: springSecurityService.encodePassword("verysecret"),
                userRealName: "Operator",
                email: "test2@test.com",
                enabled: true,
                accountExpired: false,
                accountLocked: false,
                passwordExpired: false)
        assertNotNull(operator.save(flush: true))
        assertNotNull(new AclSid(sid: operator.username, principal: true).save(flush: true))
        User admin = new User(username: ADMIN,
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

    static Object doWithAnonymousAuth(@SuppressWarnings("rawtypes") final Closure closure) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken("test", "Anonymous", [new SimpleGrantedAuthority("ROLE_ANONYMOUS")])
        SecurityContextHolder.getContext().setAuthentication(auth)

        try {
            return closure.call();
        }
        finally {
            if (previousAuth == null) {
                SecurityContextHolder.clearContext();
            }
            else {
                SecurityContextHolder.getContext().setAuthentication(previousAuth);
            }
        }
    }
}
