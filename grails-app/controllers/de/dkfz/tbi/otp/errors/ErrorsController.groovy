/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.errors

import org.springframework.security.access.annotation.Secured
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.utils.ExceptionUtils
import de.dkfz.tbi.otp.utils.RequestUtilService

import javax.servlet.http.HttpServletResponse

@Slf4j
@Secured('isFullyAuthenticated()')
class ErrorsController {

    RequestUtilService requestUtilService
    SecurityService securityService

    static List<String> allHttpMethods = ["GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH"]

    static allowedMethods = [
            error403                   : "GET",
            error404                   : "GET",
            error405                   : allHttpMethods,
            error500                   : allHttpMethods,
            noProject                  : "GET",
            switchedUserDeniedException: "GET",
    ]

    def error403 = {
        response.status = HttpServletResponse.SC_FORBIDDEN
        if (requestUtilService.isAjax(request)) {
            return render(securityService.isLoggedIn().toString())
        }
        return [authenticated: securityService.isLoggedIn()]
    }

    def error404 = {
        response.status = HttpServletResponse.SC_NOT_FOUND
        if (requestUtilService.isAjax(request)) {
            return render(request.forwardURI)
        }
        return [notFound: request.forwardURI]
    }

    def error405 = {
        response.status = HttpServletResponse.SC_METHOD_NOT_ALLOWED
        return [method: request.method]
    }

    def error500 = {
        Throwable throwable = request.exception
        String stackTrace = ''
        if (throwable) {
            stackTrace = ExceptionUtils.getStackTrace(throwable)
        }
        String digest = stackTrace.encodeAsMD5()
        log.error("Displaying exception with digest ${digest}.")
        response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        if (requestUtilService.isAjax(request)) {
            render(digest)
        } else {
            return [
                    code      : digest,
                    message   : throwable?.message,
                    stackTrace: stackTrace,
            ]
        }
    }

    def noProject() {
        response.status = HttpServletResponse.SC_NOT_FOUND
        return [:]
    }

    def switchedUserDeniedException() {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        return [
                exception: request.getAttribute('exception')
        ]
    }
}
