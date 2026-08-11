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
 * Feature-flagged endpoints used solely to isolate end-to-end (Cypress) tests from one another by opening a database
 * transaction at the start of a spec file and rolling it back at the end (see {@link TestTransactionService}).
 *
 * <p>Every action returns HTTP {@code 403} unless {@code otp.testing.endpoints.enabled} is explicitly set to
 * {@code true}, so these endpoints are never usable in production.</p>
 */
@PreAuthorize('permitAll')
class TestingController {

    static allowedMethods = [
            begin   : "POST",
            rollback: "POST",
    ]

    ConfigService configService
    TestTransactionService testTransactionService

    /** Open a fresh, uncommitted database transaction; everything written until {@link #rollback} is undoable. */
    def begin() {
        if (denyWhenDisabled()) {
            return
        }
        testTransactionService.begin()
        render([status: "ok", action: "begin"] as JSON)
    }

    /** Roll the transaction opened by {@link #begin} back, resetting the database to its pre-spec state. */
    def rollback() {
        if (denyWhenDisabled()) {
            return
        }
        testTransactionService.rollback()
        render([status: "ok", action: "rollback"] as JSON)
    }

    private boolean denyWhenDisabled() {
        if (!configService.testingEndpointsEnabled) {
            render(status: HttpStatus.FORBIDDEN.value())
            return true
        }
        return false
    }
}
