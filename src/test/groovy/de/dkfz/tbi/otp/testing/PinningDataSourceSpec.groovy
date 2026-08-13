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

    void "begin then rollback opens the outer transaction and tears it down"() {
        given:
        Connection connection = Mock(Connection)
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        when: "the first begin opens the outer transaction"
        pinningDataSource.begin()

        then:
        1 * target.connection >> connection
        1 * connection.setAutoCommit(false)
        pinningDataSource.active

        when: "the matching rollback discards it and releases the connection"
        pinningDataSource.rollback()

        then:
        1 * connection.rollback()
        1 * connection.setAutoCommit(true)
        1 * connection.close()
        0 * connection.rollback(_)
        !pinningDataSource.active
    }

    void "a second begin nests a savepoint instead of a new transaction, and rollback unwinds only that layer"() {
        given:
        Connection connection = Mock(Connection)
        Savepoint savepoint = Mock(Savepoint)
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        and: "the outer transaction is already open"
        target.connection >> connection
        pinningDataSource.begin()

        when: "a nested begin is issued"
        pinningDataSource.begin()

        then: "it creates a savepoint on the same connection, not a second transaction"
        1 * connection.setSavepoint() >> savepoint
        0 * target.connection
        pinningDataSource.active

        when: "the nested layer is rolled back"
        pinningDataSource.rollback()

        then: "only the savepoint is unwound; the outer transaction stays open"
        1 * connection.rollback(savepoint)
        1 * connection.releaseSavepoint(savepoint)
        0 * connection.rollback()
        0 * connection.close()
        pinningDataSource.active

        when: "the outer transaction is finally rolled back"
        pinningDataSource.rollback()

        then:
        1 * connection.rollback()
        1 * connection.close()
        !pinningDataSource.active
    }

    void "several nested savepoints unwind last-in-first-out down to the outer transaction"() {
        given:
        Connection connection = Mock(Connection)
        Savepoint firstSavepoint = Mock(Savepoint)
        Savepoint secondSavepoint = Mock(Savepoint)
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        and: "the outer transaction and two stacked savepoints are open (nesting depth 3)"
        target.connection >> connection
        connection.setSavepoint() >>> [firstSavepoint, secondSavepoint]
        pinningDataSource.begin() // depth 1: outer transaction
        pinningDataSource.begin() // depth 2: first savepoint
        pinningDataSource.begin() // depth 3: second savepoint

        when: "the innermost layer is rolled back"
        pinningDataSource.rollback()

        then: "only the most recently opened savepoint is unwound; nothing older is touched"
        1 * connection.rollback(secondSavepoint)
        1 * connection.releaseSavepoint(secondSavepoint)
        0 * connection.rollback(firstSavepoint)
        0 * connection.rollback()
        0 * connection.close()
        pinningDataSource.active

        when: "the next layer is rolled back"
        pinningDataSource.rollback()

        then: "the first savepoint is unwound, but the outer transaction still stands"
        1 * connection.rollback(firstSavepoint)
        1 * connection.releaseSavepoint(firstSavepoint)
        0 * connection.rollback()
        0 * connection.close()
        pinningDataSource.active

        when: "the outer transaction is finally rolled back"
        pinningDataSource.rollback()

        then:
        1 * connection.rollback()
        1 * connection.setAutoCommit(true)
        1 * connection.close()
        !pinningDataSource.active
    }

    void "rollback without an active transaction throws (unbalanced begin/rollback)"() {
        given:
        DataSource target = Mock(DataSource)
        PinningDataSource pinningDataSource = new PinningDataSource(target)

        when:
        pinningDataSource.rollback()

        then:
        thrown(TestTransactionException)
        0 * target._
        !pinningDataSource.active
    }
}
