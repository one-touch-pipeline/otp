/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.utils

import groovy.transform.CompileDynamic
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.exceptions.SecurityContextAlreadyExistsException

/**
 * Utility to run a closure with security context of an user.
 *
 * In case a security context is already set, it will fail.
 */
class SystemUserService {

    ProcessingOptionService processingOptionService

    /**
     * run the closure with the security context having role Operator and the default system user name.
     *
     * In case a security context is already set, it will fail.
     */
    public <T> T useSystemUserAsOperator(Closure<T> closure) {
        return useSystemUser(Role.ROLE_OPERATOR, closure)
    }

    /**
     * run the closure with the security context having role Admin and the default system user name.
     *
     * In case a security context is already set, it will fail.
     */
    public <T> T useSystemUserAsAdmin(Closure<T> closure) {
        return useSystemUser(Role.ROLE_ADMIN, closure)
    }

    /**
     * run the closure with the security context having given role and the default system user name.
     *
     * In case a security context is already set, it will fail.
     */
    @CompileDynamic
    public <T> T useSystemUser(String role, Closure<T> closure) {
        String userName = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_SYSTEM_USER)
        assert userName: "no system user is defined"
        CollectionUtils.exactlyOneElement(User.findAllByUsername(userName), "Could not find user '${userName}' in the database")
        return useUser(userName, role, closure)
    }

    /**
     * run the closure with the security context having given role and the given system user name.
     *
     * In case a security context is already set, it will fail.
     */
    @CompileDynamic
    public <T> T useUser(String userName, String role, Closure<T> closure) {
        Authentication authentication = SecurityContextHolder.context.authentication
        if (authentication) {
            throw new SecurityContextAlreadyExistsException("Security context can't be created multiple times")
        }

        try {
            List<GrantedAuthority> authorities = [
                    new SimpleGrantedAuthority(role),
            ]
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(userName, "", authorities)
            SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities)

            return closure()
        } finally {
            SecurityContextHolder.context.authentication = authentication
        }
    }
}
