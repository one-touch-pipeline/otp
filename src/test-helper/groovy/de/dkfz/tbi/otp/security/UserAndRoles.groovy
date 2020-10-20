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
package de.dkfz.tbi.otp.security

import grails.plugin.springsecurity.acl.AclSid
import grails.plugin.springsecurity.acl.AclUtilService
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Principal
import de.dkfz.tbi.otp.utils.StringUtils

import static org.junit.Assert.assertNotNull

trait UserAndRoles {

    AclUtilService aclUtilService

    static final String ADMIN = "admin"
    static final String OPERATOR = "operator"
    static final String TESTUSER = "testuser"
    static final String USER = "user"

    static ProjectRole coordinator, other, pi, leadBioinformatician, bioinformatician, submitter

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
            it == ADMIN ? UserRole.create(user, adminRole) : ""
            it == OPERATOR ? UserRole.create(user, operatorRole) : ""
        }
    }

    void createAllBasicProjectRoles() {
        ProjectRole.ALL_BASIC_PROJECT_ROLES.each { String name ->
            this[StringUtils.toCamelCase(name)] = DomainFactory.createProjectRole(name: name)
        }
    }

    User getUser(String username) {
        return CollectionUtils.exactlyOneElement(User.findAllByUsername(username))
    }

    static Object doWithAnonymousAuth(final Closure closure) {
        Authentication previousAuth = SecurityContextHolder.getContext().authentication
        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken("test", new Principal(username: "Anonymous"), [new SimpleGrantedAuthority("ROLE_ANONYMOUS")])
        SecurityContextHolder.getContext().authentication = auth

        try {
            return closure.call()
        }
        finally {
            if (previousAuth == null) {
                SecurityContextHolder.clearContext()
            }
            else {
                SecurityContextHolder.getContext().authentication = previousAuth
            }
        }
    }

    static Object doAsSwitchedToUser(final String username, final Closure closure) {
        Authentication previousAuth = SecurityContextHolder.getContext().authentication
        Authentication primaryAuth = previousAuth
        if (primaryAuth == null) {
            primaryAuth = new UsernamePasswordAuthenticationToken(new Principal(username: DomainFactory.createUser().username), null, [])
            SecurityContextHolder.context.authentication = primaryAuth
        }

        List<GrantedAuthority> authorities = [new SwitchUserGrantedAuthority(SwitchUserFilter.ROLE_PREVIOUS_ADMINISTRATOR, primaryAuth)]
        Authentication switchedAuth = new UsernamePasswordAuthenticationToken(new Principal(username: username), null, authorities)
        SecurityContextHolder.context.authentication = switchedAuth

        try {
            return closure.call()
        }
        finally {
            if (previousAuth == null) {
                SecurityContextHolder.clearContext()
            }
            else {
                SecurityContextHolder.context.authentication = previousAuth
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
