/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.config.datasource

import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Marks requests executing a script of the groovy web console, so that all their database work uses the restricted
 * console pool (see {@link ConsoleRoutingDataSource}). The marker is always removed afterwards, because Tomcat reuses
 * its request threads.
 *
 * <p>Registered in {@code resources.groovy} for {@value #CONSOLE_URL_PATTERN} with {@link #ORDER}, i.e. before the
 * Spring Security filter chain, so no connection can be taken from the default pool earlier in the request. The pattern
 * covers the whole console and not only {@value #CONSOLE_EXECUTE_PATH}, because Grails also routes variants like
 * {@code /console/execute/} to the execute action.</p>
 */
class ConsoleDataSourceFilter extends OncePerRequestFilter {

    /** The path of the groovy web console action executing a script, relative to the context path. */
    static final String CONSOLE_EXECUTE_PATH = "/console/execute"

    /** The servlet URL pattern of all requests of the groovy web console. */
    static final String CONSOLE_URL_PATTERN = "/console/*"

    /** Runs before the Spring Security filter chain. */
    static final int ORDER = SecurityProperties.DEFAULT_FILTER_ORDER - 10

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ConsoleDataSourceContext.markConsole()
        try {
            filterChain.doFilter(request, response)
        } finally {
            ConsoleDataSourceContext.clear()
        }
    }
}
