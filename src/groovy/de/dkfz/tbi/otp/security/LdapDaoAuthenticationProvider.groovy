package de.dkfz.tbi.otp.security

import groovy.transform.*
import org.springframework.beans.factory.annotation.*
import org.springframework.security.authentication.*
import org.springframework.security.core.*
import org.springframework.security.core.userdetails.*
import org.springframework.security.ldap.authentication.*
import org.springframework.stereotype.*

/**
 * This provider checks whether the user exists in the database and can be authenticated by LDAP.
 * It also does the default pre-authentication checks, but does not validate the password from the database.
 * If successful, the LDAP authentication object is returned
 */
@Component("ldapDaoAuthenticationProvider")
@CompileStatic
class LdapDaoAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    LdapAuthenticationProvider ldapAuthProvider
    @Autowired
    UserDetailsService userDetailsService

    @Override
    Authentication authenticate(Authentication authentication) throws AuthenticationException {
        assert authentication

        // authenticate with LDAP first, so that it can't be found out whether an account
        // is locked/disabled/expired by somebody else than the account owner
        Authentication ldapAuth = ldapAuthProvider.authenticate(authentication)

        UserDetails user = userDetailsService.loadUserByUsername(authentication.getName())
        if (user == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation")
        }

        if (!user.isAccountNonLocked()) {
            throw new LockedException("User account is locked")
        } else if (!user.isEnabled()) {
            throw new DisabledException("User is disabled")
        } else if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException( "User account has expired")
        }

        return ldapAuth
    }

    @Override
    boolean supports(Class<? extends Object> aClass) {
        ldapAuthProvider.supports(aClass)
    }
}
