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
package de.dkfz.tbi.otp.testing

import grails.converters.JSON
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.config.ConfigService

/**
 * Feature-flagged endpoints used solely to isolate end-to-end (Cypress) tests from one another via a rolled-back
 * database transaction (see {@link TestTransactionService}). There are two reset-first levels: {@code beginPage} opens a
 * fresh transaction for a whole spec file (resetting any left open by a previous spec), and {@code beginTest} resets to
 * a per-test savepoint on top of it. Cleanup happens on the next {@code begin*} call, so there is no separate rollback
 * endpoint.
 *
 * <p>Every action returns HTTP {@code 403} unless {@code otp.testing.endpoints.enabled} is explicitly set to
 * {@code true}, so these endpoints are never usable in production.</p>
 */
@PreAuthorize('permitAll')
class TestingController {

    static allowedMethods = [
            beginPage: "POST",
            beginTest: "POST",
    ]

    ConfigService configService
    TestTransactionService testTransactionService

    /** Page (spec) level: reset any transaction left open by a previous spec and open a fresh, uncommitted one. */
    def beginPage() {
        if (denyWhenDisabled()) {
            return
        }
        testTransactionService.beginPage()
        render([status: "ok", action: "beginPage"] as JSON)
    }

    /** Test level: reset to a per-test savepoint on top of the page transaction, so each test starts from the page seed. */
    def beginTest() {
        if (denyWhenDisabled()) {
            return
        }
        testTransactionService.beginTest()
        render([status: "ok", action: "beginTest"] as JSON)
    }

    private boolean denyWhenDisabled() {
        if (!configService.testingEndpointsEnabled) {
            render(status: HttpStatus.FORBIDDEN.value())
            return true
        }
        return false
    }
}
