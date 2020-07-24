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

import grails.plugin.springsecurity.SpringSecurityService

import javax.servlet.http.HttpServletResponse

class ErrorsController {
    SpringSecurityService springSecurityService

    def error403 = {
        response.setStatus HttpServletResponse.SC_FORBIDDEN
        if (springSecurityService.isAjax(request)) {
            render springSecurityService.isLoggedIn().toString()
            return
        } else {
            return [authenticated: springSecurityService.isLoggedIn()]
        }
    }

    def error404 = {
        if (springSecurityService.isAjax(request)) {
            render request.forwardURI
            return
        } else {
            return [notFound: request.forwardURI]
        }
    }

    def error405 = {
        return [method: request.method]
    }

    def error500 = {
        def exception = request.getAttribute('exception')
        String stackTrace = ''
        if (exception) {
            StringWriter stackTraceBuffer = new StringWriter()
            PrintWriter stackTracePrintWriter = new PrintWriter(stackTraceBuffer)
            exception.printStackTrace(stackTracePrintWriter)
            stackTrace = stackTraceBuffer.toString()
        }
        String digest = stackTrace.encodeAsMD5()
        log.error("Displaying exception with digest ${digest}.")
        if (springSecurityService.isAjax(request)) {
            render digest
            return
        } else {
            [
                    code: digest,
                    message: exception?.message,
                    stackTrace: stackTrace,
            ]
        }
    }

    def noProject() { }

    def switchedUserDeniedException() {
        return [
                exception: request.getAttribute('exception')
        ]
    }
}
