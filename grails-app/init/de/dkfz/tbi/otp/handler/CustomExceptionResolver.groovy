/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.handler

import org.grails.web.errors.GrailsExceptionResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.otp.security.SecurityService

import javax.servlet.http.HttpServletRequest
import java.lang.reflect.InvocationTargetException

class CustomExceptionResolver extends GrailsExceptionResolver {
    @Autowired
    SecurityService securityService

    @Override
    protected void filterStackTrace(Exception e) {
        // Don't pollute Logs with Stacktrace from AccessDeniedExceptions
        if (e instanceof InvocationTargetException && e.cause instanceof AccessDeniedException) {
            return
        }
        super.filterStackTrace(e)
    }

    @Override
    protected void logStackTrace(Exception e, HttpServletRequest request) {
        // Don't pollute Logs with Stacktrace from AccessDeniedExceptions, just log information which request and which user got denied
        if (e instanceof InvocationTargetException && e.cause instanceof AccessDeniedException) {
            String causeName = getRootCause(e).class.simpleName
            String requestDescription = "[${request.method.toUpperCase()}] ${request.requestURI}"
            String userDescription = "for user ${securityService.currentUser}"
            String errorMessage = "${causeName} occurred when processing request: ${requestDescription} ${userDescription}"
            LOG.error(errorMessage)
            return
        }
        super.logStackTrace(e, request)
    }
}
