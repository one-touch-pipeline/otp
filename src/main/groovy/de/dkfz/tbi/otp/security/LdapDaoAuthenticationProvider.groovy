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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.security.user.identityProvider.LdapService

/**
 * This provider checks whether the user exists in the database and can be authenticated by LDAP.
 * It also does the default pre-authentication checks, but does not validate the password from the database.
 * If successful, the LDAP authentication object is returned
 */
@Component
class LdapDaoAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    UserCreatingUserDetailsService userDetailsService

    @Autowired
    ConfigService configService

    @Autowired
    LdapService ldapService

    @Override
    Authentication authenticate(Authentication authentication) throws AuthenticationException {
        assert authentication

        if (authentication.name != authentication.name.toLowerCase()) {
            throw new UsernameNotFoundException("Username must be lower case")
        }

        UserDetails user = userDetailsService.loadUserByUsername(authentication.name)
        if (user == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation")
        }

        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled")
        }

        Boolean authenticated = ldapService.authenticateUser(authentication.name, authentication.credentials.toString())
        if (authenticated) {
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(authentication.name,
                    authentication.credentials.toString(), user.authorities)
            return new UsernamePasswordAuthenticationToken(userDetails, authentication.credentials.toString(), user.authorities)
        }

        return null
    }

    @Override
    boolean supports(Class<?> authentication) {
        return authentication == UsernamePasswordAuthenticationToken
    }
}
