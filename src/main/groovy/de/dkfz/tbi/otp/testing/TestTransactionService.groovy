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

/**
 * Controls the pinned database transaction used to isolate end-to-end (Cypress) tests. Delegates to the
 * {@link PinningDataSource} installed by {@link PinningDataSourceConnectionSourceFactory}.
 *
 * <p>This is deliberately NOT a Grails service in {@code grails-app/services}: it is a plain bean registered
 * conditionally in {@code resources.groovy}, so it only exists when {@code otp.testing.endpoints.enabled=true} in the
 * development environment. Callers must still guard on that flag (as {@code TestingController} does) before invoking
 * these methods.</p>
 *
 * <p>It must NOT be transactional: {@link #beginPage()} / {@link #beginTest()} operate on the raw pinned JDBC connection
 * directly, outside GORM's transaction handling.</p>
 */
class TestTransactionService {

    /**
     * Page (spec) level: reset any transaction left open by a previous spec and open a fresh, uncommitted transaction
     * that all subsequent HTTP requests will run within, until the next {@link #beginPage()}.
     */
    void beginPage() {
        pinningDataSource.beginPage()
    }

    /**
     * Test level: on top of the page transaction, reset to a single savepoint so each test starts from the same
     * page-seed state. Requires an active page transaction (see {@link #beginPage()}).
     */
    void beginTest() {
        pinningDataSource.beginTest()
    }

    private PinningDataSource getPinningDataSource() {
        PinningDataSource pinningDataSource = PinningDataSource.activeInstance
        if (!pinningDataSource) {
            throw new TestingEndpointsException("The testing data source is not installed. This should only be reachable " +
                    "when 'otp.testing.endpoints.enabled' is true.")
        }
        return pinningDataSource
    }
}
