package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.Principal
import grails.plugin.springsecurity.acl.*
import org.springframework.security.authentication.*
import org.springframework.security.core.*
import org.springframework.security.core.authority.*
import org.springframework.security.core.context.*
import org.springframework.security.web.authentication.switchuser.*

import static org.junit.Assert.*

trait UserAndRoles {

    AclUtilService aclUtilService

    final static String ADMIN = "admin"
    final static String OPERATOR = "operator"
    final static String TESTUSER = "testuser"
    final static String USER = "user"

    /**
     * Creates four users and their roles:
     * @li testuser
     * @li user
     * @li operator with ROLE_OPERATOR
     * @li admin with ROLE_ADMIN
     */
    void createUserAndRoles() {
        Role adminRole = new Role(authority: Role.ROLE_ADMIN)
        assertNotNull(adminRole.save(flush: true))

        Role operatorRole = new Role(authority: Role.ROLE_OPERATOR)
        assertNotNull(operatorRole.save(flush: true))

        [ADMIN, OPERATOR, TESTUSER, USER].each {
            User user = DomainFactory.createUser([username: it])
            assertNotNull(user.save(flush: true))
            assertNotNull(new AclSid(sid: user.username, principal: true).save(flush: true))
            it == ADMIN ? UserRole.create(user, adminRole, true) : ""
            it == OPERATOR ? UserRole.create(user, operatorRole, true) : ""
        }
    }

    static Object doWithAnonymousAuth(final Closure closure) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication()
        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken("test", new Principal(username: "Anonymous"), [new SimpleGrantedAuthority("ROLE_ANONYMOUS")])
        SecurityContextHolder.getContext().setAuthentication(auth)

        try {
            return closure.call()
        }
        finally {
            if (previousAuth == null) {
                SecurityContextHolder.clearContext()
            }
            else {
                SecurityContextHolder.getContext().setAuthentication(previousAuth)
            }
        }
    }

    static Object doAsSwitchedToUser(final String username, final Closure closure) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication()
        Authentication primaryAuth = previousAuth
        if (primaryAuth == null) {
            primaryAuth = new UsernamePasswordAuthenticationToken(new Principal(username: DomainFactory.createUser().username), null, [])
            SecurityContextHolder.getContext().setAuthentication(primaryAuth)
        }

        List<GrantedAuthority> authorities = [new SwitchUserGrantedAuthority(SwitchUserFilter.ROLE_PREVIOUS_ADMINISTRATOR, primaryAuth)]
        Authentication switchedAuth = new UsernamePasswordAuthenticationToken(new Principal(username: username), null, authorities)
        SecurityContextHolder.getContext().setAuthentication(switchedAuth)

        try {
            return closure.call()
        }
        finally {
            if (previousAuth == null) {
                SecurityContextHolder.clearContext()
            }
            else {
                SecurityContextHolder.getContext().setAuthentication(previousAuth)
            }
        }
    }

    void addUserWithReadAccessToProject(User user, Project project, boolean enabled = true) {
        DomainFactory.createUserProjectRole(
                user: user,
                project: project,
                enabled: enabled,
                accessToOtp: true,
        )
    }
}
