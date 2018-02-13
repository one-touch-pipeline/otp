package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import grails.plugin.springsecurity.*
import grails.plugin.springsecurity.acl.*
import org.springframework.security.acls.domain.*
import org.springframework.security.authentication.*
import org.springframework.security.core.*
import org.springframework.security.core.authority.*
import org.springframework.security.core.context.*

import static org.junit.Assert.*

trait UserAndRoles {

    AclUtilService aclUtilService

    final static String ADMIN = "admin"
    final static String OPERATOR = "operator"
    final static String TESTUSER = "testuser"
    final static String USER = "user"

    /**
     * Creates four users and their roles:
     * @li testuser with role ROLE_USER
     * @li user with role ROLE_USER
     * @li operator with ROLE_USER and ROLE_OPERATOR
     * @li admin with ROLE_USER and ROLE_ADMIN
     */
    void createUserAndRoles() {
        Role userRole = new Role(authority: Role.ROLE_USER)
        assertNotNull(userRole.save(flush: true))

        Role adminRole = new Role(authority: Role.ROLE_ADMIN)
        assertNotNull(adminRole.save(flush: true))

        Role operatorRole = new Role(authority: Role.ROLE_OPERATOR)
        assertNotNull(operatorRole.save(flush: true))

        [ADMIN, OPERATOR, TESTUSER, USER].each {
            User user = DomainFactory.createUser([username: it])
            assertNotNull(user.save(flush: true))
            assertNotNull(new AclSid(sid: user.username, principal: true).save(flush: true))
            UserRole.create(user, userRole, true)
            it == ADMIN ? UserRole.create(user, adminRole, true) : ""
            it == OPERATOR ? UserRole.create(user, operatorRole, true) : ""
        }
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

    void addUserToProject(String user, Project project) {
        Role role = new Role(authority: "GROUP_TEST_PROJECT").save(flush: true)
        UserRole.create(User.findByUsername(user), role)
        SpringSecurityUtils.doWithAuth(ADMIN) {
            aclUtilService.addPermission(project, new GrantedAuthoritySid(role.authority), BasePermission.READ)
        }
    }
}
