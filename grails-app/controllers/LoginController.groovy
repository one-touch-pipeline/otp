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

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.security.authentication.*
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.WebAttributes
import org.springframework.security.web.authentication.session.SessionAuthenticationException

import de.dkfz.tbi.otp.security.FailedToCreateUserException

@SuppressWarnings('ClassNameSameAsSuperclass') //this affects the URL of the superclass
@Secured('permitAll')
class LoginController extends grails.plugin.springsecurity.LoginController {

    static allowedMethods = [
            auth: "GET",
            authfail: "GET",
    ]

    /** Show the login page. */
    def auth(LoginCommand cmd) {
        assert cmd.validate()

        if (springSecurityService.isLoggedIn()) {
            redirect uri: cmd.target ?: conf.successHandler.defaultTargetUrl
            return [:]
        }

        String postUrl = "${request.contextPath}${conf.apf.filterProcessesUrl}"
        return [
                target             : cmd.target,
                username           : flash.username,
                postUrl            : postUrl,
                rememberMeParameter: conf.rememberMe.parameter,
                usernameParameter  : conf.apf.usernameParameter,
                passwordParameter  : conf.apf.passwordParameter,
                gspLayout          : conf.gsp.layoutAuth,
        ]
    }

    /** Callback after a failed login. Redirects to the auth page with a warning message. */
    @Override
    def authfail() {
        String msg = ''
        AuthenticationException exception = session[WebAttributes.AUTHENTICATION_EXCEPTION] as AuthenticationException
        if (exception) {
            switch (exception.class) {
                case AccountExpiredException: msg = g.message(code: "springSecurity.errors.login.expired"); break
                case CredentialsExpiredException: msg = g.message(code: "springSecurity.errors.login.passwordExpired"); break
                case DisabledException: msg = g.message(code: "springSecurity.errors.login.disabled"); break
                case LockedException: msg = g.message(code: "springSecurity.errors.login.locked"); break
                case SessionAuthenticationException: msg = g.message(code: "springSecurity.errors.login.max.sessions.exceeded"); break
                case FailedToCreateUserException: msg = exception.message; break
                default: msg = g.message(code: "springSecurity.errors.login.fail"); break
            }
        }

        if (springSecurityService.isAjax(request)) {
            render([error: msg] as JSON)
        } else {
            flash.message = msg
            flash.username = session.getAttribute(SpringSecurityUtils.SPRING_SECURITY_LAST_USERNAME_KEY)
            redirect action: 'auth', params: params
        }
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
