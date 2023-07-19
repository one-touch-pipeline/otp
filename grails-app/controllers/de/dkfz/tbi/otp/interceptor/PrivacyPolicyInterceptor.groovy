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
package de.dkfz.tbi.otp.interceptor

import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.security.user.UserService

@CompileStatic
class PrivacyPolicyInterceptor {
    UserService userService

    int order = 0

    static final String FILTER_APPLIED = "__otp_privacy_policy_filter_applied"

    PrivacyPolicyInterceptor() {
        matchAll()
                .except(controller: "privacyPolicy", action: "accept")
                .except(controller: "logout", action: "index")
                .except(controller: "auth", action: "logout")
                .except(controller: "info", action: "newsBanner")
    }

    @Override
    boolean before() {
        // ensure that filter is only applied once per request
        if (request.getAttribute(FILTER_APPLIED)) {
            return true
        }
        request.setAttribute(FILTER_APPLIED, true)

        if (!userService.privacyPolicyAccepted) {
            forward(controller: "privacyPolicy", action: "index")
            return false
        }
        return true
    }

    @Override
    boolean after() { true }

    @Override
    void afterView() {
        // no-op
    }
}
