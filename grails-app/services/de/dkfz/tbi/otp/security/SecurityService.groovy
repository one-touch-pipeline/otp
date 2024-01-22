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
import groovy.transform.CompileDynamic
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.authentication.AuthenticationTrustResolver
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority
import org.springframework.util.StringUtils

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.PseudoEnvironment
import de.dkfz.tbi.otp.security.user.SwitchedUserDeniedException
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * This service provides security related information about the current user.
 *
 * This code was copied from the Grails Spring Security Core plugin
 * (grails.plugin.springsecurity.SpringSecurityService and grails.plugin.springsecurity.SpringSecurityUtils)
 */
@CompileDynamic
@Transactional
class SecurityService {

    AuthenticationTrustResolver authenticationTrustResolver
    ConfigService configService
    RoleHierarchy roleHierarchy

    boolean isLoggedIn() {
        return authentication && !authenticationTrustResolver.isAnonymous(authentication)
    }

    User getCurrentUser() {
        if (!loggedIn) {
            return null
        }

        return User.createCriteria().get {
            eq("username", username, [ignoreCase: true])
            cache(true)
        }
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.context?.authentication
    }

    private Object getPrincipal() {
        return authentication?.principal
    }

    String getUsername(Object principal = this.principal) {
        if (configService.oidcEnabled && !switched) {
            DefaultOidcUser oidcUser = (DefaultOidcUser) principal
            return oidcUser ? oidcUser.userInfo.preferredUsername : ""
        }

        return principal ? principal["username"] : ""
    }

    boolean hasCurrentUserAdministrativeRoles() {
        return ifAnyGranted(Role.ADMINISTRATIVE_ROLES.collect { new SimpleGrantedAuthority(it) })
    }

    /**
     * Returns the true current user, so if the user is switched it returns not the switched user but
     * the user that switched to the current user.
     */
    User getUserSwitchInitiator() {
        if (switched) {
            SwitchUserGrantedAuthority authority = authentication.authorities
                    .find { it instanceof SwitchUserGrantedAuthority } as SwitchUserGrantedAuthority
            String name = configService.oidcEnabled ? ((DefaultOidcUser) authority?.source?.principal)?.userInfo?.preferredUsername : authority?.source?.name
            return CollectionUtils.exactlyOneElement(User.findAllByUsername(name))
        }
        return currentUser
    }

    boolean isSwitched() {
        return (roleHierarchy.getReachableGrantedAuthorities(principalAuthorities) ?: []).any { authority ->
            return authority instanceof SwitchUserGrantedAuthority
        }
    }

    private List<PseudoEnvironment> getWhitelistedEnvironments() {
        return PseudoEnvironment.values() - PseudoEnvironment.PRODUCTION
    }

    boolean isToBeBlockedBecauseOfSwitchedUser() {
        return switched && !(configService.pseudoEnvironment in whitelistedEnvironments)
    }

    void ensureNotSwitchedUser() throws SwitchedUserDeniedException {
        if (isToBeBlockedBecauseOfSwitchedUser()) {
            throw new SwitchedUserDeniedException("Operation is not allowed for switched users")
        }
    }

    /**
     * Check if the current user has all of the specified roles.
     * @param roles a comma-delimited list of role names
     * @return <code>true</code> if the user is authenticated and has all the roles
     */
    boolean ifAllGranted(String roles) {
        return ifAllGranted(parseAuthoritiesString(roles))
    }

    boolean ifAllGranted(Collection<? extends GrantedAuthority> roles) {
        return authoritiesToRoles(findInferredAuthorities(principalAuthorities)).containsAll(authoritiesToRoles(roles))
    }

    /**
     * Check if the current user has none of the specified roles.
     * @param roles a comma-delimited list of role names
     * @return <code>true</code> if the user is authenticated and has none the roles
     */
    boolean ifNotGranted(String roles) {
        return ifNotGranted(parseAuthoritiesString(roles))
    }

    boolean ifNotGranted(Collection<? extends GrantedAuthority> roles) {
        return !retainAll(findInferredAuthorities(principalAuthorities), roles)
    }

    /**
     * Check if the current user has any of the specified roles.
     * @param roles a comma-delimited list of role names
     * @return <code>true</code> if the user is authenticated and has any the roles
     */
    boolean ifAnyGranted(String roles) {
        return ifAnyGranted(parseAuthoritiesString(roles))
    }

    boolean ifAnyGranted(Collection<? extends GrantedAuthority> roles) {
        return retainAll(findInferredAuthorities(principalAuthorities), roles)
    }

    /**
     * Split the role names and create {@link GrantedAuthority}s for each.
     * @param roleNames comma-delimited role names
     * @return authorities (possibly empty)
     */
    private List<GrantedAuthority> parseAuthoritiesString(String roleNames) {
        List<GrantedAuthority> requiredAuthorities = []
        for (String auth in StringUtils.commaDelimitedListToStringArray(roleNames)) {
            auth = auth.trim()
            if (auth) {
                requiredAuthorities << new SimpleGrantedAuthority(auth)
            }
        }

        return requiredAuthorities
    }

    /**
     * Get the current user's authorities.
     * @return a list of authorities (empty if not authenticated).
     */
    Collection<GrantedAuthority> getPrincipalAuthorities() {
        if (!authentication || !authentication.authorities) {
            return Collections.emptyList()
        }

        Collection<? extends GrantedAuthority> authorities = authentication.authorities
        if (authorities == null) {
            return Collections.emptyList()
        }

        // remove the fake role if it's there
        Collection<GrantedAuthority> copy = ([] + authorities) as Collection<GrantedAuthority>
        for (Iterator<GrantedAuthority> iter = copy.iterator(); iter.hasNext();) {
            if (NO_ROLE == iter.next().authority) {
                iter.remove()
            }
        }

        return copy
    }

    /**
     * Used to ensure that all authenticated users have at least one granted authority to work
     * around Spring Security code that assumes at least one. By granting this non-authority,
     * the user can't do anything but gets past the somewhat arbitrary restrictions.
     */
    private final static String NO_ROLE = 'ROLE_NO_ROLES'

    private Collection<? extends GrantedAuthority> findInferredAuthorities(Collection<GrantedAuthority> granted) {
        return roleHierarchy.getReachableGrantedAuthorities(granted) ?: (Collections.emptyList() as Collection<? extends GrantedAuthority>)
    }

    /**
     * Find authorities in <code>granted</code> that are also in <code>required</code>.
     * @param granted the granted authorities (a collection or array of {@link GrantedAuthority}).
     * @param required the required authorities (a collection or array of {@link GrantedAuthority}).
     * @return the authority names
     */
    private Set<String> retainAll(Collection<? extends GrantedAuthority> granted, Collection<? extends GrantedAuthority> required) {
        Set<String> grantedRoles = authoritiesToRoles(granted)
        grantedRoles.retainAll(authoritiesToRoles(required))
        return grantedRoles
    }

    /**
     * Extract the role names from authorities.
     * @param authorities the authorities (a collection or array of {@link GrantedAuthority}).
     * @return the names
     */
    private Set<String> authoritiesToRoles(Collection<? extends GrantedAuthority> authorities = []) {
        return authorities.collect { GrantedAuthority authority ->
            String authorityName = authority.authority
            assert authorityName != null,
                    "Cannot process GrantedAuthority objects which return null from getAuthority() - attempting to process ${authority}"
            return authorityName
        } as Set
    }
}
