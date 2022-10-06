/* Copyright 2006-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dkfz.tbi.otp.security

import grails.gorm.transactions.Transactional
import org.springframework.security.authentication.AuthenticationTrustResolver
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

import de.dkfz.tbi.otp.security.user.UserService

/**
 * This service provides security related information about the current user.
 *
 * This code was copied from the Grails Spring Security Core plugin
 * (grails.plugin.springsecurity.SpringSecurityService and grails.plugin.springsecurity.SpringSecurityUtils)
 */
@Transactional
class SecurityService {

    AuthenticationTrustResolver authenticationTrustResolver
    UserService userService

    boolean isLoggedIn() {
        return authentication && !authenticationTrustResolver.isAnonymous(authentication)
    }

    User getCurrentUser() {
        if (!loggedIn) {
            return null
        }

        return User.createCriteria().get {
            eq("username", principal["username"] as String, [ignoreCase: true])
            cache(true)
        }
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.context?.authentication
    }

    private getPrincipal() {
        return authentication?.principal
    }

    private List<Role> getRolesOfCurrentUser() {
        return userService.getRolesOfUser(currentUser)
    }

    private boolean checkRolesContainsAdministrativeRole(List<Role> roles) {
        return roles.any {
            it.authority in Role.ADMINISTRATIVE_ROLES
        }
    }

    boolean hasCurrentUserAdministrativeRoles() {
        return checkRolesContainsAdministrativeRole(rolesOfCurrentUser)
    }
}
