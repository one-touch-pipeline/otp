/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp

import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.*
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.session.SessionAuthenticationException

import de.dkfz.tbi.otp.security.FailedToCreateUserException
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.utils.RequestUtilService

@PreAuthorize("permitAll()")
class LoginController {

    RequestUtilService requestUtilService
    SecurityService securityService

    final static String LAST_USERNAME_KEY = "LAST_USERNAME_KEY"
    final static String LAST_TARGET_KEY = "LAST_TARGET_KEY"

    static allowedMethods = [
            index   : "GET",
            authfail: "GET",
    ]

    def index(LoginCommand cmd) {
        assert cmd.validate()

        if (securityService.isLoggedIn()) {
            redirect uri: cmd.target ?: "/"
            return [:]
        }

        String postUrl = "${request.contextPath}/authenticate"
        return [
                target             : cmd.target,
                username           : flash.username,
                postUrl            : postUrl,
        ]
    }

    /** Callback after a failed login. Redirects to the auth page with a warning message. */
    def authfail() {
        String msg = ''
        AuthenticationException exception = session[AUTHENTICATION_EXCEPTION] as AuthenticationException
        if (exception) {
            switch (exception.class) {
                case AccountExpiredException: msg = g.message(code: "security.errors.login.expired"); break
                case CredentialsExpiredException: msg = g.message(code: "security.errors.login.passwordExpired"); break
                case DisabledException: msg = g.message(code: "security.errors.login.disabled"); break
                case LockedException: msg = g.message(code: "security.errors.login.locked"); break
                case SessionAuthenticationException: msg = g.message(code: "security.errors.login.max.sessions.exceeded"); break
                case FailedToCreateUserException: msg = exception.message; break
                default: msg = g.message(code: "security.errors.login.fail"); break
            }
        }

        if (requestUtilService.isAjax(request)) {
            render([error: msg] as JSON)
            return
        }

        flash.message = msg
        flash.username = session[LAST_USERNAME_KEY]
        String target = session[LAST_TARGET_KEY]
        redirect uri: "${request.contextPath}/login", params: [target: target ?: ""]
    }
}

class LoginCommand {
    String target

    static constraints = {
        target nullable: true, validator: { String val ->
            !val || val.startsWith("/")
        }
    }
}
