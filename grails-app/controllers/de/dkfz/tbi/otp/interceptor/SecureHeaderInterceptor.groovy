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
package de.dkfz.tbi.otp.interceptor

import groovy.transform.CompileStatic

/**
 * Set HTTP headers improving security
 * Based on the recommendations of <a href="https://observatory.mozilla.org/analyze.html?host=otp.dkfz.de&third-party=false">
 *     Mozilla Observatory test results</a>
 */
@CompileStatic
class SecureHeaderInterceptor {

    SecureHeaderInterceptor() {
        matchAll()
    }

    @Override
    boolean before() { return true }

    @Override
    boolean after() {
        response.setHeader("Content-Security-Policy", [
                "default-src https: 'self'",
                "script-src https: 'unsafe-inline' 'self'",
                "style-src https: 'unsafe-inline' 'self'",
                "img-src https: data: 'self'",
                "base-uri 'self'",
                "form-action 'self'",
                "frame-ancestors 'none'",
                "plugin-types application/pdf",
        ].join("; "))
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("X-Frame-Options", "DENY")
        response.setHeader("X-XSS-Protection", "1; mode=block")
        return true
    }

    @Override
    void afterView() {
        // no-op
    }
}
