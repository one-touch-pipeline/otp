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
package de.dkfz.tbi.otp.config.datasource

import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection

class ConsoleRoutingDataSourceSpec extends Specification {

    void cleanup() {
        ConsoleDataSourceContext.clear()
    }

    void "getConnection, outside of the console, uses the default data source"() {
        given:
        Connection connection = Mock(Connection)
        DataSource defaultDataSource = Mock(DataSource)
        DataSource consoleDataSource = Mock(DataSource)
        ConsoleRoutingDataSource routingDataSource = new ConsoleRoutingDataSource(defaultDataSource, consoleDataSource)

        when:
        Connection result = routingDataSource.connection

        then:
        1 * defaultDataSource.connection >> connection
        0 * consoleDataSource._
        result == connection
    }

    void "getConnection, inside the console, uses the console data source"() {
        given:
        Connection connection = Mock(Connection)
        DataSource defaultDataSource = Mock(DataSource)
        DataSource consoleDataSource = Mock(DataSource)
        ConsoleRoutingDataSource routingDataSource = new ConsoleRoutingDataSource(defaultDataSource, consoleDataSource)
        ConsoleDataSourceContext.markConsole()

        when:
        Connection result = routingDataSource.connection

        then:
        1 * consoleDataSource.connection >> connection
        0 * defaultDataSource._
        result == connection
    }

    void "close closes both data sources"() {
        given:
        CloseableDataSource defaultDataSource = Mock(CloseableDataSource)
        CloseableDataSource consoleDataSource = Mock(CloseableDataSource)
        ConsoleRoutingDataSource routingDataSource = new ConsoleRoutingDataSource(defaultDataSource, consoleDataSource)

        when:
        routingDataSource.close()

        then:
        1 * defaultDataSource.close()
        1 * consoleDataSource.close()
    }

    void "close, when closing the first data source fails, still closes the second one"() {
        given:
        CloseableDataSource defaultDataSource = Mock(CloseableDataSource)
        CloseableDataSource consoleDataSource = Mock(CloseableDataSource)
        ConsoleRoutingDataSource routingDataSource = new ConsoleRoutingDataSource(defaultDataSource, consoleDataSource)

        when:
        routingDataSource.close()

        then:
        1 * defaultDataSource.close() >> { throw new IllegalStateException("broken pool") }
        1 * consoleDataSource.close()
        noExceptionThrown()
    }

    interface CloseableDataSource extends DataSource, Closeable {
    }
}
