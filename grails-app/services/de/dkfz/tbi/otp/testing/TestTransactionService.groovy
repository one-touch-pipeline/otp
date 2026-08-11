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
 * {@link PinningDataSource} installed by {@link TestTransactionDataSourceBeanPostProcessor}.
 *
 * <p>Beans are only present when {@code otp.testing.endpoints.enabled=true}; callers must guard on that flag (as
 * {@code TestingController} does) before invoking these methods.</p>
 *
 * Not {@code @Transactional}: it must operate on the raw pinned JDBC connection directly, outside GORM's transaction
 * handling.
 */
class TestTransactionService {

    // Must never run inside a GORM transaction: while a test transaction is active it would itself borrow the pinned
    // connection, which rollback() then rolls back and closes underneath it.
    static boolean transactional = false

    /** Open a fresh, uncommitted test transaction that all subsequent HTTP requests will run within. */
    void begin() {
        pinningDataSource.begin()
    }

    /** Discard everything written since {@link #begin()}, resetting the database to its pre-spec state. */
    void rollback() {
        pinningDataSource.rollback()
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
