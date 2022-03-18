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
package de.dkfz.tbi.otp.utils

import grails.util.Environment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.security.Role

import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Filter to give a configured user access to the web application in development mode.
 *
 * The idea behind this backdoor is that in development mode the developer does not have
 * to login. For that the filter creates a UsernamePasswordAuthenticationToken which is
 * authenticated and has the authority ROLE_ADMIN. With other words: the
 * filter allows full access to the application.
 *
 * As that is quite a security risk the filter ensures that it is only used in development
 * mode. Nevertheless the configuration should also ensure that the filter is only added in
 * development mode, so that it does not even get loaded in production mode. Furthermore it
 * would be best to drop the compiled class from the war file.
 *
 * The filter is controlled by the config options {@link OtpProperty#DEVEL_USE_BACKDOOR} and
 * {@link OtpProperty#DEVEL_BACKDOOR_USER}.
 * The first one controls whether the filter is enabled at all, the second one the user to use.
 */
@Component("backdoorFilter")
class BackdoorFilter extends GenericFilterBean {

    @Autowired
    ConfigService configService
    /**
     * Ensures, that the filter is only applied once per request
     */
    static final String FILTER_APPLIED = "__otp_security_caf_applied"
    /**
     * The filter injects a user with the ROLE_ADMIN authority
     */
    private final List<GrantedAuthority> authorities = [
            new SimpleGrantedAuthority(Role.ROLE_ADMIN),
    ]

    @Override
    void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req
        HttpServletResponse response = (HttpServletResponse) res

        if (request.getAttribute(FILTER_APPLIED)) {
            // ensure that filter is only applied once per request
            chain.doFilter(request, response)
            return
        }
        HttpRequestResponseHolder holder = new HttpRequestResponseHolder(request, response)

        if (!Environment.developmentMode && Environment.current != Environment.TEST) {
            // if we are not in development or testing mode, the filter is inactive
            chain.doFilter(request, response)
            return
        }
        if (!configService.useBackdoor() || !configService.backdoorUser) {
            // backdoor filter disabled by configuration
            chain.doFilter(request, response)
            return
        }

        try {
            Authentication authentication = SecurityContextHolder.context.authentication
            if (Environment.current == Environment.TEST) {
                Principal userDetails = new Principal(configService.backdoorUser, "OTP TEST", authorities)
                SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
            } else if (!authentication || !authentication.authenticated) {
                Principal userDetails = new Principal(configService.backdoorUser, "OTP Developer", authorities)
                SecurityContextHolder.context.authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
            }
            chain.doFilter(holder.request, holder.response)
        } finally {
            request.removeAttribute(FILTER_APPLIED)
        }
    }
}
