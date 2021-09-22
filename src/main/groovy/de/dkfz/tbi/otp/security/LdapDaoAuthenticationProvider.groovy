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

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.stereotype.Component

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

        if (authentication.name != authentication.name.toLowerCase()) {
            throw new UsernameNotFoundException("Username must be lower case")
        }

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
            throw new AccountExpiredException("User account has expired")
        }

        return ldapAuth
    }

    @Override
    boolean supports(Class<? extends Object> aClass) {
        ldapAuthProvider.supports(aClass)
    }
}
