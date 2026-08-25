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

import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.Savepoint

class PinningDataSourceSpec extends Specification {

    void "beginPage opens a fresh, pinned outer transaction with auto-commit disabled"() {
        given:
        Connection connection = Mock(Connection)
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        when:
        pinningDataSource.beginPage()

        then:
        1 * target.connection >> connection
        1 * connection.setAutoCommit(false)
        pinningDataSource.active
    }

    void "beginPage while already active discards the previous transaction first (reset-first)"() {
        given:
        Connection previousConnection = Mock(Connection)
        Connection freshConnection = Mock(Connection)
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        and: "a page transaction is already open on the previous connection"
        target.connection >>> [previousConnection, freshConnection]
        pinningDataSource.beginPage()

        when: "beginPage is called again"
        pinningDataSource.beginPage()

        then: "the previous connection is rolled back and released before a fresh one is opened"
        1 * previousConnection.rollback()
        1 * previousConnection.setAutoCommit(true)
        1 * previousConnection.close()
        1 * freshConnection.setAutoCommit(false)
        pinningDataSource.active
    }

    void "beginTest creates a savepoint on the first call and rolls back to it (reused) on the next"() {
        given:
        Connection connection = Mock(Connection)
        Savepoint savepoint = Mock(Savepoint)
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        and: "a page transaction is open"
        target.connection >> connection
        pinningDataSource.beginPage()

        when: "the first beginTest opens the test savepoint"
        pinningDataSource.beginTest()

        then:
        1 * connection.setSavepoint() >> savepoint
        0 * connection.rollback(_)
        pinningDataSource.active

        when: "a second beginTest resets to the same savepoint without creating a new one"
        pinningDataSource.beginTest()

        then:
        1 * connection.rollback(savepoint)
        0 * connection.setSavepoint()
        pinningDataSource.active
    }

    void "beginPage clears the test savepoint, so the next beginTest opens a new one instead of reusing the discarded one"() {
        given:
        Connection connection = Mock(Connection)
        Savepoint firstSavepoint = Mock(Savepoint)
        Savepoint secondSavepoint = Mock(Savepoint)
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        and: "a page transaction with an open test savepoint"
        target.connection >> connection
        pinningDataSource.beginPage()

        when: "the first test savepoint is opened"
        pinningDataSource.beginTest()

        then:
        1 * connection.setSavepoint() >> firstSavepoint

        when: "the page transaction is reset"
        pinningDataSource.beginPage()

        then: "the previous page connection is discarded"
        1 * connection.rollback()
        1 * connection.close()

        when: "a test is opened again on the fresh page"
        pinningDataSource.beginTest()

        then: "a brand-new savepoint is created rather than rolling back to the discarded one"
        1 * connection.setSavepoint() >> secondSavepoint
        0 * connection.rollback(firstSavepoint)
        pinningDataSource.active
    }

    void "beginTest without an active page transaction throws"() {
        given:
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        when:
        pinningDataSource.beginTest()

        then:
        thrown(TestTransactionException)
        0 * target._
        !pinningDataSource.active
    }
}
