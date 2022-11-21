/*
 * Copyright 2011-2020 The OTP authors
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

import org.springframework.expression.Expression
import org.springframework.security.access.expression.ExpressionUtils
import org.springframework.security.access.expression.SecurityExpressionHandler
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.FilterInvocation

import de.dkfz.tbi.otp.security.SecurityService

import javax.servlet.FilterChain

class SecurityTagLib {

    static namespace = "sec"

    SecurityExpressionHandler webExpressionHandler
    SecurityService securityService

    /**
     * @attr roles REQUIRED comma-separated string
     */
    def ifAllGranted = { Map attrs, Closure body ->
        String roles = getRequiredAttribute("roles", attrs, "ifAllGranted")
        if (securityService.ifAllGranted(roles)) {
            out << body()
        }
    }

    /**
     * @attr roles REQUIRED comma-separated string
     */
    def ifNotGranted = { Map attrs, Closure body ->
        String roles = getRequiredAttribute("roles", attrs, "ifNotGranted")
        if (securityService.ifNotGranted(roles)) {
            out << body()
        }
    }

    /**
     * @attr roles REQUIRED comma-separated string
     */
    def ifAnyGranted = { Map attrs, Closure body ->
        String roles = getRequiredAttribute("roles", attrs, "ifAnyGranted")
        if (securityService.ifAnyGranted(roles)) {
            out << body()
        }
    }

    def ifLoggedIn = { Map attrs, Closure body ->
        if (securityService.loggedIn) {
            out << body()
        }
    }

    def ifNotLoggedIn = { Map attrs, Closure body ->
        if (!securityService.loggedIn) {
            out << body()
        }
    }

    def ifSwitched = { Map attrs, Closure body ->
        if (securityService.switched) {
            out << body()
        }
    }

    def ifNotSwitched = { Map attrs, Closure body ->
        if (!securityService.switched) {
            out << body()
        }
    }

    def switchedUserOriginalUsername = { Map attrs ->
        out << securityService.userSwitchInitiator.username
    }

    /**
     * @attr expression REQUIRED
     */
    def access = { Map attrs, Closure body ->
        String expression = getRequiredAttribute("expression", attrs, "access")
        if (hasAccess(expression)) {
            out << body()
        }
    }

    /**
     * @attr expression REQUIRED
     */
    def noAccess = { Map attrs, Closure body ->
        String expression = getRequiredAttribute("expression", attrs, "noAccess")
        if (!hasAccess(expression)) {
            out << body()
        }
    }

    /**
     * Disables all submit buttons wrapped inside this tag and adds a warning highlight and tooltip.
     *
     * Only disables the buttons in a real production environment. In other environments it only adds the
     * highlight but leaves the buttons enabled.
     *
     * To be used in conjunction with:
     *   - SecurityService.ensureNotSwitchedUser in the respective function to prevent direct submissions
     *   - taglib/NoSwitchedUser.js to actually disable the buttons in the GUI
     */
    def noSwitchedUser = { Map attrs, Closure body ->
        out << "<div "
        if (securityService.toBeBlockedBecauseOfSwitchedUser) {
            out << "class=\"no-switched-user\">"
            out << "<img src=\"${g.assetPath(src: "warning.png")}\"/> "
            out << "${g.message(code: "error.switchedUserDeniedException.description")}"
        } else {
            out << ">"
        }
        out << body()
        out << "</div>"
    }

    private String getRequiredAttribute(String name, Map attrs, String tag) {
        if (!attrs.containsKey(name)) {
            throwTagError("Missing attribute '${name}' for tag '${tag}'.")
        }
        return attrs.remove(name)
    }

    private static final FilterChain DUMMY_CHAIN = [
            doFilter: { req, res -> throw new UnsupportedOperationException() }
    ] as FilterChain

    private boolean hasAccess(String expressionText) {
        if (!expressionText || !securityService.loggedIn) {
            return false
        }

        Expression expression = webExpressionHandler.expressionParser.parseExpression(expressionText)
        Authentication auth = SecurityContextHolder.context.authentication
        FilterInvocation fi = new FilterInvocation(request, response, DUMMY_CHAIN)
        return ExpressionUtils.evaluateAsBoolean(expression, webExpressionHandler.createEvaluationContext(auth, fi))
    }
}
