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
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

import javax.servlet.FilterChain

class ConsoleDataSourceFilterSpec extends Specification {

    void cleanup() {
        ConsoleDataSourceContext.clear()
    }

    void "doFilter marks the request as console request while the chain runs and clears the marker afterwards"() {
        given:
        boolean consoleDuringChain = false
        FilterChain chain = Mock(FilterChain) {
            1 * doFilter(_, _) >> { consoleDuringChain = ConsoleDataSourceContext.console }
        }

        when:
        new ConsoleDataSourceFilter().doFilter(new MockHttpServletRequest("POST", "/console/execute"), new MockHttpServletResponse(), chain)

        then:
        consoleDuringChain
        !ConsoleDataSourceContext.console
    }

    void "doFilter, when the chain throws, still clears the marker"() {
        given:
        FilterChain chain = Mock(FilterChain) {
            1 * doFilter(_, _) >> { throw new IllegalStateException("script failed") }
        }

        when:
        new ConsoleDataSourceFilter().doFilter(new MockHttpServletRequest("POST", "/console/execute"), new MockHttpServletResponse(), chain)

        then:
        thrown(IllegalStateException)
        !ConsoleDataSourceContext.console
    }

    void "ORDER is before the Spring Security filter chain"() {
        expect:
        ConsoleDataSourceFilter.ORDER < SecurityProperties.DEFAULT_FILTER_ORDER
    }
}
